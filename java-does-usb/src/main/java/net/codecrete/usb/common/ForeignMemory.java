//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class ForeignMemory {
    /**
     * A value layout constant whose size is the same as that of a machine address.
     * <p>
     * {@code MemorySegment} instances created by dereferencing an address of this type
     * will have length {@code Long.MAX_VALUE}/
     * </p>
     */
    public static final ValueLayout.OfAddress UNBOUNDED_ADDRESS = ValueLayout.ADDRESS.asUnbounded();

    /**
     * Dereferences the address at the start of the memory segment and returns the result as
     * a memory segment of the specified length.
     *
     * @param segment  the memory segment with the pointer at  offset 0
     * @param byteSize the length
     * @return the dereferenced memory segment
     */
    public static MemorySegment deref(MemorySegment segment, long byteSize) {
        return segment.get(UNBOUNDED_ADDRESS, 0).asSlice(0, byteSize);
    }
}
