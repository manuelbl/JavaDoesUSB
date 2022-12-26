//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.ValueLayout;

public class ForeignMemory {
    /**
     * A value layout constant whose size is the same as that of a machine address.
     * <p>
     * {@code MemorySegment} instances created by dereferencing an address of this type
     * will have length {@code Long.MAX_VALUE}/
     * </p>
     */
    public static ValueLayout.OfAddress UNBOUNDED_ADDRESS = ValueLayout.ADDRESS.asUnbounded();
}
