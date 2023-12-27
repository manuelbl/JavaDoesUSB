//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.epoll.epoll_event;
import net.codecrete.usb.linux.gen.errno.errno;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.linux.Linux.allocateErrorState;
import static net.codecrete.usb.linux.LinuxUsbException.throwLastError;
import static net.codecrete.usb.linux.gen.epoll.epoll.*;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "SameParameterValue", "java:S100"})
public class EPoll {
    private EPoll() {}

    private static final Linker linker = Linker.nativeLinker();

    private static final FunctionDescriptor epoll_create$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT);
    private static final MethodHandle epoll_create$MH = linker.downcallHandle(linker.defaultLookup().find(
            "epoll_create").get(), epoll_create$FUNC, Linux.ERRNO_STATE);

    private static final FunctionDescriptor epoll_ctl$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    private static final MethodHandle epoll_ctl$MH = linker.downcallHandle(linker.defaultLookup().find(
            "epoll_ctl").get(), epoll_ctl$FUNC, Linux.ERRNO_STATE);

    private static final FunctionDescriptor epoll_wait$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
    private static final MethodHandle epoll_wait$MH = linker.downcallHandle(linker.defaultLookup().find(
            "epoll_wait").get(), epoll_wait$FUNC, Linux.ERRNO_STATE);

    private static final VarHandle epoll_event_data_fd$VH = epoll_event.$LAYOUT().varHandle(
            MemoryLayout.PathElement.groupElement("data"),
            MemoryLayout.PathElement.groupElement("fd")
    );

    static int epoll_create(int size, MemorySegment errno) {
        try {
            return (int) epoll_create$MH.invokeExact(errno, size);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    private static int epoll_ctl(int epfd, int op, int fd, MemorySegment event, MemorySegment errno) {
        try {
            return (int) epoll_ctl$MH.invokeExact(errno, epfd, op, fd, event);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static int epoll_wait(int epfd, MemorySegment events, int maxevent, int timeout, MemorySegment errno) {
        try {
            return (int) epoll_wait$MH.invokeExact(errno, epfd, events, maxevent, timeout);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static void addFileDescriptor(int epfd, int op, int fd) {
        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            var event = arena.allocate(epoll_event.$LAYOUT());
            epoll_event.events$set(event, op);
            epoll_event_data_fd$VH.set(event, fd);
            var ret = epoll_ctl(epfd, EPOLL_CTL_ADD(), fd, event, errorState);
            if (ret < 0)
                throwLastError(errorState, "internal error (epoll_ctl_add)");
        }
    }

    static void removeFileDescriptor(int epfd, int fd) {
        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            var event = arena.allocate(epoll_event.$LAYOUT());
            epoll_event.events$set(event, 0);
            epoll_event_data_fd$VH.set(event, fd);
            var ret = epoll_ctl(epfd, EPOLL_CTL_DEL(), fd, event, errorState);
            if (ret < 0) {
                var err = Linux.getErrno(errorState);
                if (err != errno.ENOENT())
                    throwLastError(errorState, "internal error (epoll_ctl_del)");
            }
        }
    }
}
