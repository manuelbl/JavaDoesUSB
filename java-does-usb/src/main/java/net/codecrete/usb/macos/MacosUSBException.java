//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.macos;

import net.codecrete.usb.USBException;
import net.codecrete.usb.macos.gen.mach.mach;

public class MacosUSBException extends USBException {
    public MacosUSBException(String message) {
        super(message);
    }

    public MacosUSBException(String message, int errorCode) {
        super(String.format("%s - %s", message, machErrorMessage(errorCode)), errorCode);
    }

    public MacosUSBException(String message, Throwable cause) {
        super(message, cause);
    }

    private static String machErrorMessage(int errorCode) {
        var msg = mach.mach_error_string(errorCode);
        return msg.getUtf8String(0);
    }
}
