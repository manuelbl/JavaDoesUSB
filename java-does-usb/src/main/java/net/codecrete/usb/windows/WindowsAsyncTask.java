//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.UsbException;
import windows.win32.system.io.OVERLAPPED;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUsbException.throwLastError;
import static windows.win32.foundation.WIN32_ERROR.ERROR_OPERATION_ABORTED;
import static windows.win32.system.io.Apis.CreateIoCompletionPort;
import static windows.win32.system.io.Apis.GetQueuedCompletionStatus;
import static windows.win32.system.threading.Constants.INFINITE;

/**
 * Background task for handling asynchronous transfers.
 * <p>
 * Each USB device must register its handle with this task.
 * </p>
 * <p>
 * The task keeps track of the submitted transfers by indexing them
 * by OVERLAPPED struct address.
 * </p>
 * <p>
 * OVERLAPPED structs are allocated but never freed. To limit the memory usage,
 * OVERLAPPED structs are reused. So the maximum number of outstanding transfers
 * determines the number of allocated OVERLAPPED structs.
 * </p>
 */
@SuppressWarnings("java:S6548")
class WindowsAsyncTask {

    private static final System.Logger LOG = System.getLogger(WindowsAsyncTask.class.getName());

    /**
     * Singleton instance of background task.
     */
    static final WindowsAsyncTask INSTANCE = new WindowsAsyncTask();

    // Currently outstanding transfer requests,
    // indexed by OVERLAPPED address.
    private Map<Long, WindowsTransfer> requestsByOverlapped;
    // available OVERLAPPED data structures
    private List<MemorySegment> availableOverlappedStructs;
    // Arena used to allocate OVERLAPPED data structures
    private Arena overlappedArena;

    /**
     * Windows completion port for asynchronous/overlapped IO
     */
    private MemorySegment asyncIoCompletionPort = NULL;

    /**
     * Indicates that the background task has terminated due to an unrecoverable error.
     */
    private boolean taskTerminated;

    /**
     * Background task for handling asynchronous IO completions.
     */
    @SuppressWarnings("java:S2189")
    private void asyncCompletionTask() {

        try (var arena = Arena.ofConfined()) {

            var overlappedHolder = arena.allocate(ADDRESS);
            var numBytesHolder = arena.allocate(JAVA_INT);
            var completionKeyHolder = arena.allocate(JAVA_LONG);
            var errorState = allocateErrorState(arena);

            while (true) {
                try {
                    overlappedHolder.set(ADDRESS, 0, NULL);
                    completionKeyHolder.set(JAVA_LONG, 0, 0);

                    var res = GetQueuedCompletionStatus(errorState, asyncIoCompletionPort, numBytesHolder,
                            completionKeyHolder, overlappedHolder, INFINITE);
                    var overlappedAddr = overlappedHolder.get(JAVA_LONG, 0);

                    // A null OVERLAPPED means no completion packet was dequeued (nothing posts
                    // packets without an OVERLAPPED): the completion port itself has failed,
                    // and no further completions will ever be delivered.
                    if (overlappedAddr == 0) {
                        var success = res != 0;
                        throwLastError(errorState, "internal error (GetQueuedCompletionStatus, success: %s)", success);
                    }

                    completeTransfer(overlappedAddr);

                } catch (Exception e) {
                    LOG.log(ERROR, "USB async IO thread failed and is terminating; "
                            + "all outstanding transfers will fail, and no further transfers are possible", e);
                    failAllPendingTransfers();
                    return;
                }
            }
        }
    }

    /**
     * Fails all outstanding transfers and marks this task as terminated.
     * <p>
     * Called when the background task can no longer dispatch completions. Waiters blocked
     * on the failed transfers wake up with an error result instead of hanging forever,
     * and future submissions are rejected.
     * </p>
     */
    private void failAllPendingTransfers() {
        List<WindowsTransfer> pendingTransfers;
        synchronized (this) {
            taskTerminated = true;
            pendingTransfers = new ArrayList<>(requestsByOverlapped.values());
            requestsByOverlapped.clear();
            availableOverlappedStructs.clear();
            for (var transfer : pendingTransfers) {
                transfer.setResultCode(ERROR_OPERATION_ABORTED);
                transfer.setResultSize(0);
                transfer.setOverlapped(null);
            }
        }

        for (var transfer : pendingTransfers) {
            try {
                transfer.completion().completed(transfer);
            } catch (Exception e) {
                LOG.log(ERROR, "Unexpected exception while handling async IO completion", e);
            }
        }
    }

