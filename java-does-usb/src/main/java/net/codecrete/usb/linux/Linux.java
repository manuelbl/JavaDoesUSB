//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.string.string;

/**
 * Helper functions for Linux
 */
public class Linux {

    /**
     * Gets the error message for the specified error code (returned by {@code errno}).
     *
     * @param err error code
     * @return error message
     */
    public static String getErrorMessage(int err) {
        return string.strerror(err).getUtf8String(0);
    }
}
