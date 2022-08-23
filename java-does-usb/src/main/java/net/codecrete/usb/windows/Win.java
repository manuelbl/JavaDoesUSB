//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.MemoryAddress;

/**
 * Windows helpers.
 */
public class Win {

    /**
     * Checks if a Windows handle is invalid.
     *
     * @param handle Windows handle
     * @return {@code true} if the handle is invalid, {@code false} otherwise
     */
    public static boolean IsInvalidHandle(MemoryAddress handle) {
        return handle.toRawLongValue() == -1L;
    }
}
