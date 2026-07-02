//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbDirection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.WARNING;
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

    private static final System.Logger LOG = System.getLogger(EndpointOutputStream.class.getName());

    // Maximum time (ms) to wait for outstanding transfers to complete during teardown.
    // A completion that is never delivered (e.g. after an unplug) then degrades to a
    // logged warning instead of a permanent hang.
    private static final long TEARDOWN_TIMEOUT_MS = 1000;

    protected UsbDeviceImpl device;
    protected final int endpointNumber;
    protected final Arena arena;
    // Endpoint packet size
    private final int packetSize;
    // Transfer size (multiple of packet size)
    private final int transferSize;
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
     * @param bufferSize     approximate buffer size (in bytes)
     */
    protected EndpointOutputStream(UsbDeviceImpl device, int endpointNumber, int bufferSize) {
        this.device = device;
        this.endpointNumber = endpointNumber;
        //arena = Arena.ofShared();  // not supported by GraalVM
        arena = Arena.ofAuto();

        packetSize = device.getEndpoint(UsbDirection.OUT, endpointNumber).getPacketSize();

        // use between 4 and 32 packets per transfer (256B to 2KB for FS, 2KB to 16KB for HS)
        var numPacketsPerTransfer = (int) Math.round(Math.sqrt((double) bufferSize / packetSize));
        numPacketsPerTransfer = Math.clamp(numPacketsPerTransfer, 4, 32);
        transferSize = numPacketsPerTransfer * packetSize;

        // use at least 2 outstanding transfers (3 in total)
        var maxOutstandingTransfers = Math.max((bufferSize + transferSize / 2) / transferSize, 3);

        configureEndpoint();

        availableTransferQueue = new ArrayBlockingQueue<>(maxOutstandingTransfers);

        // prefill transfer queue
        for (var i = 0; i < maxOutstandingTransfers; i++) {
            final var transfer = device.createTransfer();
            transfer.setData(arena.allocate(transferSize, 8));
            transfer.setCompletion(this::onCompletion);

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

        // Teardown path: every wait is bounded by a single deadline so a lost completion
        // (device unplugged, or completion dropped in a source-removal race) degrades to a
        // logged warning instead of hanging the application thread. Unlike the public
        // flush(), this must not route through the unbounded waits.
        var deadline = System.currentTimeMillis() + TEARDOWN_TIMEOUT_MS;

        try {
            if (!hasError) {
                // best-effort: transmit any remaining buffered data (and a ZLP if needed)
                if (writeOffset > 0)
                    submitForClose(writeOffset, deadline);
                if (needsZlp && currentTransfer != null)
                    submitForClose(0, deadline);
            }

            drainOutstandingTransfers(deadline);

        } catch (Exception e) {
            // teardown must not fail; data-path errors are already surfaced by write()/flush()
            LOG.log(WARNING, "error while closing output stream - ignoring", e);

        } finally {
            device = null;
            availableTransferQueue.clear();
            currentTransfer = null;
            //arena.close();
        }
    }

    /**
     * Submits the current transfer during teardown and acquires a replacement,
     * both bounded by the given deadline.
     * <p>
     * Unlike {@link #submitTransfer(int)} this does not recurse into {@link #close()} on error,
     * and it does not block indefinitely when acquiring the next transfer instance.
     * </p>
     *
     * @param size     size of data to be transmitted
     * @param deadline absolute deadline (ms since epoch) for acquiring the next transfer
     */
    private void submitForClose(int size, long deadline) {
        currentTransfer.setDataSize(size);
        submitTransferOut(currentTransfer);

        synchronized (this) {
            numOutstandingTransfers += 1;
        }

        needsZlp = size == packetSize;
        writeOffset = 0;
        // if no transfer becomes available within the deadline, currentTransfer stays null,
        // the drain below still bounded-waits for the in-flight transfer to complete
        currentTransfer = pollAvailableTransfer(deadline);
    }

    /**
     * Waits for all outstanding transfers to complete, bounded by the given deadline.
     * <p>
     * If a completion is not delivered in time, the remaining transfers are abandoned and a
     * warning is logged.
     * </p>
     *
     * @param deadline absolute deadline (ms since epoch)
     */
    private void drainOutstandingTransfers(long deadline) {
        int numTransfers;
        synchronized (this) {
            numTransfers = numOutstandingTransfers + availableTransferQueue.size();
        }

        for (var i = 0; i < numTransfers; i++) {
            if (pollAvailableTransfer(deadline) == null) {
                int abandoned;
                synchronized (this) {
                    abandoned = numOutstandingTransfers;
                }
                LOG.log(WARNING,
                        "abandoning {0} outstanding transfer(s) during output stream teardown - no completion within {1} ms",
                        abandoned, TEARDOWN_TIMEOUT_MS);
                break;
            }
        }
    }

    /**
     * Waits until a transfer instance is available for use, bounded by the given deadline.
     *
     * @param deadline absolute deadline (ms since epoch)
     * @return transfer instance ready for use, or {@code null} if the deadline expired
     */
    private Transfer pollAvailableTransfer(long deadline) {
        var wasInterrupted = false;
        try {
            while (true) {
                var remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                    return null;

                try {
                    var transfer = availableTransferQueue.poll(remaining, TimeUnit.MILLISECONDS);
                    if (transfer == null)
                        return null;

                    // surface a transfer error unless we are already in the error path
                    var result = transfer.resultCode();
                    if (result != 0 && !hasError) {
                        transfer.setResultCode(0);
                        device.throwOSException(result, "error occurred while transmitting to endpoint %d", endpointNumber);
                    }

                    return transfer;

                } catch (InterruptedException _) {
                    // defer the interrupt: keep polling the remaining time without re-setting
                    // the flag (avoids a busy-spin), then re-assert it once we are done
                    wasInterrupted = true;
                }
            }
        } finally {
            if (wasInterrupted)
                Thread.currentThread().interrupt();
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkIsOpen();

        currentTransfer.data().set(JAVA_BYTE, writeOffset, (byte) b);
        writeOffset += 1;
        if (writeOffset == transferSize)
            submitTransfer(writeOffset);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        checkIsOpen();

        while (len > 0) {
            var chunkSize = Math.min(len, transferSize - writeOffset);
            MemorySegment.copy(b, off, currentTransfer.data(), JAVA_BYTE, writeOffset, chunkSize);
            writeOffset += chunkSize;
            off += chunkSize;
            len -= chunkSize;

            if (writeOffset == transferSize)
                submitTransfer(writeOffset);
        }
    }

    @Override
    public void flush() throws IOException {
        checkIsOpen();

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
            currentTransfer.setDataSize(size);
            submitTransferOut(currentTransfer);

            synchronized (this) {
                numOutstandingTransfers += 1;
            }

            needsZlp = size == packetSize;
            writeOffset = 0;
            currentTransfer = waitForAvailableTransfer();

        } catch (Exception t) {
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
        for (var i = 0; i < numTransfers; i++)
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
        // Defer interruption: keep a local flag instead of re-asserting the interrupt
        // inside the loop (which would make the next take() throw immediately and
        // busy-spin). Re-assert once a transfer has actually become available.
        var wasInterrupted = false;
        try {
            while (true) {
                try {
                    var transfer = availableTransferQueue.take();

                    // check for error
                    var result = transfer.resultCode();
                    if (result != 0 && !hasError) {
                        transfer.setResultCode(0);
                        device.throwOSException(result, "error occurred while transmitting to endpoint %d", endpointNumber);
                    }

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

    protected void configureEndpoint() {
    }

    private void checkIsOpen() throws IOException {
        if (isClosed())
            throw new IOException("endpoint output stream has been closed");
    }
}
