//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows.winsdk;

import net.codecrete.usb.windows.Win;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Native function calls for Kernel32.
 * <p>
 * This code is manually created to include the additional parameters for capturing
 * {@code GetLastError()} until jextract catches up and can generate the corresponding code.
 * </p>
 */
public class Kernel32B {
    static {
        System.loadLibrary("Kernel32");
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();


    private static final FunctionDescriptor CreateFileW$FUNC =
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);

    private static final MethodHandle CreateFileW$MH = LINKER.downcallHandle(
            LOOKUP.find("CreateFileW").get(),
            CreateFileW$FUNC,
            Win.LAST_ERROR_STATE
    );

    public static MemorySegment CreateFileW(MemorySegment lpFileName, int dwDesiredAccess, int dwShareMode,
                                            MemorySegment lpSecurityAttributes, int dwCreationDisposition,
                                            int dwFlagsAndAttributes, MemorySegment hTemplateFile,
                                            MemorySegment lastErrorState) {
        try {
            return (MemorySegment)CreateFileW$MH.invokeExact(lastErrorState, lpFileName, dwDesiredAccess, dwShareMode,
                    lpSecurityAttributes, dwCreationDisposition, dwFlagsAndAttributes, hTemplateFile);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor DeviceIoControl$FUNC =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    private static final MethodHandle DeviceIoControl$MH = LINKER.downcallHandle(
            LOOKUP.find("DeviceIoControl").get(),
            DeviceIoControl$FUNC,
            Win.LAST_ERROR_STATE
    );

    public static int DeviceIoControl(MemorySegment hDevice, int dwIoControlCode, MemorySegment lpInBuffer,
                                      int nInBufferSize, MemorySegment lpOutBuffer, int nOutBufferSize,
                                      MemorySegment lpBytesReturned, MemorySegment lpOverlapped, MemorySegment lastErrorState) {
        try {
            return (int)DeviceIoControl$MH.invokeExact(lastErrorState, hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}