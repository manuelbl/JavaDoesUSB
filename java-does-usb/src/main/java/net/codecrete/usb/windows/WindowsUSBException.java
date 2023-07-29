//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows;

import net.codecrete.usb.USBException;
import net.codecrete.usb.USBStallException;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.ntdll.NtDll;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static net.codecrete.usb.common.ForeignMemory.dereference;

/**
 * Exception thrown if a Windows specific error occurs.
 */
public class WindowsUSBException extends USBException {

    /**
     * Creates a new instance.
     * <p>
     * The message for the Windows error code is looked up and appended to the message.
     * </p>
     *
     * @param message   exception message
     * @param errorCode Windows error code (usually returned from {@code GetLastError()})
     */
    public WindowsUSBException(String message, int errorCode) {
        super(String.format("%s. %s", message, getErrorMessage(errorCode)), errorCode);
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
        if (errorCode == Kernel32.ERROR_GEN_FAILURE() || errorCode == NtDll.STATUS_UNSUCCESSFUL()) {
            throw new USBStallException(formattedMessage);
        } else {
            throw new WindowsUSBException(formattedMessage, errorCode);
        }
    }

    /**
     * Throws a USB exception.
     *
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args    arguments for exception message
     */
    static void throwException(String message, Object... args) {
        throw new USBException(String.format(message, args));
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
                var moduleName = Win.createSegmentFromString("NTDLL.DLL", arena);
                ntModule = Kernel32.GetModuleHandleW(moduleName);
            }
        }

        return ntModule;
    }

    static String getErrorMessage(int errorCode) {
        try (var arena = Arena.ofConfined()) {
            var messagePointerHolder = arena.allocate(ADDRESS);

            // First try: Win32 error code
            var res = Kernel32.FormatMessageW(
                    Kernel32.FORMAT_MESSAGE_ALLOCATE_BUFFER() | Kernel32.FORMAT_MESSAGE_FROM_SYSTEM() | Kernel32.FORMAT_MESSAGE_IGNORE_INSERTS(),
                    NULL, errorCode, 0, messagePointerHolder, 0, NULL);

            // Second try: NTSTATUS error code
            if (res == 0) {
                res = Kernel32.FormatMessageW(
                        Kernel32.FORMAT_MESSAGE_ALLOCATE_BUFFER() | Kernel32.FORMAT_MESSAGE_FROM_HMODULE() | Kernel32.FORMAT_MESSAGE_IGNORE_INSERTS(),
                        getNtModule(), errorCode, 0, messagePointerHolder, 0, NULL);
            }

            // Fallback
            if (res == 0)
                return "unspecified error";

            var messagePointer = dereference(messagePointerHolder).reinterpret(128 * 1024); // NOSONAR
            var message = Win.createStringFromSegment(messagePointer);
            Kernel32.LocalFree(messagePointer);
            return message.trim();
        }
    }
}
