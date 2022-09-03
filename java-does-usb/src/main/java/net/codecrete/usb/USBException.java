//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * This exception is thrown if an operation with USB devices fails.
 */
public class USBException extends RuntimeException {

    private int errorCode_ = -1;

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public USBException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with a message and an error code.
     *
     * @param message   the message
     * @param errorCode the error code
     */
    public USBException(String message, int errorCode) {
        super(message + " (error code: " + errorCode + ")");
        errorCode_ = errorCode;
    }

    /**
     * Creates a new instance with a message and a causal exception.
     *
     * @param message the message
     * @param cause   the causal exception
     */
    public USBException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public int errorCode() {
        return errorCode_;
    }
}
