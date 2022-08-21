//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.ValueLayout.ADDRESS;

public class Foreign {

    /**
     * Dereference the pointer and return the address the pointer is pointing at
     *
     * @param pointer pointer to dereference
     * @param session memory session
     * @return dereferenced value (of type address)
     */
    public static MemoryAddress derefAddress(MemoryAddress pointer, MemorySession session) {
        var seg = MemorySegment.ofAddress(pointer, ADDRESS.byteSize(), session);
        return seg.get(ADDRESS, 0);
    }
}
