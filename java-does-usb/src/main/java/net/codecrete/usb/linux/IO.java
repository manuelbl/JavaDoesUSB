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

import static java.lang.foreign.ValueLayout.*;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "SameParameterValue", "java:S100"})
class IO {

    private IO() {
    }

    private static final Linker linker = Linker.nativeLinker();
    private static final FunctionDescriptor ioctl$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS);
    private static final MethodHandle ioctl$MH = linker.downcallHandle(linker.defaultLookup().find("ioctl").get(),
            ioctl$FUNC, Linux.ERRNO_STATE);
    private static final FunctionDescriptor open$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
    private static final MethodHandle open$MH = linker.downcallHandle(linker.defaultLookup().find("open").get(),
            open$FUNC, Linux.ERRNO_STATE);
    private static final FunctionDescriptor eventfd$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT);
    private static final MethodHandle eventfd$MH = linker.downcallHandle(linker.defaultLookup().find("eventfd").get()
            , eventfd$FUNC, Linux.ERRNO_STATE);
    private static final FunctionDescriptor eventfd_read$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
    private static final MethodHandle eventfd_read$MH = linker.downcallHandle(linker.defaultLookup().find(
            "eventfd_read").get(), eventfd_read$FUNC, Linux.ERRNO_STATE);
    private static final FunctionDescriptor eventfd_write$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG);
    private static final MethodHandle eventfd_write$MH = linker.downcallHandle(linker.defaultLookup().find(
            "eventfd_write").get(), eventfd_write$FUNC, Linux.ERRNO_STATE);

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

    static int eventfd(int count, int flags, MemorySegment errno) {
        try {
            return (int) eventfd$MH.invokeExact(errno, count, flags);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static int eventfd_read(int fd, MemorySegment value, MemorySegment errno) {
        try {
            return (int) eventfd_read$MH.invokeExact(errno, fd, value);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static int eventfd_write(int fd, long value, MemorySegment errno) {
        try {
            return (int) eventfd_write$MH.invokeExact(errno, fd, value);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

}
