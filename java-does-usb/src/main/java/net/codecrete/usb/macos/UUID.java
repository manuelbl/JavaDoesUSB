//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;

/**
 * Memory layouts and helpers for CFUUID.
 */
public class UUID {

    /**
     * Creates a CFUUID struct from a byte array.
     * @param bytes UUID as 16 bytes
     * @return the CFUUID
     */
    public static MemoryAddress CreateCFUUID(byte[] bytes) {
        try (var session = MemorySession.openConfined()) {
            var uuidBytes = session.allocate(16);
            uuidBytes.asByteBuffer().put(bytes);
            return CoreFoundation.CFUUIDCreateFromUUIDBytes(NULL, uuidBytes);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
