//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.kernel32._OVERLAPPED;
import net.codecrete.usb.windows.winsdk.Kernel32B;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUsbException.throwLastError;

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
     * Background task for handling asynchronous IO completions.
     */
    private void asyncCompletionTask() {

        try (var arena = Arena.ofConfined()) {

            var overlappedHolder = arena.allocate(ADDRESS, NULL);
            var numBytesHolder = arena.allocate(JAVA_INT, 0);
            var completionKeyHolder = arena.allocate(JAVA_LONG, 0);
            var errorState = allocateErrorState(arena);

            while (true) {
                overlappedHolder.set(ADDRESS, 0, NULL);
                completionKeyHolder.set(JAVA_LONG, 0, 0);

                var res = Kernel32B.GetQueuedCompletionStatus(asyncIoCompletionPort, numBytesHolder,
                        completionKeyHolder, overlappedHolder, Kernel32.INFINITE(), errorState);
                var overlappedAddr = overlappedHolder.get(JAVA_LONG, 0);

                if (res == 0 && overlappedAddr == 0)
                    throwLastError(errorState, "internal error (SetupDiGetDeviceInterfaceDetailW)");

                if (overlappedAddr == 0)
                    return; // registry closing?

                completeTransfer(overlappedAddr);
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
            var portHandle = Kernel32B.CreateIoCompletionPort(handle, asyncIoCompletionPort,
                    handle.address(), 0, errorState);
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
        MemorySegment overlapped;
        var size = availableOverlappedStructs.size();
        if (size == 0) {
            overlapped = _OVERLAPPED.allocate(overlappedArena);
        } else {
            overlapped = availableOverlappedStructs.remove(size - 1);
        }

        transfer.setOverlapped(overlapped);
        transfer.setResultSize(-1);
        requestsByOverlapped.put(overlapped.address(), transfer);
    }

    /**
     * Completes the transfer by calling the completion handler.
     *
     * @param overlappedAddr address of OVERLAPPED struct
     */
    private synchronized void completeTransfer(long overlappedAddr) {
        var transfer = requestsByOverlapped.remove(overlappedAddr);
        if (transfer == null)
            return;

        transfer.setResultCode((int) _OVERLAPPED.Internal$get(transfer.overlapped()));
        transfer.setResultSize((int) _OVERLAPPED.InternalHigh$get(transfer.overlapped()));

        availableOverlappedStructs.add(transfer.overlapped());
        transfer.setOverlapped(null);
        transfer.completion().completed(transfer);
    }
}
