//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ArrayBlockingQueue;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Input stream for bulk endpoints – optimized for high throughput.
 *
 * <p>
 * Multiple asynchronous transfer IN requests are submitted to achieve a good
 * degree of concurrency between the USB communication handled by the operating
 * system and the consuming application code.
 * </p>
 * <p>
 * For thread synchronization (between the background thread handling IO completion
 * and the consuming application thread) a blocking queue is used. When an transfer IN
 * requests completes, the background thread adds it to the queue. The consuming code
 * waits for the next item in the queue.
 * </p>
 */
public abstract class EndpointInputStream extends InputStream {

    private static final int MAX_OUTSTANDING_REQUESTS = 8;

    protected USBDeviceImpl device;
    protected final int endpointNumber;
    // Arena to allocate buffers and completion handlers
    protected final Arena arena;
    // Size of buffers (multiple of packet size)
    protected final int bufferSize;
    // Queue of completed requests
    private final ArrayBlockingQueue<TransferRequest> completedRequestQueue;
    // Number of outstanding requests (includes requests pending with the
    // operating system and requests in the completed queue)
    private int numOutstandingRequests;
    // Request and buffer being currently read from
    private TransferRequest currentRequest;
    // Read offset within current request
    private int readOffset;

    /**
     * Creates a new instance
     *
     * @param device         USB device
     * @param endpointNumber endpoint number
     */
    protected EndpointInputStream(USBDeviceImpl device, int endpointNumber) {
        this.device = device;
        this.endpointNumber = endpointNumber;

        bufferSize = 4 * device.getEndpoint(USBDirection.IN, endpointNumber).packetSize();
        completedRequestQueue = new ArrayBlockingQueue<>(MAX_OUTSTANDING_REQUESTS);

        // create all requests and submit all except one
        arena = Arena.openShared();
        try {
            for (int i = 0; i < MAX_OUTSTANDING_REQUESTS; i++) {
                final var request = new TransferRequest();
                request.buffer = arena.allocate(bufferSize, 8);
                request.completionHandler = (result, size) -> onCompletion(request, result, size);

                if (i == 0) {
                    currentRequest = request;
                } else {
                    submitRequest(request);
                }
            }
        } catch (Throwable t) {
            collectOutstandingRequests();
            throw t;
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        if (isClosed())
            return;

        // abort all transfers on endpoint
        try {
            device.abortTransfers(USBDirection.IN, endpointNumber);
        } catch (USBException e) {
            // If aborting the transfer is not possible, the device has
            // likely been closed or unplugged. So all outstanding
            // requests will terminate anyway.
        }
        device = null;

        collectOutstandingRequests();
    }

    private void collectOutstandingRequests() {
        // wait until completion handlers have been called
        while (numOutstandingRequests > 0)
            waitForRequestCompletion();

        completedRequestQueue.clear();
        currentRequest = null;
        arena.close();
    }

    @Override
    public int read() throws IOException {
        if (isClosed())
            return -1;

        if (available() == 0)
            receiveMoreData();

        int b = currentRequest.buffer.get(JAVA_BYTE, readOffset) & 0xff;
        readOffset += 1;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (isClosed())
            return -1;

        if (available() == 0)
            receiveMoreData();

        // copy data to receiving buffer
        int n = Math.min(len, currentRequest.resultSize - readOffset);
        MemorySegment.copy(currentRequest.buffer, readOffset, MemorySegment.ofArray(b), off, n);
        readOffset += n;

        // TODO: poll for further completed requests if 'n' is less than 'len'

        return n;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public int available() throws IOException {
        return currentRequest.resultSize - readOffset;
    }

    private boolean isClosed() {
        return device == null;
    }

    private void receiveMoreData() throws IOException {
        try {
            // loop until non-ZLP has been received
            do {
                submitRequest(currentRequest);

                currentRequest = waitForRequestCompletion();
                readOffset = 0;

                // check for error
                if (currentRequest.result != 0)
                    throwException(currentRequest.result, "error reading from USB endpoint");

            } while (currentRequest.resultSize == 0);

        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    void submitRequest(TransferRequest request) {
        submitTransferIn(request.buffer, bufferSize, request.completionHandler);
        numOutstandingRequests += 1;
    }

    protected abstract void submitTransferIn(MemorySegment buffer, int bufferSize, AsyncIOCompletion completion);

    protected abstract void throwException(int errorCode, String message);

    void onCompletion(TransferRequest request, int result, int size) {
        request.result = result;
        request.resultSize = size;
        completedRequestQueue.add(request);
    }

    TransferRequest waitForRequestCompletion() {
        while (true) {
            try {
                TransferRequest request = completedRequestQueue.take();
                numOutstandingRequests -= 1;
                return request;

            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
    }

    private static class TransferRequest {
        MemorySegment buffer;
        int result;
        int resultSize;
        AsyncIOCompletion completionHandler;
    }
}
