//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * MacOS Kernel functions, and a workaround to load Cocoa frameworks
 */
public class Kernel {
    public static final int RTLD_LAZY = 1;

    private static final MethodHandle mach_error;
    private static final MethodHandle dlopen$Func;
    private static final MethodHandle dlsym$Func;
    private static final MethodHandle dlclose$Func;

    static {
        var linker = Linker.nativeLinker();
        var defaultLookup = linker.defaultLookup();
        mach_error = linker.downcallHandle(
                defaultLookup.lookup("mach_error").get(),
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT)
        );
        dlopen$Func = linker.downcallHandle(
                defaultLookup.lookup("dlopen").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT)
        );
        dlsym$Func = linker.downcallHandle(
                defaultLookup.lookup("dlsym").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)
        );
        dlclose$Func = linker.downcallHandle(
                defaultLookup.lookup("dlclose").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );
    }

    public static void mach_error(String str, int errorValue) {
        try (var session = MemorySession.openConfined()) {
            mach_error.invokeExact((Addressable) session.allocateUtf8String(str), errorValue);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Addressable dlopen(String libname, int dlopenOptions) {
        try (var session = MemorySession.openConfined()) {
            return (MemoryAddress) dlopen$Func.invokeExact((Addressable) session.allocateUtf8String(libname), dlopenOptions);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemoryAddress dlsym(Addressable handle, String symbolName) {
        try (var session = MemorySession.openConfined()) {
            return (MemoryAddress) dlsym$Func.invokeExact(handle, (Addressable) session.allocateUtf8String(symbolName));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int dlclose(Addressable handle) {
        try {
            return (int) dlclose$Func.invokeExact(handle);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static SymbolLookup libraryLookup(String libraryName, MemorySession session) {
        final Addressable handle = dlopen(libraryName, RTLD_LAZY);
        if (handle == NULL)
            throw new IllegalArgumentException("Cannot find library: " + libraryName);

        session.addCloseAction(() -> dlclose(handle));

        return name -> {
            MemoryAddress addr = dlsym(handle, name);
            return addr == NULL ?
                    Optional.empty() :
                    Optional.of(MemorySegment.ofAddress(addr, 0L, session));
        };
    }

    public static SymbolLookup frameworkLookup(String framework, MemorySession session) {
        return libraryLookup(framework + ".framework/" + framework, session);
    }
}
