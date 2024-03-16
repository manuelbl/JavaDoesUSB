//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.UsbException;
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

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;


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
    private MemorySegment messagePort;
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
                if (state == TaskState.NOT_STARTED)
                    startAsyncIOThread();
                waitForRunLoopReady();
            }

            CoreFoundation.CFRunLoopAddSource(asyncIoRunLoop, source, IOKit.kCFRunLoopDefaultMode());

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
     * <p>
     * The event source is not immediately removed. Instead, it is posted to a message queue
     * processed by the same background thread processing the completion callbacks. This ensures
     * that the events from releasing interfaces and closing devices are processed.
     * </p>
     * @param source event source
     */
    void removeEventSource(MemorySegment source) {
        try (var arena = Arena.ofConfined()) {
            var eventSourceRef = arena.allocate(JAVA_LONG, 1);
            eventSourceRef.set(JAVA_LONG, 0, source.address());
            var dataRef = CoreFoundation.CFDataCreate(NULL, eventSourceRef, eventSourceRef.byteSize());
            CoreFoundation.CFMessagePortSendRequest(messagePort, 0, dataRef, 0, 0, NULL, NULL);
            CoreFoundation.CFRelease(dataRef);
        }
    }

    /**
     * Starts the background thread.
     */
    @SuppressWarnings("java:S125")
    private void startAsyncIOThread() {
        MemorySegment messagePortSource;

        try {
            state = TaskState.STARTING;

            // create descriptor for completion callback function
            var completionHandlerFuncDesc = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
            var asyncIOCompletedMH = MethodHandles.lookup().findVirtual(MacosAsyncTask.class, "asyncIOCompleted",
                    MethodType.methodType(void.class, MemorySegment.class, int.class, MemorySegment.class));

            var methodHandle = asyncIOCompletedMH.bindTo(this);
            completionUpcallStub = Linker.nativeLinker().upcallStub(methodHandle, completionHandlerFuncDesc, Arena.global());

            // create descriptor for message port callback function
            var messagePortCallbackFuncDec = FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
            var messagePortCallbackMH = MethodHandles.lookup().findVirtual(MacosAsyncTask.class, "messagePortCallback",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class, int.class, MemorySegment.class, MemorySegment.class));
            var messagePortCallbackHandle = messagePortCallbackMH.bindTo(this);
            var messagePortCallbackStub = Linker.nativeLinker().upcallStub(messagePortCallbackHandle, messagePortCallbackFuncDec, Arena.global());

            // create local and remote message ports
            var pid = ProcessHandle.current().pid();
            var portName = CoreFoundationHelper.createCFStringRef("net.codecrete.usb.macos.eventsource." + pid, Arena.global());
            var localPort = CoreFoundation.CFMessagePortCreateLocal(NULL, portName, messagePortCallbackStub, NULL, NULL);
            messagePortSource = CoreFoundation.CFMessagePortCreateRunLoopSource(NULL, localPort, 0);
            messagePort = CoreFoundation.CFMessagePortCreateRemote(NULL, portName);

        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new UsbException("internal error (creating method handle)", e);
        }

        var thread = new Thread(() -> asyncIOCompletionTask(messagePortSource), "USB async IO");
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
            CoreFoundation.CFRunLoopAddSource(asyncIoRunLoop, firstSource, IOKit.kCFRunLoopDefaultMode());
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
     * Callback function called when a message is received on the message port.
     * <p>
     * All messages are related to removing event sources. They just contain the run loop source reference.
     * </p>
     */
    @SuppressWarnings({"java:S1144", "unused"})
    private MemorySegment messagePortCallback(MemorySegment local, int msgid, MemorySegment data, MemorySegment info) {
        var runloopSourceRefPtr = CoreFoundation.CFDataGetBytePtr(data);
        var runloopSourceRef = MemorySegment.ofAddress(runloopSourceRefPtr.get(JAVA_LONG_UNALIGNED, 0));
        CoreFoundation.CFRunLoopRemoveSource(asyncIoRunLoop, runloopSourceRef, IOKit.kCFRunLoopDefaultMode());
        return NULL;
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
