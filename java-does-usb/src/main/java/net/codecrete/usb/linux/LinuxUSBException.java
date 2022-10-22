//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.linux;

import net.codecrete.usb.USBException;
import net.codecrete.usb.USBStallException;
import net.codecrete.usb.linux.gen.errno.errno;

/**
 * Exception thrown if a Linux specific error occurs.
 */
public class LinuxUSBException extends USBException {

    /**
     * Creates a new instance.
     * <p>
     * The message for the Linux error code is looked up and appended to the message.
     * </p>
     * @param message exception message
     * @param errorCode Linux error code (returned by {@code errno})
     */
    public LinuxUSBException(String message, int errorCode) {
        super(String.format("%s. %s", message, Linux.getErrorMessage(errorCode)), errorCode);
    }

    /**
     * Throws an exception for the specified Linux error code.
     * <p>
     * The message for the Linux error code is looked up and appended to the message.
     * </p>
     * @param errorCode Linux error code (returned by {@code errno})
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args arguments for exception message
     */
    static void throwException(int errorCode, String message, Object... args) {
        var formattedMessage = String.format(message, args);
        if (errorCode == errno.EPIPE()) {
            throw new USBStallException(formattedMessage);
        } else {
            throw new LinuxUSBException(formattedMessage, errorCode);
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
     * The message of the last Linux error code is looked ({@code errno}) up and appended to the message.
     * </p>
     * @param message exception message format ({@link String#format(String, Object...)} style)
     * @param args arguments for exception message
     */
    static void throwLastError(String message, Object... args) {
        throwException(IO.getErrno(), message, args);
    }

}
