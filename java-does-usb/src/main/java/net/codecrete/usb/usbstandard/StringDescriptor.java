//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.usbstandard;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * USB string descriptor
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class StringDescriptor {

    private final MemorySegment descriptor;

    public StringDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public int length() {
        return 0xff & descriptor.get(JAVA_BYTE, bLength$OFFSET);
    }

    public String string() {
        var chars = descriptor.asSlice(string$OFFSET, length() - 2L).toArray(JAVA_CHAR);
        return new String(chars);
    }

    // struct USBStringDescriptor {
    //     uint8_t   bLength;
    //     uint8_t   bDescriptorType;
    //     uint16_t  string[1];
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_SHORT.withName("string")
    );

    private static final long bLength$OFFSET = 0;
    private static final long string$OFFSET = 2;
}
