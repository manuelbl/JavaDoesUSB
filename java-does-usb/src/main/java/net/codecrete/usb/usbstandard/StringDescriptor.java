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
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.*;

/**
 * USB string descriptor
 */
public class StringDescriptor {

    private final MemorySegment descriptor;

    public StringDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public int length() {
        return 0xff & (byte) bLength$VH.get(descriptor);
    }

    public String string() {
        var chars = descriptor.asSlice(string$offset, length() - 2).toArray(JAVA_CHAR);
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

    private static final VarHandle bLength$VH = LAYOUT.varHandle(groupElement("bLength"));
    private static final long string$offset = LAYOUT.byteOffset(groupElement("string"));
}
