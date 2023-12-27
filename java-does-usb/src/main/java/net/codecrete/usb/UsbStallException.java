//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown if a communication on a USB endpoint failed.
 * <p>
 * If a USB endpoint stalls, it is halted and the halt condition must be cleared
 * using {@link UsbDevice#clearHalt(UsbDirection, int)} before communication can resume.
 * </p>
 * <p>
 * If the control endpoint 0 stalls, it throws this exception but is not halted.
 * </p>
 */
public class UsbStallException extends UsbException {

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public UsbStallException(@NotNull String message) {
        super(message);
    }
}
