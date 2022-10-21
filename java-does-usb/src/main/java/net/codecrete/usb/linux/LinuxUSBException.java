//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.linux;

import net.codecrete.usb.USBException;

public class LinuxUSBException extends USBException {
    public LinuxUSBException(String message) {
        super(message);
    }

    public LinuxUSBException(String message, int errorCode) {
        super(String.format("%s - %s", message, Linux.getErrorMessage(errorCode)), errorCode);
    }

    public LinuxUSBException(String message, Throwable cause) {
        super(message, cause);
    }
}
