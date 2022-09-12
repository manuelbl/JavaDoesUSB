//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows;

import net.codecrete.usb.USBException;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;

public class WindowsUSBException extends USBException {
    public WindowsUSBException(String message) {
        super(message);
    }

    public WindowsUSBException(String message, int errorCode) {
        super(String.format("%s - %s", message, getErrorMessage(errorCode)), errorCode);
    }

    public WindowsUSBException(String message, Throwable cause) {
        super(message, cause);
    }

    private static String getErrorMessage(int errorCode) {
        try (var session = MemorySession.openConfined()) {
            var messagePointerHolder = session.allocate(ADDRESS);
            Kernel32.FormatMessageW(Kernel32.FORMAT_MESSAGE_ALLOCATE_BUFFER()
                            | Kernel32.FORMAT_MESSAGE_FROM_SYSTEM() | Kernel32.FORMAT_MESSAGE_IGNORE_INSERTS(),
                    NULL, errorCode, 0, messagePointerHolder, 0, NULL);
            var messagePointer = messagePointerHolder.get(ADDRESS, 0);
            String message = Win.createStringFromSegment(MemorySegment.ofAddress(messagePointer, 4000, session));
            Kernel32.LocalFree(messagePointer);
            return message.trim();
        }
    }
}
