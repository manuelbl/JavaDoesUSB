//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

/**
 * DFU exception thrown if a DFU operation fails.
 */
public class DFUException extends RuntimeException {

    /**
     * Creates a new instance with the given message
     * @param message the message
     */
    DFUException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the given message and cause
     * @param message the message
     * @param cause the exception causing the DFU operation to fail
     */
    DFUException(String message, Throwable cause) {
        super(message, cause);
    }
}
