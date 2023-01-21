//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows;

import net.codecrete.usb.USBException;
import net.codecrete.usb.USBStallException;
import net.codecrete.usb.common.ForeignMemory;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * Exception thrown if a Windows specific error occurs.
 */
public class WindowsUSBException extends USBException {

    /**
     * Creates a new instance.
     * <p>
     * The message for the Windows error code is looked up and appended to the message.
     * </p>
     * @param message exception message
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
     * @param errorCode Windows error code (usually returned from {@code GetLastError()})
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args arguments for exception message
     */
    static void throwException(int errorCode, String message, Object... args) {
        var formattedMessage = String.format(message, args);
        if (errorCode == Kernel32.ERROR_GEN_FAILURE()) {
            throw new USBStallException(formattedMessage);
        } else {
            throw new WindowsUSBException(formattedMessage, errorCode);
        }
    }

    /**
     * Throws a USB exception.
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args arguments for exception message
     */
    static void throwException(String message, Object... args) {
        throw new USBException(String.format(message, args));
    }

    /**
     * Throws an exception for the last error.
     * <p>
     * The last Windows error code is taken from the call capture state
     * {@link Win.LAST_ERROR_STATE} provided as the first parameter.
     * </p>
     *
     * @param lastErrorState call capture state containing last error code
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args arguments for exception message
     */
    static void throwLastError(MemorySegment lastErrorState, String message, Object... args) {
        throwException(Win.getLastError(lastErrorState), message, args);
    }

    private static String getErrorMessage(int errorCode) {
        try (var arena = Arena.openConfined()) {
            var messagePointerHolder = arena.allocate(ADDRESS);
            int res = Kernel32.FormatMessageW(Kernel32.FORMAT_MESSAGE_ALLOCATE_BUFFER()
                            | Kernel32.FORMAT_MESSAGE_FROM_SYSTEM() | Kernel32.FORMAT_MESSAGE_IGNORE_INSERTS(),
                    NULL, errorCode, 0, messagePointerHolder, 0, NULL);
            if (res == 0)
                return "unspecified error";
            var messagePointer = messagePointerHolder.get(ForeignMemory.UNBOUNDED_ADDRESS, 0);
            String message = Win.createStringFromSegment(messagePointer);
            Kernel32.LocalFree(messagePointer);
            return message.trim();
        }
    }
}
