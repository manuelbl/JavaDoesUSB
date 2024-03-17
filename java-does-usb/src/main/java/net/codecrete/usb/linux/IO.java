//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "SameParameterValue", "java:S100"})
class IO {

    private IO() {
    }

    private static final Linker linker = Linker.nativeLinker();
    private static final FunctionDescriptor ioctl$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS);
    private static final MethodHandle ioctl$MH = linker.downcallHandle(linker.defaultLookup().find("ioctl").get(),
            ioctl$FUNC, Linux.ERRNO_STATE, Linker.Option.firstVariadicArg(2));
    private static final FunctionDescriptor open$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
    private static final MethodHandle open$MH = linker.downcallHandle(linker.defaultLookup().find("open").get(),
            open$FUNC, Linux.ERRNO_STATE);

    static int ioctl(int fd, long request, MemorySegment segment, MemorySegment errno) {
        try {
            return (int) ioctl$MH.invokeExact(errno, fd, request, segment);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static int open(MemorySegment file, int oflag, MemorySegment errno) {
        try {
            return (int) open$MH.invokeExact(errno, file, oflag);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }
}
