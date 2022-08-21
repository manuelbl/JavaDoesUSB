//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public class IO {

    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;
    public static final int O_CREAT = 64;
    public static final int O_EXCL = 128;
    public static final int O_TRUNC = 512;
    public static final int O_APPEND = 1024;
    public static final int O_NONBLOCK = 2048;
    public static final int O_CLOEXEC = 524288;

    private static final MethodHandle open$Func;
    private static final MethodHandle ioctl$Func;
    private static final MethodHandle close$Func;
    private static final MethodHandle errno_location$Func;

    static {
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = linker.defaultLookup();

        // int open (const char *__path, int __oflag, ...)
        // (method handle without variadic arguments)
        open$Func = linker.downcallHandle(
                lookup.lookup("open").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT)
        );

        // int ioctl (int __fd, unsigned long int __request, ...);
        ioctl$Func = linker.downcallHandle(
                lookup.lookup("ioctl").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS)
        );

        // int close(int fd);
        close$Func = linker.downcallHandle(
                lookup.lookup("close").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT)
        );

        // int *__errno_location (void)
        errno_location$Func = linker.downcallHandle(
                lookup.lookup("__errno_location").get(),
                FunctionDescriptor.of(ADDRESS)
        );
    }

    // int open (const char *__path, int __oflag, ...)
    public static int open(String path, int oflag) {
        try (var session = MemorySession.openConfined()) {
            var pathCStr = session.allocateUtf8String(path);
            return (int) open$Func.invokeExact((Addressable) pathCStr, oflag);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // int ioctl (int __fd, unsigned long int __request, ...);
    public static int ioctl(int fd, long request, Addressable val) {
        try {
            return (int) ioctl$Func.invokeExact(fd, request, val);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // int close(int fd);
    public static int close(int fd) {
        try {
            return (int) close$Func.invokeExact(fd);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getErrno() {
        try (var session = MemorySession.openConfined()) {
            var location = (MemoryAddress) errno_location$Func.invokeExact();
            var errnoSegment = MemorySegment.ofAddress(location, JAVA_INT.byteSize(), session);
            return errnoSegment.get(JAVA_INT, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
