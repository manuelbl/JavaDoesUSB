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
 * Multiple asynchronous transfer are submitted to achieve a good
 * degree of concurrency between the USB communication handled by the operating
 * system and the consuming application code.
 * </p>
 * <p>
 * For thread synchronization (between the background thread handling IO completion
 * and the consuming application thread) a blocking queue is used. When an transfer
 * completes, the background thread adds it to the queue. The consuming code
 * waits for the next item in the queue.
 * </p>
 */
public abstract class EndpointInputStream extends InputStream {

    protected USBDeviceImpl device;
    protected final int endpointNumber;
    // Arena to allocate buffers and completion handlers
    protected final Arena arena;
    // Transfer size (multiple of packet size)
    protected final int transferSize;
    // Queue of completed transfers
    private final ArrayBlockingQueue<Transfer> completedTransferQueue;
    // Number of outstanding transfers (includes transfers pending with the
    // operating system and transfers in the completed queue)
    private int numOutstandingTransfers;
    // Transfer and associated buffer being currently read from
    private Transfer currentTransfer;
    // Read offset within current transfer buffer
    private int readOffset;

    /**
     * Creates a new instance
     *
     * @param device         USB device
     * @param endpointNumber endpoint number
     * @param bufferSize     approximate buffer size (in bytes)
     */
    protected EndpointInputStream(USBDeviceImpl device, int endpointNumber, int bufferSize) {
        this.device = device;
        this.endpointNumber = endpointNumber;
        arena = Arena.openShared();

        int packetSize = device.getEndpoint(USBDirection.IN, endpointNumber).packetSize();
        int n = (int) Math.round(Math.sqrt((double) bufferSize / packetSize));
        n = Math.min(Math.max(n, 4), 32); // 32 limits packet size to 16KB (for USB HS)
        transferSize = n * packetSize;
        int maxOutstandingTransfers = Math.max((bufferSize + transferSize / 2) / transferSize, 2);

        configureEndpoint();

        completedTransferQueue = new ArrayBlockingQueue<>(maxOutstandingTransfers);

        // create all transfers, and submit them except one
        try {
            for (int i = 0; i < maxOutstandingTransfers; i++) {
                final var transfer = device.createTransfer();
                transfer.data = arena.allocate(transferSize, 8);
                transfer.dataSize = transferSize;
                transfer.completion = this::onCompletion;

                if (i == 0) {
                    currentTransfer = transfer;
                } else {
                    submitTransfer(transfer);
                }
            }
        } catch (Throwable t) {
            collectOutstandingTransfers();
            throw t;
        }
    }

    private boolean isClosed() {
        return device == null;
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
            // transfers will terminate anyway.
        }
        device = null;

        collectOutstandingTransfers();
    }

    @Override
    public int read() throws IOException {
        if (isClosed())
            return -1;

        if (available() == 0)
            receiveMoreData();

        int b = currentTransfer.data.get(JAVA_BYTE, readOffset) & 0xff;
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
        int n = Math.min(len, currentTransfer.resultSize - readOffset);
        MemorySegment.copy(currentTransfer.data, readOffset, MemorySegment.ofArray(b), off, n);
        readOffset += n;

        // TODO: poll for further completed transfers if 'n' is less than 'len'

        return n;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public int available() throws IOException {
        return currentTransfer.resultSize - readOffset;
    }

    private void receiveMoreData() throws IOException {
        try {
            // loop until non-ZLP has been received
            do {
                // the current transfer has no more data to process and
                // can be submitted to read more data
                submitTransfer(currentTransfer);

                currentTransfer = waitForCompletedTransfer();
                readOffset = 0;

                // check for error
                if (currentTransfer.resultCode != 0)
                    device.throwOSException(currentTransfer.resultCode, "error reading from endpoint %d",
                            endpointNumber);

            } while (currentTransfer.resultSize <= 0);

        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    private Transfer waitForCompletedTransfer() {
        while (true) {
            try {
                Transfer transfer = completedTransferQueue.take();
                numOutstandingTransfers -= 1;
                return transfer;
            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
    }

    private void submitTransfer(Transfer transfer) {
        submitTransferIn(transfer);
        numOutstandingTransfers += 1;
    }

    private void onCompletion(Transfer transfer) {
        completedTransferQueue.add(transfer);
    }

    private void collectOutstandingTransfers() {
        // wait until completion handlers have been called
        while (numOutstandingTransfers > 0)
            waitForCompletedTransfer();

        completedTransferQueue.clear();
        currentTransfer = null;
        arena.close();
    }

    protected abstract void submitTransferIn(Transfer transfer);

    protected void configureEndpoint() {
    }
}
