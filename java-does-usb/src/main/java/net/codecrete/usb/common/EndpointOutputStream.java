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
 * Multiple asynchronous transfers are submitted to achieve a good
 * degree of concurrency between the USB communication handled by the operating
 * system and the producing application code. The number of concurrent transfers is limited
 * in order to retain flow control and keep memory usage at a reasonable size.
 * </p>
 * <p>
 * For thread synchronization (between the background thread handling IO completion
 * and the producing application thread) a blocking queue is used. It is prefilled with
 * transfer instances ready to be used. The background thread adds the completed
 * instances back to this queue. The producing application thread waits until a transfer instance
 * is available for use.
 * </p>
 */
public abstract class EndpointOutputStream extends OutputStream {

    private static final int MAX_OUTSTANDING_TRANSFERS = 4;

    protected USBDeviceImpl device;
    protected final int endpointNumber;
    protected final Arena arena;
    // Endpoint's packet size
    private final int packetSize;
    // Size of buffers (multiple of packet size)
    private final int bufferSize;
    // Blocking queue of available transfers (to limit the number of submitted transfers)
    private final ArrayBlockingQueue<Transfer> availableTransferQueue;
    private boolean needsZlp;
    private Transfer currentTransfer;
    private int writeOffset;
    private int numOutstandingTransfers;
    private boolean hasError;


    /**
     * Creates a new instance
     *
     * @param device         USB device
     * @param endpointNumber endpoint number
     */
    protected EndpointOutputStream(USBDeviceImpl device, int endpointNumber) {
        this.device = device;
        this.endpointNumber = endpointNumber;

        packetSize = device.getEndpoint(USBDirection.OUT, endpointNumber).packetSize();
        bufferSize = packetSize;

        availableTransferQueue = new ArrayBlockingQueue<>(MAX_OUTSTANDING_TRANSFERS);
        arena = Arena.openShared();

        // prefill transfer queue
        for (int i = 0; i < MAX_OUTSTANDING_TRANSFERS; i++) {
            final var transfer = device.createTransfer();
            transfer.data = arena.allocate(bufferSize, 8);
            transfer.completion = this::onCompletion;

            if (i == 0) {
                currentTransfer = transfer;
            } else {
                availableTransferQueue.add(transfer);
            }
        }
    }

    private boolean isClosed() {
        return device == null;
    }

    @Override
    public void close() throws IOException {
        if (isClosed())
            return;

        if (!hasError)
            flush();
        else
            waitForOutstandingTransfers();

        device = null;
        availableTransferQueue.clear();
        currentTransfer = null;
        arena.close();
    }

    @Override
    public void write(int b) throws IOException {
        if (isClosed())
            throw new IOException("Bulk endpoint output stream has been closed");

        currentTransfer.data.set(JAVA_BYTE, writeOffset, (byte) b);
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
            MemorySegment.copy(b, off, currentTransfer.data, JAVA_BYTE, writeOffset, chunkSize);
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

        waitForOutstandingTransfers();
    }

    /**
     * Submits a transfer and set a new transfer instance
     * as the current one, possibly waiting until one is ready.
     * <p>
     * Throws an exception if the transfer to be reused has completed with an error on the
     * previous operation. The exception is suppressed if {@code hasError} flag is set.
     * </p>
     *
     * @param size size of data to be transmitted
     */
    private void submitTransfer(int size) throws IOException {
        try {
            currentTransfer.dataSize = size;
            submitTransferOut(currentTransfer);

            synchronized (this) {
                numOutstandingTransfers += 1;
            }

            needsZlp = size == packetSize;
            writeOffset = 0;
            currentTransfer = waitForAvailableTransfer();

        } catch (Throwable t) {
            hasError = true;
            close();
            throw t;
        }
    }

    /**
     * Wait until all outstanding transfers have been completed.
     * <p>
     * Throws an exception if any of the transfers has completed with an error.
     * The exception is suppressed if {@code hasError} flag is set.
     * </p>
     */
    private void waitForOutstandingTransfers() {
        // Wait until all buffers have been transmitted by removing them from the
        // queue and reinserting them.

        int numTransfers;
        synchronized (this) {
            numTransfers = numOutstandingTransfers + availableTransferQueue.size();
        }

        if (numTransfers == 0)
            return;

        var transfers = new Transfer[numTransfers];
        for (int i = 0; i < numTransfers; i++)
            transfers[i] = waitForAvailableTransfer();

        // reinsert the transfer instances
        if (!hasError)
            availableTransferQueue.addAll(Arrays.asList(transfers));
    }

    /**
     * Wait until one of the allocated transfer instances is available for use.
     * <p>
     * Throws an exception if the transfer to be reused has completed with an error on the
     * previous operation. The exception is suppressed if {@code hasError} flag is set.
     * </p>
     *
     * @return transfer instance ready for use
     */
    private Transfer waitForAvailableTransfer() {
        while (true) {
            try {
                Transfer transfer = availableTransferQueue.take();

                // check for error
                int result = transfer.resultCode;
                if (result != 0 && !hasError) {
                    transfer.resultCode = 0;
                    device.throwOSException(result, "error writing to endpoint %d", endpointNumber);
                }

                return transfer;

            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
    }

    /**
     * Called by the asynchronous IO completion handler.
     *
     * @param transfer the completed request
     */
    private synchronized void onCompletion(Transfer transfer) {
        availableTransferQueue.add(transfer);
        numOutstandingTransfers -= 1;
    }

    protected abstract void submitTransferOut(Transfer request);
}
