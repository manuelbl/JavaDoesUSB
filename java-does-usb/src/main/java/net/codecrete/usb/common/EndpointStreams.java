//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbTimeoutException;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Helpers shared by {@link EndpointInputStream} and {@link EndpointOutputStream}.
 */
final class EndpointStreams {

    private EndpointStreams() {
    }

    /**
     * Wraps a USB error in an {@link IOException} so it is surfaced through the
     * {@link java.io.InputStream}/{@link java.io.OutputStream} contract.
     * <p>
     * A transfer timeout is mapped to {@link InterruptedIOException} (java.io's "I/O timed out").
     * All other USB errors become a plain {@link IOException} with the {@link UsbException} as cause,
     * which preserves the USB error code and any stall/timeout subtype for callers that inspect it.
     * </p>
     *
     * @param e the USB error
     * @return the corresponding I/O exception
     */
    static IOException toIOException(UsbException e) {
        if (e instanceof UsbTimeoutException) {
            var ioException = new InterruptedIOException(e.getMessage());
            ioException.initCause(e);
            return ioException;
        }
        return new IOException(e.getMessage(), e);
    }
}
