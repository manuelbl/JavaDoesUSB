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

    /**
     * {@snippet lang=c :
     * int ioctl(int fd, unsigned long request, ...);
     * }
     */
    public static int ioctl(int fd, long request, MemorySegment segment, MemorySegment errno) {
        try {
            return (int)ioctl$MH.invokeExact(errno, fd, request, segment);
        } catch (Throwable ex) {
            throw new AssertionError("should not reach here", ex);
        }
    }

    /**
     * {@snippet lang=c :
     * int open(char* file, int oflag, ...);
     * }
     */
    public static int open(MemorySegment file, int oflag, MemorySegment errno) {
        try {
            return (int)open$MH.invokeExact(errno, file, oflag);
        } catch (Throwable ex) {
            throw new AssertionError("should not reach here", ex);
        }
    }

    /**
     * Gets the error code from the memory segment.
     * <p>
     * The memory segment is assumed to have the layout {@link #ERRNO_STATE}.
     * </p>
     * @param errno memory segment with error code
     * @return error code
     */
    public static int getErrno(MemorySegment errno) {
        return (int) callState_errno$VH.get(errno);
    }
}
