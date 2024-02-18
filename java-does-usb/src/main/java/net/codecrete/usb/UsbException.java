//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * USB exception, thrown if an operation with USB devices fails.
 */
public class UsbException extends RuntimeException {

    /**
     * Error code.
     */
    private final int code;

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public UsbException(@NotNull String message) {
        super(message);
        code = -1;
    }

    /**
     * Creates a new instance with a message and an error code.
     *
     * @param message   the message
     * @param errorCode the error code
     */
    public UsbException(@NotNull String message, int errorCode) {
        super(message + " (error code:" + errorCode + ")");
        code = errorCode;
    }

    /**
     * Creates a new instance with a message and a causal exception.
     *
     * @param message the message
     * @param cause   the causal exception
     */
    public UsbException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        code = -1;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public int getErrorCode() {
        return code;
    }
}
