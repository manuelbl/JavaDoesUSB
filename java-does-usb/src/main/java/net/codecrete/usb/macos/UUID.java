//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.corefoundation.CFUUIDBytes;
import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.*;

/**
 * Memory layouts and helpers for CFUUID.
 */
public class UUID {

    public static final GroupLayout CFUUID = structLayout(
            ADDRESS.withName("_cfisa"), // uintptr_t _cfisa;
            sequenceLayout(4, JAVA_BYTE).withName("_cfinfo"), // uint8_t _cfinfo[4];
            JAVA_INT.withName("_rc"), // uint32_t _rc;
            CFUUIDBytes.$LAYOUT().withName("_bytes") // CFUUIDBytes _bytes;
    );
    public static final long CFUUID_bytes$Offset = CFUUID.byteOffset(MemoryLayout.PathElement.groupElement("_bytes"));

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
