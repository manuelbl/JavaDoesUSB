//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.kernel32.OVERLAPPED;
import net.codecrete.usb.windows.winsdk.Kernel32B;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

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
public class WindowsAsyncTask {

    private static WindowsAsyncTask singletonInstance;

    /**
     * Singleton instance of background task.
     *
     * @return background task
     */
    static synchronized WindowsAsyncTask instance() {
        if (singletonInstance == null)
            singletonInstance = new WindowsAsyncTask();
        return singletonInstance;
    }

    // Currently outstanding transfer requests,
    // indexed by OVERLAPPED address.
    private Map<Long, WindowsTransfer> requestsByOverlapped;
    // available OVERLAPPED data structures
    private List<MemorySegment> availableOverlappedStructs;
    // Arena used to allocate OVERLAPPED data structures
    private Arena arena;

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
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            while (true) {
                overlappedHolder.set(ADDRESS, 0, NULL);
                completionKeyHolder.set(JAVA_LONG, 0, 0);

                int res = Kernel32B.GetQueuedCompletionStatus(asyncIoCompletionPort, numBytesHolder,
                        completionKeyHolder, overlappedHolder, Kernel32.INFINITE(), lastErrorState);
                var overlappedAddr = overlappedHolder.get(JAVA_LONG, 0);

                if (res == 0 && overlappedAddr == 0)
                    throwLastError(lastErrorState, "Internal error (SetupDiGetDeviceInterfaceDetailW)");

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
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            // Creates a new port if it doesn't exist; adds handle to existing port if it exists
            MemorySegment portHandle = Kernel32B.CreateIoCompletionPort(handle, asyncIoCompletionPort,
                    handle.address(), 0, lastErrorState);
            if (portHandle == MemorySegment.NULL)
                throwLastError(lastErrorState, "internal error (CreateIoCompletionPort)");

            if (asyncIoCompletionPort == MemorySegment.NULL) {
                asyncIoCompletionPort = portHandle;
                startAsyncIOTask();
            }
        }
    }

    private void startAsyncIOTask() {
        availableOverlappedStructs = new ArrayList<>();
        arena = Arena.ofAuto();
        requestsByOverlapped = new HashMap<>();

        // start background thread for handling IO completion
        Thread t = new Thread(this::asyncCompletionTask, "USB async IO");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Prepare a transfer for submission by adding the OVERLAPPED struct.
     *
     * @param transfer transfer to prepare
     */
    synchronized void prepareForSubmission(WindowsTransfer transfer) {
        MemorySegment overlapped;
        int size = availableOverlappedStructs.size();
        if (size == 0) {
            overlapped = arena.allocate(OVERLAPPED.$LAYOUT());
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

        transfer.setResultCode((int) OVERLAPPED.Internal$get(transfer.overlapped()));
        transfer.setResultSize((int) OVERLAPPED.InternalHigh$get(transfer.overlapped()));

        availableOverlappedStructs.add(transfer.overlapped());
        transfer.setOverlapped(null);
        transfer.completion().completed(transfer);
    }
}
