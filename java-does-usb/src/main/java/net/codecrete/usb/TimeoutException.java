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
public class TimeoutException extends USBException {

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public TimeoutException(String message) {
        super(message);
    }
}
