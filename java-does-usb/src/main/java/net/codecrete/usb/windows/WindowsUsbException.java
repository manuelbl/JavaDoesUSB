//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows;

import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbStallException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static net.codecrete.usb.common.ForeignMemory.dereference;
import static windows.win32.foundation.Apis.LocalFree;
import static windows.win32.foundation.Constants.STATUS_UNSUCCESSFUL;
import static windows.win32.foundation.WIN32_ERROR.ERROR_GEN_FAILURE;
import static windows.win32.system.diagnostics.debug.Apis.FormatMessageW;
import static windows.win32.system.diagnostics.debug.FORMAT_MESSAGE_OPTIONS.FORMAT_MESSAGE_ALLOCATE_BUFFER;
import static windows.win32.system.diagnostics.debug.FORMAT_MESSAGE_OPTIONS.FORMAT_MESSAGE_FROM_HMODULE;
import static windows.win32.system.diagnostics.debug.FORMAT_MESSAGE_OPTIONS.FORMAT_MESSAGE_FROM_SYSTEM;
import static windows.win32.system.diagnostics.debug.FORMAT_MESSAGE_OPTIONS.FORMAT_MESSAGE_IGNORE_INSERTS;
import static windows.win32.system.libraryloader.Apis.GetModuleHandleW;

/**
 * Exception thrown if a Windows specific error occurs.
 */
public class WindowsUsbException extends UsbException {

    /**
     * Creates a new instance.
     * <p>
     * The message for the Windows error code is looked up and appended to the message.
     * </p>
     *
     * @param message   exception message
     * @param errorCode Windows error code (usually returned from {@code GetLastError()})
     */
    public WindowsUsbException(String message, int errorCode) {
        super(String.format("%s: %s", message, getErrorMessage(errorCode)), errorCode);
    }

    /**
     * Throws an exception for the specified Windows error code.
     * <p>
     * The message for the Windows error code is looked up and appended to the message.
     * </p>
     *
     * @param errorCode Windows error code (usually returned from {@code GetLastError()})
     * @param message   exception message format ({@link String#format(String, Object...)} style)
     * @param args      arguments for exception message
     */
    static void throwException(int errorCode, String message, Object... args) {
        var formattedMessage = String.format(message, args);
        if (errorCode == ERROR_GEN_FAILURE || errorCode == STATUS_UNSUCCESSFUL) {
            throw new UsbStallException(formattedMessage);
        } else {
            throw new WindowsUsbException(formattedMessage, errorCode);
        }
    }

    /**
     * Throws a USB exception.
     *
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args    arguments for exception message
     */
    static void throwException(String message, Object... args) {
        throw new UsbException(String.format(message, args));
    }

    /**
     * Throws an exception for the last error.
     * <p>
     * The last Windows error code is taken from the call capture state
     * {@link Win#LAST_ERROR_STATE} provided as the first parameter.
     * </p>
     *
     * @param errorState call capture state containing last error code
     * @param message    exception message format ({@link String#format(String, Object...)} style)
     * @param args       arguments for exception message
     */
    static void throwLastError(MemorySegment errorState, String message, Object... args) {
        throwException(Win.getLastError(errorState), message, args);
    }

    private static MemorySegment ntModule; // NOSONAR

    private static MemorySegment getNtModule() {
        if (ntModule == null) {
            try (var arena = Arena.ofConfined()) {
                var errorState = Win.allocateErrorState(arena);
                var moduleName = arena.allocateFrom("NTDLL.DLL", UTF_16LE);
                ntModule = GetModuleHandleW(errorState, moduleName);
            }
        }

        return ntModule;
    }

    static String getErrorMessage(int errorCode) {
        try (var arena = Arena.ofConfined()) {
            var errorState = Win.allocateErrorState(arena);
            var messagePointerHolder = arena.allocate(ADDRESS);

            // First try: Win32 error code
            var res = FormatMessageW(
                    errorState, FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                    NULL, errorCode, 0, messagePointerHolder, 0, NULL);

            // Second try: NTSTATUS error code
            if (res == 0) {
                res = FormatMessageW(
                        errorState, FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_HMODULE | FORMAT_MESSAGE_IGNORE_INSERTS,
                        getNtModule(), errorCode, 0, messagePointerHolder, 0, NULL);
            }

            // Fallback
            if (res == 0)
                return "unspecified error";

            var messagePointer = dereference(messagePointerHolder).reinterpret(128 * 1024); // NOSONAR
            var message = messagePointer.getString(0, UTF_16LE);
            LocalFree(errorState, messagePointer);
            return message.trim();
        }
    }
}
