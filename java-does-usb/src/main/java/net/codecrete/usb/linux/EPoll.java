//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.errno.errno;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.ADDRESS_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static net.codecrete.usb.linux.Linux.allocateErrorState;
import static net.codecrete.usb.linux.LinuxUsbException.throwLastError;
import static net.codecrete.usb.linux.gen.epoll.epoll.EPOLL_CTL_ADD;
import static net.codecrete.usb.linux.gen.epoll.epoll.EPOLL_CTL_DEL;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "SameParameterValue", "java:S100", "java:S1192"})
public class EPoll {
    private EPoll() {}

    private static final boolean IS_AARCH64 = System.getProperty("os.arch").equals("aarch64");

    private static final GroupLayout DATA$LAYOUT = MemoryLayout.unionLayout(
        ADDRESS_UNALIGNED.withName("ptr"),
        JAVA_INT_UNALIGNED.withName("fd"),
        JAVA_INT_UNALIGNED.withName("u32"),
        JAVA_LONG_UNALIGNED.withName("u64")
    ).withName("epoll_data");

    static final GroupLayout EVENT$LAYOUT = IS_AARCH64
            ? MemoryLayout.structLayout(
                    JAVA_INT.withName("events"),
                    MemoryLayout.paddingLayout(4),
                    DATA$LAYOUT.withName("data")).withName("epoll_event")
            : MemoryLayout.structLayout(
                    JAVA_INT_UNALIGNED.withName("events"),
                    DATA$LAYOUT.withName("data")).withName("epoll_event");

    // varhandle to access the "fd" field in an epoll_event array
    static final VarHandle EVENT_ARRAY_DATA_FD$VH = EVENT$LAYOUT.arrayElementVarHandle(
            MemoryLayout.PathElement.groupElement("data"),
            MemoryLayout.PathElement.groupElement("fd")
    );

    // varhandle to access the "fd" field in an epoll_event struct
    private static final VarHandle EVENT_DATA_FD$VH = EVENT$LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("data"),
            MemoryLayout.PathElement.groupElement("fd")
    );

    private static final VarHandle EVENTS$VH = EVENT$LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("events")
    );

    private static final Linker linker = Linker.nativeLinker();

    private static final FunctionDescriptor epoll_create1$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT);
    private static final MethodHandle epoll_create1$MH = linker.downcallHandle(linker.defaultLookup().find(
            "epoll_create").get(), epoll_create1$FUNC, Linux.ERRNO_STATE);

    private static final FunctionDescriptor epoll_ctl$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    private static final MethodHandle epoll_ctl$MH = linker.downcallHandle(linker.defaultLookup().find(
            "epoll_ctl").get(), epoll_ctl$FUNC, Linux.ERRNO_STATE);

    private static final FunctionDescriptor epoll_wait$FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
    private static final MethodHandle epoll_wait$MH = linker.downcallHandle(linker.defaultLookup().find(
            "epoll_wait").get(), epoll_wait$FUNC, Linux.ERRNO_STATE);

    static int epoll_create1(int flags, MemorySegment errno) {
        try {
            return (int) epoll_create1$MH.invokeExact(errno, flags);
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

            var event = arena.allocate(EVENT$LAYOUT);
            EVENTS$VH.set(event, 0, op);
            EVENT_DATA_FD$VH.set(event, 0, fd);
            var ret = epoll_ctl(epfd, EPOLL_CTL_ADD(), fd, event, errorState);
            if (ret < 0)
                throwLastError(errorState, "internal error (epoll_ctl_add)");
        }
    }

    static void removeFileDescriptor(int epfd, int fd) {
        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            var event = arena.allocate(EVENT$LAYOUT);
            EVENTS$VH.set(event, 0, 0);
            EVENT_DATA_FD$VH.set(event, 0, fd);
            var ret = epoll_ctl(epfd, EPOLL_CTL_DEL(), fd, event, errorState);
            if (ret < 0) {
                var err = Linux.getErrno(errorState);
                // ignore ENOENT as this method might be called twice when cleaning up
                if (err != errno.ENOENT())
                    throwLastError(errorState, "internal error (epoll_ctl_del)");
            }
        }
    }
}
