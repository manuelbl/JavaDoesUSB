//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDirection;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

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
public abstract class AsyncEndpointOutputStream extends OutputStream {

    private static final int MAX_OUTSTANDING_REQUESTS = 4;

    protected USBDeviceImpl device;
    protected final int endpointNumber;
    protected final Arena arena;
    // Endpoint's packet size
    private final int packetSize;
    // Size of buffers (multiple of packet size)
    private final int bufferSize;
    // Blocking queue of available requests (to limit the number of submitted requests)
    private final ArrayBlockingQueue<TransferRequest> availableRequestQueue;
    private boolean needsZlp;
    private TransferRequest currentRequest;
    private int writeOffset;
    private int numOutstandingRequests;
    private boolean hasError;


    /**
     * Creates a new instance
     * @param device USB device
     * @param endpointNumber endpoint number
     */
    protected AsyncEndpointOutputStream(USBDeviceImpl device, int endpointNumber) {
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
            request.completionHandler = (result, size) -> onCompletion(request, result);

            if (i == 0) {
                currentRequest = request;
            } else {
                availableRequestQueue.add(request);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed())
            return;

        if (!hasError)
            flush();
        else
            waitForOutstandingRequests();

        device = null;
        availableRequestQueue.clear();
        currentRequest = null;
        arena.close();
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

        waitForOutstandingRequests();
    }

    private boolean isClosed() {
        return device == null;
    }

    /**
     * Submits a request for transfer and set a new request
     * as the current one, possibly waiting until it is ready.
     * <p>
     *     Throws an exception if the request to be reused has completed with an error on the
     *     previous operation. The exception is suppressed if {@code hasError} flag is set.
     * </p>
     *
     * @param size size of data to be transmitted
     */
    private void submitTransfer(int size) throws IOException {
        try {
            submitTransferOut(currentRequest.buffer, size, currentRequest.completionHandler);

            synchronized (this) {
                numOutstandingRequests += 1;
            }

            needsZlp = size == packetSize;
            writeOffset = 0;
            currentRequest = waitForAvailableRequest();

        } catch (Throwable t) {
            hasError = true;
            close();
            throw t;
        }
    }

    /**
     * Wait until all outstanding requests have been completed.
     * <p>
     *     Throws an exception if any of the requests has completed with an error.
     *     The exception is suppressed if {@code hasError} flag is set.
     * </p>
     */
    private void waitForOutstandingRequests() {
        // Wait until all buffers have been transmitted by removing them from the
        // queue and reinserting them.

        int numRequests;
        synchronized (this) {
            numRequests = numOutstandingRequests + availableRequestQueue.size();
        }

        if (numRequests == 0)
            return;

        var requests = new TransferRequest[numRequests];
        for (int i = 0; i < numRequests; i++)
            requests[i] = waitForAvailableRequest();

        // reinsert the requests
        if (!hasError)
            availableRequestQueue.addAll(Arrays.asList(requests));
    }

    /**
     * Wait until one of the allocated requests is available for use.
     * <p>
     *     Throws an exception if the request to be reused has completed with an error on the
     *     previous operation. The exception is suppressed if {@code hasError} flag is set.
     * </p>
     * @return request ready for use
     */
    private TransferRequest waitForAvailableRequest() {
        while (true) {
            try {
                TransferRequest request = availableRequestQueue.take();

                // check for error
                int result = request.result;
                if (result != 0 && !hasError) {
                    request.result = 0;
                    throwException(result, "error reading from USB endpoint");
                }

                return request;

            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
    }

    /**
     * Called by the asynchronous IO completion handler.
     *
     * @param request the completed request
     * @param result the request result code
     */
    private synchronized void onCompletion(TransferRequest request, int result) {
        request.result = result;
        availableRequestQueue.add(request);
        numOutstandingRequests -= 1;
    }

    protected abstract void submitTransferOut(MemorySegment data, int dataSize, AsyncIOCompletion completion);

    protected abstract void throwException(int errorCode, String message);

    private static class TransferRequest {
        MemorySegment buffer;
        int result;
        AsyncIOCompletion completionHandler;
    }
}