    /**
     * Add a Windows handle (of a USB device) to the completion port.
     * <p>
     * The handle is removed by closing it.
     * </p>
     *
     * @param handle Windows handle
     */
    synchronized void addDevice(MemorySegment handle) {

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            // Creates a new port if it doesn't exist; adds handle to existing port if it exists
            var portHandle = CreateIoCompletionPort(errorState, handle, asyncIoCompletionPort,
                    handle.address(), 0);
            if (portHandle == MemorySegment.NULL)
                throwLastError(errorState, "internal error (CreateIoCompletionPort)");

            if (asyncIoCompletionPort == MemorySegment.NULL) {
                asyncIoCompletionPort = portHandle;
                startAsyncIOTask();
            }
        }
    }

    private void startAsyncIOTask() {
        availableOverlappedStructs = new ArrayList<>();
        overlappedArena = Arena.ofAuto();
        requestsByOverlapped = new HashMap<>();

        // start background thread for handling IO completion
        var thread = new Thread(this::asyncCompletionTask, "USB async IO");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Prepare a transfer for submission by adding the OVERLAPPED struct.
     *
     * @param transfer transfer to prepare
     */
    synchronized void prepareForSubmission(WindowsTransfer transfer) {
        if (taskTerminated)
            throw new UsbException("USB async IO background thread has terminated due to an unrecoverable error; "
                    + "USB transfers are no longer possible");

        MemorySegment overlapped;
        var size = availableOverlappedStructs.size();
        if (size == 0) {
            overlapped = OVERLAPPED.allocate(overlappedArena);
        } else {
            overlapped = availableOverlappedStructs.remove(size - 1);
        }

        transfer.setOverlapped(overlapped);
        transfer.setResultSize(-1);
        requestsByOverlapped.put(overlapped.address(), transfer);
    }

    /**
     * Undoes the registration performed by {@link #prepareForSubmission(WindowsTransfer)}.
     * <p>
     * Must be called if the native submission of a prepared transfer fails synchronously.
     * In that case, no completion packet will ever be posted for the transfer, so its map
     * entry would leak and the OVERLAPPED struct would never return to the pool unless
     * they are cleaned up here.
     * </p>
     *
     * @param transfer transfer whose submission failed
     */
    synchronized void submissionFailed(WindowsTransfer transfer) {
        requestsByOverlapped.remove(transfer.overlapped().address());
        availableOverlappedStructs.add(transfer.overlapped());
        transfer.setOverlapped(null);
    }

    /**
     * Completes the transfer by calling the completion handler.
     *
     * @param overlappedAddr address of OVERLAPPED struct
     */
    private void completeTransfer(long overlappedAddr) {
        WindowsTransfer transfer;
        synchronized (this) {
            transfer = requestsByOverlapped.remove(overlappedAddr);
            if (transfer == null)
                return;

            // the results must be read from the OVERLAPPED struct before it is
            // returned to the pool and possibly reused by another submission
            transfer.setResultCode((int) OVERLAPPED.Internal(transfer.overlapped()));
            transfer.setResultSize((int) OVERLAPPED.InternalHigh(transfer.overlapped()));

            availableOverlappedStructs.add(transfer.overlapped());
            transfer.setOverlapped(null);
        }

        // The completion handler must be called without holding the lock: handlers acquire
        // other monitors (transfer, device), and threads submitting transfers acquire this
        // task's lock while holding those monitors, so calling handlers under the lock can
        // deadlock.
        try {
            transfer.completion().completed(transfer);
        } catch (Exception e) {
            // This method runs on the process-wide async IO thread. Any exception escaping
            // would kill that thread and hang all async transfers for the entire library.
            LOG.log(ERROR, "Unexpected exception while handling async IO completion", e);
        }
    }
}
