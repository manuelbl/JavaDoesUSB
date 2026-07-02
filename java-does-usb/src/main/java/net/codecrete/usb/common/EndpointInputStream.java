//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.WARNING;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Input stream for bulk endpoints – optimized for high throughput.
 *
 * <p>
 * Multiple asynchronous transfers are submitted to achieve a good
 * degree of concurrency between the USB communication handled by the operating
 * system and the consuming application code.
 * </p>
 * <p>
 * For thread synchronization (between the background thread handling IO completions
 * and the consuming application thread) a blocking queue is used. When an transfer
 * completes, the background thread adds it to the queue. The consuming code
 * waits for the next item in the queue.
 * </p>
 */
public abstract class EndpointInputStream extends InputStream {

    private static final System.Logger LOG = System.getLogger(EndpointInputStream.class.getName());

    // Maximum time (ms) to wait for outstanding transfers to complete during teardown.
    // A completion that is never delivered (e.g. after an unplug) then degrades to a
    // logged warning instead of a permanent hang.
    private static final long TEARDOWN_TIMEOUT_MS = 1000;

    protected UsbDeviceImpl device;
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
    protected EndpointInputStream(UsbDeviceImpl device, int endpointNumber, int bufferSize) {
        this.device = device;
        this.endpointNumber = endpointNumber;
        //arena = Arena.ofShared();  // not supported by GraalVM
        arena = Arena.ofAuto();

        var packetSize = device.getEndpoint(UsbDirection.IN, endpointNumber).getPacketSize();

        // use between 4 and 32 packets per transfer (256B to 2KB for FS, 2KB to 16KB for HS)
        var numPacketsPerTransfer = (int) Math.round(Math.sqrt((double) bufferSize / packetSize));
        numPacketsPerTransfer = Math.clamp(numPacketsPerTransfer, 4, 32);
        transferSize = numPacketsPerTransfer * packetSize;

        // use at least 2 outstanding transfers (3 in total)
        var maxOutstandingTransfers = Math.max((bufferSize + transferSize / 2) / transferSize, 3);

        configureEndpoint();

        completedTransferQueue = new ArrayBlockingQueue<>(maxOutstandingTransfers);

        // create all transfers, and submit them except one
        try {
            for (var i = 0; i < maxOutstandingTransfers; i++) {
                final var transfer = device.createTransfer();
                transfer.setData(arena.allocate(transferSize, 8));
                transfer.setDataSize(transferSize);
                transfer.setCompletion(this::onCompletion);

                if (i == 0) {
                    currentTransfer = transfer;
                } else {
                    submitTransfer(transfer);
                }
            }
        } catch (Exception t) {
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
            device.abortTransfers(UsbDirection.IN, endpointNumber);

        } catch (UsbException _) {
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

        var b = currentTransfer.data().get(JAVA_BYTE, readOffset) & 0xff;
        readOffset += 1;
        return b;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        if (isClosed())
            return -1;

        var numRead = 0;
        do {
            if (available() == 0)
                receiveMoreData();

            // copy data to receiving buffer
            var n = Math.min(len - numRead, currentTransfer.resultSize() - readOffset);
            MemorySegment.copy(currentTransfer.data(), readOffset, MemorySegment.ofArray(b), (long) off + numRead, n);
            readOffset += n;
            numRead += n;

        } while (numRead < len && hasMoreTransfers());

        return numRead;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public int available() throws IOException {
        return currentTransfer.resultSize() - readOffset;
    }

    private boolean hasMoreTransfers() {
        return !completedTransferQueue.isEmpty();
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
                if (currentTransfer.resultCode() != 0)
                    device.throwOSException(currentTransfer.resultCode(), "error occurred while reading from endpoint %d",
                            endpointNumber);

            } while (currentTransfer.resultSize() <= 0);

        } catch (Exception t) {
            close();
            throw t;
        }
    }

    private Transfer waitForCompletedTransfer() {
        // Defer interruption: keep a local flag instead of re-asserting the interrupt
        // inside the loop (which would make the next take() throw immediately and
        // busy-spin). Re-assert once the completion has actually arrived.
        var wasInterrupted = false;
        try {
            while (true) {
                try {
                    var transfer = completedTransferQueue.take();
                    numOutstandingTransfers -= 1;
                    return transfer;
                } catch (InterruptedException _) {
                    wasInterrupted = true;
                }
            }
        } finally {
            if (wasInterrupted)
                Thread.currentThread().interrupt();
        }
    }

    private void submitTransfer(Transfer transfer) {
        submitTransferIn(transfer);
        numOutstandingTransfers += 1;
    }

    private void onCompletion(Transfer transfer) {
        completedTransferQueue.add(transfer);
    }

    @SuppressWarnings("java:S2142")
    private void collectOutstandingTransfers() {
        // Wait until the completion handlers have been called. This is a teardown path,
        // so the wait is bounded: if a completion is never delivered (device unplugged,
        // or the completion is lost in a source-removal race), abandon the transfer and
        // log a warning instead of blocking the application thread forever.
        var deadline = System.currentTimeMillis() + TEARDOWN_TIMEOUT_MS;
        var wasInterrupted = false;

        while (numOutstandingTransfers > 0) {
            var remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                LOG.log(WARNING,
                        "abandoning {0} outstanding transfer(s) during input stream teardown - no completion within {1} ms",
                        numOutstandingTransfers, TEARDOWN_TIMEOUT_MS);
                break;
            }

            try {
                if (completedTransferQueue.poll(remaining, TimeUnit.MILLISECONDS) != null)
                    numOutstandingTransfers -= 1;
            } catch (InterruptedException _) {
                // defer the interrupt: keep polling the remaining time without re-setting
                // the flag (avoids a busy-spin), then re-assert it once we are done
                wasInterrupted = true;
            }
        }

        if (wasInterrupted)
            Thread.currentThread().interrupt();

        completedTransferQueue.clear();
        currentTransfer = null;
        //arena.close();
    }

    protected abstract void submitTransferIn(Transfer transfer);

    protected void configureEndpoint() {
    }
}
