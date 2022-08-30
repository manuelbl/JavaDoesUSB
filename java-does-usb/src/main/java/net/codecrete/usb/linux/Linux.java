//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;

/**
 * Helper functions for Linux
 */
public class Linux {

    /**
     * Creates a Java string as a copy of the null-terminated UTF-8 string.
     *
     * @param str the memory address pointing to string
     * @return the Java string, or {@code null} if the memory address is 0
     */
    public static String createStringFromAddress(MemoryAddress str) {
        try (var session = MemorySession.openConfined()) {
            if (str == NULL)
                return null;

            var ret = MemorySegment.ofAddress(str, 2000, session);
            return ret.getUtf8String(0);
        }
    }
}
