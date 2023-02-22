//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.*;

public class IO {

    /**
     * Call state for capturing the {@code errno} value.
     */
    public static final Linker.Option.CaptureCallState ERRNO_STATE = Linker.Option.captureCallState("errno");

    private static final VarHandle callState_errno$VH = ERRNO_STATE.layout().varHandle(MemoryLayout.PathElement.groupElement("errno"));

    private static final Linker linker = Linker.nativeLinker();
    private static final FunctionDescriptor ioctl$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS);
    private static final MethodHandle ioctl$MH = linker.downcallHandle(linker.defaultLookup().find("ioctl").get(), ioctl$FUNC, ERRNO_STATE);
    private static final FunctionDescriptor open$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
    private static final MethodHandle open$MH = linker.downcallHandle(linker.defaultLookup().find("open").get(), open$FUNC, ERRNO_STATE);
    private static final FunctionDescriptor eventfd$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT);
    private static final MethodHandle eventfd$MH = linker.downcallHandle(linker.defaultLookup().find("eventfd").get(), eventfd$FUNC, ERRNO_STATE);
    private static final FunctionDescriptor eventfd_read$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
    private static final MethodHandle eventfd_read$MH = linker.downcallHandle(linker.defaultLookup().find("eventfd_read").get(), eventfd_read$FUNC, ERRNO_STATE);
    private static final FunctionDescriptor eventfd_write$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG);
    private static final MethodHandle eventfd_write$MH = linker.downcallHandle(linker.defaultLookup().find("eventfd_write").get(), eventfd_write$FUNC, ERRNO_STATE);

    public static int ioctl(int fd, long request, MemorySegment segment, MemorySegment errno) {
        try {
            return (int) ioctl$MH.invokeExact(errno, fd, request, segment);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int open(MemorySegment file, int oflag, MemorySegment errno) {
        try {
            return (int) open$MH.invokeExact(errno, file, oflag);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int eventfd(int count, int flags, MemorySegment errno) {
        try {
            return (int) eventfd$MH.invokeExact(errno, count, flags);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int eventfd_read(int fd, MemorySegment value, MemorySegment errno) {
        try {
            return (int) eventfd_read$MH.invokeExact(errno, fd, value);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int eventfd_write(int fd, long value, MemorySegment errno) {
        try {
            return (int) eventfd_write$MH.invokeExact(errno, fd, value);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the error code from the memory segment.
     * <p>
     * The memory segment is assumed to have the layout {@link #ERRNO_STATE}.
     * </p>
     *
     * @param errno memory segment with error code
     * @return error code
     */
    public static int getErrno(MemorySegment errno) {
        return (int) callState_errno$VH.get(errno);
    }
}
