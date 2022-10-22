//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.macos;

import net.codecrete.usb.USBException;
import net.codecrete.usb.USBStallException;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.mach.mach;

public class MacosUSBException extends USBException {
    public MacosUSBException(String message) {
        super(message);
    }

    public MacosUSBException(String message, int errorCode) {
        super(String.format("%s - %s", message, machErrorMessage(errorCode)), errorCode);
    }

    private static String machErrorMessage(int errorCode) {
        var msg = mach.mach_error_string(errorCode);
        return msg.getUtf8String(0);
    }

    static void throwException(int errorCode, String message, Object... args) {
        var formattedMessage = String.format(message, args);
        if (errorCode == IOKit.kIOUSBPipeStalled()) {
            throw new USBStallException(formattedMessage);
        } else {
            throw new MacosUSBException(formattedMessage, errorCode);
        }
    }

    static void throwException(String message, Object... args) {
        throw new MacosUSBException(String.format(message, args));
    }

}
