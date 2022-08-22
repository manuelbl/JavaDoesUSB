//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySession;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * MacOS Kernel functions, and a workaround to load Cocoa frameworks
 */
public class Kernel {
    private static final MethodHandle mach_error;

    static {
        var linker = Linker.nativeLinker();
        var defaultLookup = linker.defaultLookup();
        mach_error = linker.downcallHandle(
                defaultLookup.lookup("mach_error").get(),
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT)
        );
    }

    public static void mach_error(String str, int errorValue) {
        try (var session = MemorySession.openConfined()) {
            mach_error.invokeExact((Addressable) session.allocateUtf8String(str), errorValue);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
