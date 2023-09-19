//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBException;
import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;
import net.codecrete.usb.macos.gen.iokit.IOKit;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Background task for handling asynchronous transfers.
 * <p>
 * Each USB device and interface must register its event source with this task.
 * </p>
 * <p>
 * The task assigns a consecutive number to transfers. It is used in the
 * {@code refcon} argument to match callbacks with the submitted transfer.
 * </p>
 */
@SuppressWarnings("java:S6548")
class MacosAsyncTask {

    enum TaskState {
        NOT_STARTED,
        STARTING,
        RUNNING
    }

    /**
     * Singleton instance of background task.
     */
    static final MacosAsyncTask INSTANCE = new MacosAsyncTask();

    private final ReentrantLock asyncIoLock = new ReentrantLock();
    private final Condition asyncIoReady = asyncIoLock.newCondition();
    private TaskState state = TaskState.NOT_STARTED;
    private MemorySegment asyncIoRunLoop;
    private MemorySegment completionUpcallStub;
    private long lastTransferId;
    private final Map<Long, MacosTransfer> transfersById = new HashMap<>();

    /**
     * Adds an event source to this background.
     *
     * @param source event source
     */
    void addEventSource(MemorySegment source) {
        try {
            asyncIoLock.lock();

            if (state != TaskState.RUNNING) {
                if (state == TaskState.NOT_STARTED) {
                    startAsyncIOThread(source);
                    waitForRunLoopReady();
                    return;

                } else {
                    // special case: run loop is not ready yet but background process is already starting
                    waitForRunLoopReady();
                }
            }

            CoreFoundation.CFRunLoopAddSource(asyncIoRunLoop, source, IOKit.kCFRunLoopDefaultMode$get());

        } finally {
            asyncIoLock.unlock();
        }
    }

    private void waitForRunLoopReady() {
        while (state != TaskState.RUNNING)
            asyncIoReady.awaitUninterruptibly();
    }

    /**
     * Removes an event source from this background task.
     *
     * @param source event source
     */
    void removeEventSource(MemorySegment source) {
        CoreFoundation.CFRunLoopRemoveSource(asyncIoRunLoop, source, IOKit.kCFRunLoopDefaultMode$get());
    }

    /**
     * Starts the background thread.
     *
     * @param firstSource first event source
     */
    private void startAsyncIOThread(MemorySegment firstSource) {
        try {
            state = TaskState.STARTING;
            var completionHandlerFuncDesc = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
            var asyncIOCompletedMH = MethodHandles.lookup().findVirtual(MacosAsyncTask.class, "asyncIOCompleted",
                    MethodType.methodType(void.class, MemorySegment.class, int.class, MemorySegment.class));

            var methodHandle = asyncIOCompletedMH.bindTo(this);
            completionUpcallStub = Linker.nativeLinker().upcallStub(methodHandle, completionHandlerFuncDesc,
                    Arena.global());

        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new USBException("internal error (creating method handle)", e);
        }

        var thread = new Thread(() -> asyncIOCompletionTask(firstSource), "USB async IO");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Background task calling the completion handlers.
     * <p>
     * Without an initial event source, the run loop will immediately exit.
     * Later it has no problems if the number of event sources drops to 0.
     * </p>
     *
     * @param firstSource first event source
     */
    private void asyncIOCompletionTask(MemorySegment firstSource) {
        try {
            asyncIoLock.lock();
            asyncIoRunLoop = CoreFoundation.CFRunLoopGetCurrent();
            CoreFoundation.CFRunLoopAddSource(asyncIoRunLoop, firstSource, IOKit.kCFRunLoopDefaultMode$get());
            state = TaskState.RUNNING;
            asyncIoReady.signalAll();
        } finally {
            asyncIoLock.unlock();
        }

        // loop forever
        CoreFoundation.CFRunLoopRun();
    }

    /**
     * Prepare a transfer for submission by assigning it an ID
     * and remembering the association to the transfer.
     * <p>
     * Each submission needs to be prepared separately.
     * </p>
     *
     * @param transfer transfer
     */
    synchronized void prepareForSubmission(MacosTransfer transfer) {
        lastTransferId += 1;
        transfer.setId(lastTransferId);
        transfer.setResultSize(-1);
        transfersById.put(lastTransferId, transfer);
    }

    /**
     * Callback function called when an asynchronous transfer has completed.
     *
     * @param refcon contains transfer ID
     * @param result contains result code
     * @param arg0   contains actual length of transferred data
     */
    @SuppressWarnings("java:S1144")
    private void asyncIOCompleted(MemorySegment refcon, int result, MemorySegment arg0) {

        MacosTransfer transfer;
        synchronized (this) {
            transfer = transfersById.remove(refcon.address());
        }

        transfer.setResultCode(result);
        transfer.setResultSize((int) arg0.address());
        transfer.completion().completed(transfer);
    }

    /**
     * Gets the native IO completion callback function for asynchronous transfers
     * to be handled by this background task.
     *
     * @return function pointer
     */
    MemorySegment nativeCompletionCallback() {
        return completionUpcallStub;
    }
}
