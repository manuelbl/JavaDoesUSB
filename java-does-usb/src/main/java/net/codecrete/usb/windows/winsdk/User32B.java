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

import static java.lang.foreign.ValueLayout.*;

/**
 * Native function calls for User32.
 * <p>
 * This code is manually created to include the additional parameters for capturing
 * {@code GetLastError()} until jextract catches up and can generate the corresponding code.
 * </p>
 */
public class User32B {

    static {
        System.loadLibrary("User32");
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    private static final FunctionDescriptor RegisterClassExW$FUNC = FunctionDescriptor.of(JAVA_SHORT, ADDRESS);

    private static final MethodHandle RegisterClassExW$MH = LINKER.downcallHandle(
            LOOKUP.find("RegisterClassExW").get(),
            RegisterClassExW$FUNC,
            Win.LAST_ERROR_STATE
    );

    private static final FunctionDescriptor CreateWindowExW$FUNC =
            FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS);

    private static final MethodHandle CreateWindowExW$MH = LINKER.downcallHandle(
            LOOKUP.find("CreateWindowExW").get(),
            CreateWindowExW$FUNC,
            Win.LAST_ERROR_STATE
    );

    private static final FunctionDescriptor RegisterDeviceNotificationW$FUNC =
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT);

    private static final MethodHandle RegisterDeviceNotificationW$MH = LINKER.downcallHandle(
            LOOKUP.find("RegisterDeviceNotificationW").get(),
            RegisterDeviceNotificationW$FUNC,
            Win.LAST_ERROR_STATE
    );

    private static final FunctionDescriptor GetMessageW$FUNC =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);

    private static final MethodHandle GetMessageW$MH = LINKER.downcallHandle(
            LOOKUP.find("GetMessageW").get(),
            GetMessageW$FUNC,
            Win.LAST_ERROR_STATE
    );

    /**
     * {@snippet :
     * ATOM RegisterClassExW(const WNDCLASSEXW* unnamedParam1);
     * }
     */
    public static short RegisterClassExW(MemorySegment unnamedParam1, MemorySegment lastErrorState) {
        try {
            return (short)RegisterClassExW$MH.invokeExact(lastErrorState, unnamedParam1);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@snippet :
     * HWND CreateWindowExW(DWORD dwExStyle, LPCWSTR lpClassName, LPCWSTR lpWindowName, DWORD dwStyle, int X, int Y, int nWidth, int nHeight, HWND hWndParent, HMENU hMenu, HINSTANCE hInstance, LPVOID lpParam);
     * }
     */
    public static MemorySegment CreateWindowExW(int dwExStyle, MemorySegment lpClassName, MemorySegment lpWindowName,
                                                int dwStyle, int X, int Y, int nWidth, int nHeight,
                                                MemorySegment hWndParent, MemorySegment hMenu, MemorySegment hInstance,
                                                MemorySegment lpParam, MemorySegment lastErrorState) {
        try {
            return (MemorySegment)CreateWindowExW$MH.invokeExact(lastErrorState, dwExStyle, lpClassName, lpWindowName,
                    dwStyle, X, Y, nWidth, nHeight, hWndParent, hMenu, hInstance, lpParam);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@snippet :
     * HDEVNOTIFY RegisterDeviceNotificationW(HANDLE hRecipient, LPVOID NotificationFilter, DWORD Flags);
     * }
     */
    public static MemorySegment RegisterDeviceNotificationW(MemorySegment hRecipient, MemorySegment NotificationFilter,
                                                            int Flags, MemorySegment lastErrorState) {
        try {
            return (MemorySegment)RegisterDeviceNotificationW$MH.invokeExact(lastErrorState, hRecipient, NotificationFilter, Flags);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@snippet :
     * BOOL GetMessageW(LPMSG lpMsg, HWND hWnd, UINT wMsgFilterMin, UINT wMsgFilterMax);
     * }
     */
    public static int GetMessageW(MemorySegment lpMsg, MemorySegment hWnd, int wMsgFilterMin, int wMsgFilterMax,
                                  MemorySegment lastErrorState) {
        try {
            return (int)GetMessageW$MH.invokeExact(lastErrorState, lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
