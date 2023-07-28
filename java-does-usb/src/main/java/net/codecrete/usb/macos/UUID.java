//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;

/**
 * Memory layouts and helpers for CFUUID.
 */
class UUID {

    private UUID() {
    }

    /**
     * Creates a CFUUID struct from a byte array.
     *
     * @param bytes UUID as 16 bytes
     * @return the CFUUID
     */
    static MemorySegment createCFUUID(byte[] bytes) {
        try (var arena = Arena.ofConfined()) {
            var uuidBytes = arena.allocate(16);
            uuidBytes.copyFrom(MemorySegment.ofArray(bytes));
            return CoreFoundation.CFUUIDCreateFromUUIDBytes(NULL, uuidBytes);
        } catch (Exception e) {
            throw new AssertionError("internal error (createCFUUID)", e);
        }
    }
}
