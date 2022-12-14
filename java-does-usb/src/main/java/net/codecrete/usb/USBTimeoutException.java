//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * Exception thrown if a USB operation times out.
 */
public class USBTimeoutException extends USBException {

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public USBTimeoutException(String message) {
        super(message);
    }
}
