//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.ADDRESS;

public class ForeignMemory {
    /**
     * Dereferences the address at the start of the memory segment and returns the result as
     * a memory segment of the specified length.
     *
     * @param segment  the memory segment with the pointer at  offset 0
     * @param byteSize the length
     * @return the dereferenced memory segment
     */
    public static MemorySegment deref(MemorySegment segment, long byteSize) {
        return segment.get(ADDRESS, 0).reinterpret(byteSize);
    }
}
