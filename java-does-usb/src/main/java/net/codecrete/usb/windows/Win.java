//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * Windows Kernel32 functions and Windows helpers.
 */
public class Win {

    public static final int ERROR_NO_MORE_ITEMS = 259;

    public static final int GENERIC_READ = 0x80000000;
    public static final int GENERIC_WRITE = 0x40000000;
    public static final int GENERIC_EXECUTE = 0x20000000;
    public static final int GENERIC_ALL = 0x10000000;

    public static final int FILE_SHARE_READ = 0x00000001;
    public static final int FILE_SHARE_WRITE = 0x00000002;
    public static final int FILE_SHARE_DELETE = 0x00000004;

    public static final int FILE_ATTRIBUTE_READONLY = 0x00000001;
    public static final int FILE_ATTRIBUTE_HIDDEN = 0x00000002;
    public static final int FILE_ATTRIBUTE_SYSTEM = 0x00000004;
    public static final int FILE_ATTRIBUTE_DIRECTORY = 0x00000010;
    public static final int FILE_ATTRIBUTE_ARCHIVE = 0x00000020;
    public static final int FILE_ATTRIBUTE_DEVICE = 0x00000040;
    public static final int FILE_ATTRIBUTE_NORMAL = 0x00000080;
    public static final int FILE_ATTRIBUTE_TEMPORARY = 0x00000100;

    public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000;
    public static final int FILE_FLAG_OVERLAPPED = 0x40000000;
    public static final int FILE_FLAG_NO_BUFFERING = 0x20000000;
    public static final int FILE_FLAG_RANDOM_ACCESS = 0x10000000;
    public static final int FILE_FLAG_SEQUENTIAL_SCAN = 0x08000000;
    public static final int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000;

    public static final int CREATE_NEW = 1;
    public static final int CREATE_ALWAYS = 2;
    public static final int OPEN_EXISTING = 3;
    public static final int OPEN_ALWAYS = 4;
    public static final int TRUNCATE_EXISTING = 5;

    private static final MethodHandle GetLastError$Func;
    private static final MethodHandle CreateFileW$Func;
    private static final MethodHandle CloseHandle$Func;
    private static final MethodHandle DeviceIoControl$Func;
    private static final MethodHandle wcslen$Func;


    static {
        var session = MemorySession.openShared();
        Linker linker = Linker.nativeLinker();
        SymbolLookup kernelLookup = SymbolLookup.libraryLookup("Kernel32", session);

        GetLastError$Func = linker.downcallHandle(
                kernelLookup.lookup("GetLastError").get(),
                FunctionDescriptor.of(JAVA_INT)
        );

        // HANDLE CreateFileW(
        //  [in]           LPCWSTR               lpFileName,
        //  [in]           DWORD                 dwDesiredAccess,
        //  [in]           DWORD                 dwShareMode,
        //  [in, optional] LPSECURITY_ATTRIBUTES lpSecurityAttributes,
        //  [in]           DWORD                 dwCreationDisposition,
        //  [in]           DWORD                 dwFlagsAndAttributes,
        //  [in, optional] HANDLE                hTemplateFile
        //);
        CreateFileW$Func = linker.downcallHandle(
                kernelLookup.lookup("CreateFileW").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS)
        );

        // BOOL CloseHandle(
        //  [in] HANDLE hObject
        //);
        CloseHandle$Func = linker.downcallHandle(
                kernelLookup.lookup("CloseHandle").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );

        // BOOL DeviceIoControl(
        //  [in]                HANDLE       hDevice,
        //  [in]                DWORD        dwIoControlCode,
        //  [in, optional]      LPVOID       lpInBuffer,
        //  [in]                DWORD        nInBufferSize,
        //  [out, optional]     LPVOID       lpOutBuffer,
        //  [in]                DWORD        nOutBufferSize,
        //  [out, optional]     LPDWORD      lpBytesReturned,
        //  [in, out, optional] LPOVERLAPPED lpOverlapped
        //);
        DeviceIoControl$Func = linker.downcallHandle(
                kernelLookup.lookup("DeviceIoControl").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
        );

        // size_t wcslen(
        //   const wchar_t *str
        //);
        wcslen$Func = linker.downcallHandle(
                linker.defaultLookup().lookup("wcslen").get(),
                FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        );
    }

    public static int GetLastError() {
        try {
            return (int) GetLastError$Func.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Checks if a Windows handle is invalid.
     *
     * @param handle Windows handle
     * @return {@code true} if the handle is invalid, {@code false} otherwise
     */
    public static boolean IsInvalidHandle(MemoryAddress handle) {
        return handle.toRawLongValue() == -1L;
    }

    // HANDLE CreateFileW(
    //  [in]           LPCWSTR               lpFileName,
    //  [in]           DWORD                 dwDesiredAccess,
    //  [in]           DWORD                 dwShareMode,
    //  [in, optional] LPSECURITY_ATTRIBUTES lpSecurityAttributes,
    //  [in]           DWORD                 dwCreationDisposition,
    //  [in]           DWORD                 dwFlagsAndAttributes,
    //  [in, optional] HANDLE                hTemplateFile
    //);
    public static MemoryAddress CreateFileW(Addressable lpFileName, int dwDesiredAccess, int dwShareMode,
                                            Addressable lpSecurityAttriubtes, int dwCreationDisposition,
                                            int dwFlagsAndAttributes, Addressable hTemplateFile) {
        try {
            return (MemoryAddress) CreateFileW$Func.invokeExact(lpFileName, dwDesiredAccess, dwShareMode,
                    lpSecurityAttriubtes, dwCreationDisposition,
                    dwFlagsAndAttributes, hTemplateFile);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static boolean CloseHandle(Addressable handle) {
        try {
            return (int) CloseHandle$Func.invokeExact(handle) != 0;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // BOOL DeviceIoControl(
    //  [in]                HANDLE       hDevice,
    //  [in]                DWORD        dwIoControlCode,
    //  [in, optional]      LPVOID       lpInBuffer,
    //  [in]                DWORD        nInBufferSize,
    //  [out, optional]     LPVOID       lpOutBuffer,
    //  [in]                DWORD        nOutBufferSize,
    //  [out, optional]     LPDWORD      lpBytesReturned,
    //  [in, out, optional] LPOVERLAPPED lpOverlapped
    //);
    public static boolean DeviceIoControl(Addressable hDevice, int dwIOControlCode,
                                          Addressable lpInBuffer, int nInBufferSize,
                                          Addressable lpPoutBuffer, int nOutBufferSize,
                                          Addressable lpBytesReturned, Addressable lpOverlapped) {
        try {
            return (int) DeviceIoControl$Func.invokeExact(hDevice, dwIOControlCode,
                    lpInBuffer, nInBufferSize,
                    lpPoutBuffer, nOutBufferSize,
                    lpBytesReturned, lpOverlapped) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    // size_t wcslen(
    //   const wchar_t *str
    //);
    public static long wcslen(Addressable str) {
        try {
            return (long) wcslen$Func.invokeExact(str);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
