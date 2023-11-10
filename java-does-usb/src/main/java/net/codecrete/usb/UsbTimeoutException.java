//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown if a USB operation times out.
 */
public class UsbTimeoutException extends UsbException {

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public UsbTimeoutException(@NotNull String message) {
        super(message);
    }
}
