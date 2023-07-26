//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * Helper functions for accessing native memory.
 */
public class ForeignMemory {
    /**
     * Dereferences the address at the start of given memory segment and returns the result as
     * a memory segment with the length suitable for the specified layout.
     *
     * @param segment  the memory segment with the pointer at offset 0
     * @param layout layout for determining size of memory segment
     * @return the dereferenced memory segment
     */
    public static MemorySegment dereference(MemorySegment segment, MemoryLayout layout) {
        return segment.get(ADDRESS, 0).reinterpret(layout.byteSize());
    }

    /**
     * Dereferences the address at the start of given memory segment and returns the result as
     * a memory segment of size 0.
     *
     * @param segment  the memory segment with the pointer at offset 0
     * @return the dereferenced memory segment
     */
    public static MemorySegment dereference(MemorySegment segment) {
        return segment.get(ADDRESS, 0);
    }
}
