//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBDirection;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static net.codecrete.usb.macos.MacosUSBException.throwException;

/**
 * Output stream for bulk endpoints – optimized for high throughput.
 *
 * <p>
 * Multiple asynchronous transfer OUT requests are submitted to achieve a good
 * degree of concurrency between the USB communication handled by the operating
 * system and the producing application code. The number of requests is limited
 * in order to retain the flow control and keep memory usage at a reasonable size.
 * </p>
 * <p>
 * For thread synchronization (between the background thread handling IO completion
 * and the producing application thread) a blocking queue is used. It is prefilled with
 * requests ready to be used for a transfer. The background thread adds the completed
 * requests back to this queue. The producing application thread waits until a request
 * is available for use.
 * </p>
 */
public class MacosEndpointOutputStream extends OutputStream {

    private static final int MAX_OUTSTANDING_REQUESTS = 4;

    private MacosUSBDevice device;
    private final int endpointNumber;
    private final Arena arena;
    // Endpoint's packet size
    private final int packetSize;
    // Size of buffers (multiple of packet size)
    private final int bufferSize;
    // Blocking queue of available requests (to limit the number of submitted requests)
    private final ArrayBlockingQueue<TransferRequest> availableRequestQueue;
    private boolean needsZlp;
    private TransferRequest currentRequest;
    private int writeOffset;


    /**
     * Creates a new instance
     * @param device USB device
     * @param endpointNumber endpoint number
     */
    MacosEndpointOutputStream(MacosUSBDevice device, int endpointNumber) {
        this.device = device;
        this.endpointNumber = endpointNumber;

        packetSize = device.getEndpoint(USBDirection.OUT, endpointNumber).packetSize();
        bufferSize = packetSize;
        availableRequestQueue = new ArrayBlockingQueue<>(MAX_OUTSTANDING_REQUESTS);
        arena = Arena.openShared();

        // prefill request queue
        for (int i = 0; i < MAX_OUTSTANDING_REQUESTS; i++) {
            final var request = new TransferRequest();
            request.buffer = arena.allocate(bufferSize, 8);
            request.completionHandler = device.createCompletionHandler(USBDirection.OUT, endpointNumber, arena,
                    (result, size) -> onCompletion(request, result));

            if (i == 0) {
                currentRequest = request;
            } else {
                availableRequestQueue.add(request);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (isClosed())
            throw new IOException("Bulk endpoint output stream has been closed");

        currentRequest.buffer.set(JAVA_BYTE, writeOffset, (byte)b);
        writeOffset += 1;
        if (writeOffset == bufferSize)
            submitTransfer(writeOffset);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (isClosed())
            throw new IOException("Bulk endpoint output stream has been closed");

        while (len > 0) {
            int chunkSize = Math.min(len, bufferSize - writeOffset);
            MemorySegment.copy(b, off, currentRequest.buffer, JAVA_BYTE, writeOffset, chunkSize);
            writeOffset += chunkSize;
            off += chunkSize;
            len -= chunkSize;

            if (writeOffset == bufferSize)
                submitTransfer(writeOffset);
        }
    }

    @Override
    public void flush() throws IOException {
        if (isClosed())
            throw new IOException("Bulk endpoint output stream has been closed");

        if (writeOffset > 0)
            submitTransfer(writeOffset);

        if (needsZlp)
            submitTransfer(0);

        // Wait until all buffers have been transmitted by removing them from the
        // queue and reinserting them. One request is the current request.
        // So the queue only contains MAX_OUTSTANDING_REQUESTS - 1 requests.
        var requests = new TransferRequest[MAX_OUTSTANDING_REQUESTS - 1];
        for (int i = 0; i < MAX_OUTSTANDING_REQUESTS - 1; i++)
            requests[i] = waitForAvailableRequest();
        availableRequestQueue.addAll(Arrays.asList(requests).subList(1, MAX_OUTSTANDING_REQUESTS - 1));
    }

    @Override
    public void close() throws IOException {
        flush();

        device = null;
        availableRequestQueue.clear();
        currentRequest = null;
        arena.close();
    }

    private boolean isClosed() {
        return device == null;
    }

    private void submitTransfer(int size) {
        device.submitTransferOut(endpointNumber, currentRequest.buffer, size, currentRequest.completionHandler);
        needsZlp = size == packetSize;

        writeOffset = 0;
        currentRequest = waitForAvailableRequest();
    }

    private void onCompletion(TransferRequest request, int result) {
        request.result = result;
        availableRequestQueue.add(request);
    }

    TransferRequest waitForAvailableRequest() {
        while (true) {
            try {
                TransferRequest request = availableRequestQueue.take();

                // check for error
                int result = request.result;
                if (result != 0) {
                    request.result = 0;
                    throwException(result, "error writing to USB endpoint");
                }

                return request;

            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
    }

    private static class TransferRequest {
        MemorySegment buffer;
        int result;
        MemorySegment completionHandler;
    }
}
