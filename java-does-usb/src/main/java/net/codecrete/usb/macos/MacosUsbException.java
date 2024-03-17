//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.macos;

import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbStallException;
import net.codecrete.usb.UsbTimeoutException;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.mach.mach;

/**
 * Exception thrown if a macOS specific error occurs.
 */
public class MacosUsbException extends UsbException {

    /**
     * Creates a new instance.
     * <p>
     * The message for the macOS error code is looked up and appended to the message.
     * </p>
     *
     * @param message   exception message
     * @param errorCode macOS error code (usually returned by macOS functions)
     */
    public MacosUsbException(String message, int errorCode) {
        super(String.format("%s: %s", message, machErrorMessage(errorCode)), errorCode);
    }

    private static String machErrorMessage(int errorCode) {
        var msg = mach.mach_error_string(errorCode);
        return msg.getString(0);
    }

    /**
     * Throws an exception for the specified macOS error code.
     * <p>
     * The message for the macOS error code is looked up and appended to the message.
     * </p>
     *
     * @param errorCode macOS error code (usually returned by macOS functions)
     * @param message   exception message format ({@link String#format(String, Object...)} style)
     * @param args      arguments for exception message
     */
    static void throwException(int errorCode, String message, Object... args) {
        var formattedMessage = String.format(message, args);
        if (errorCode == IOKit.kIOUSBPipeStalled()) {
            throw new UsbStallException(formattedMessage);
        } else if (errorCode == IOKit.kIOUSBTransactionTimeout()) {
            throw new UsbTimeoutException(formattedMessage);
        } else {
            throw new MacosUsbException(formattedMessage, errorCode);
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

}
