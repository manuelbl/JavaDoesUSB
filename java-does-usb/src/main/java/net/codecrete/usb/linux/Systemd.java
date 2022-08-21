//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Functions and data structures of systemd.
 */
public class Systemd {

    private static final MethodHandle sd_device_enumerator_new$Func;
    private static final MethodHandle sd_device_enumerator_unref$Func;
    private static final MethodHandle sd_device_enumerator_add_match_subsystem$Func;
    private static final MethodHandle sd_device_enumerator_get_device_first$Func;
    private static final MethodHandle sd_device_enumerator_get_device_next$Func;
    private static final MethodHandle sd_device_get_sysattr_value$Func;
    private static final MethodHandle sd_device_get_devname$Func;

    static {
        var session = MemorySession.openShared();
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.libraryLookup("libsystemd.so.0", session);

        // int sd_device_enumerator_new(sd_device_enumerator **ret);
        sd_device_enumerator_new$Func = linker.downcallHandle(
                lookup.lookup("sd_device_enumerator_new").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );

        // sd_device_enumerator *sd_device_enumerator_unref(sd_device_enumerator *enumerator);
        sd_device_enumerator_unref$Func = linker.downcallHandle(
                lookup.lookup("sd_device_enumerator_unref").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS)
        );

        // int sd_device_enumerator_add_match_subsystem(sd_device_enumerator *enumerator, const char *subsystem, int match);
        sd_device_enumerator_add_match_subsystem$Func = linker.downcallHandle(
                lookup.lookup("sd_device_enumerator_add_match_subsystem").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)
        );

        // sd_device *sd_device_enumerator_get_device_first(sd_device_enumerator *enumerator);
        sd_device_enumerator_get_device_first$Func = linker.downcallHandle(
                lookup.lookup("sd_device_enumerator_get_device_first").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS)
        );

        // sd_device *sd_device_enumerator_get_device_next(sd_device_enumerator *enumerator);
        sd_device_enumerator_get_device_next$Func = linker.downcallHandle(
                lookup.lookup("sd_device_enumerator_get_device_next").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS)
        );

        // int sd_device_get_sysattr_value(sd_device *device, const char *sysattr, const char **_value);
        sd_device_get_sysattr_value$Func = linker.downcallHandle(
                lookup.lookup("sd_device_get_sysattr_value").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
        );

        // int sd_device_get_devname(sd_device *device, const char **ret);
        sd_device_get_devname$Func = linker.downcallHandle(
                lookup.lookup("sd_device_get_devname").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
        );
    }

    // int sd_device_enumerator_new(sd_device_enumerator **ret);
    public static int sd_device_enumerator_new(Addressable ret) {
        try {
            return (int) sd_device_enumerator_new$Func.invokeExact(ret);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // sd_device_enumerator *sd_device_enumerator_unref(sd_device_enumerator *enumerator);
    public static MemoryAddress sd_device_enumerator_unref(Addressable enumerator) {
        try {
            return (MemoryAddress) sd_device_enumerator_unref$Func.invokeExact(enumerator);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // int sd_device_enumerator_add_match_subsystem(sd_device_enumerator *enumerator, const char *subsystem, int match);
    public static int sd_device_enumerator_add_match_subsystem(Addressable enumerator, String subsystem, int match) {
        try (var session = MemorySession.openConfined()) {
            var subsys = session.allocateUtf8String(subsystem);
            return (int) sd_device_enumerator_add_match_subsystem$Func.invokeExact(enumerator, (Addressable) subsys, match);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // sd_device *sd_device_enumerator_get_device_first(sd_device_enumerator *enumerator);
    public static MemoryAddress sd_device_enumerator_get_device_first(Addressable enumerator) {
        try {
            return (MemoryAddress) sd_device_enumerator_get_device_first$Func.invokeExact(enumerator);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // sd_device *sd_device_enumerator_get_device_next(sd_device_enumerator *enumerator);
    public static MemoryAddress sd_device_enumerator_get_device_next(Addressable enumerator) {
        try {
            return (MemoryAddress) sd_device_enumerator_get_device_next$Func.invokeExact(enumerator);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // int sd_device_get_sysattr_value(sd_device *device, const char *sysattr, const char **_value);
    public static int sd_device_get_sysattr_value(Addressable device, Addressable sysattr, Addressable value) {
        try {
            return (int) sd_device_get_sysattr_value$Func.invokeExact(device, sysattr, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // int sd_device_get_devname(sd_device *device, const char **ret);
    public static int sd_device_get_devname(Addressable device, Addressable ret) {
        try {
            return (int) sd_device_get_devname$Func.invokeExact(device, ret);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
