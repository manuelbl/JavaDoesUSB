//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.usbstandard;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.*;

/**
 * USB endpoint descriptor
 */
public class EndpointDescriptor {

    private final MemorySegment descriptor;

    public EndpointDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public EndpointDescriptor(MemorySegment segment, long offset) {
        this(segment.asSlice(offset, LAYOUT.byteSize()));
    }

    public int endpointAddress() {
        return 0xff & (byte) bEndpointAddress$VH.get(descriptor);
    }

    public int attributes() {
        return 0xff & (byte) bmAttributes$VH.get(descriptor);
    }

    public int maxPacketSize() {
        return 0xffff & (short) wMaxPacketSize$VH.get(descriptor);
    }

    public int interval() {
        return 0xff & (byte) bInterval$VH.get(descriptor);
    }

    // struct USBEndpointDescriptor {
    //     uint8_t bLength;
    //     uint8_t bDescriptorType;
    //     uint8_t bEndpointAddress;
    //     uint8_t bmAttributes;
    //     uint16_t wMaxPacketSize;
    //     uint8_t bInterval;
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_BYTE.withName("bEndpointAddress"),
            JAVA_BYTE.withName("bmAttributes"),
            JAVA_SHORT_UNALIGNED.withName("wMaxPacketSize"),
            JAVA_BYTE.withName("bInterval")
    );

    private static final VarHandle bEndpointAddress$VH = LAYOUT.varHandle(groupElement("bEndpointAddress"));
    private static final VarHandle bmAttributes$VH = LAYOUT.varHandle(groupElement("bmAttributes"));
    private static final VarHandle wMaxPacketSize$VH = LAYOUT.varHandle(groupElement("wMaxPacketSize"));
    private static final VarHandle bInterval$VH = LAYOUT.varHandle(groupElement("bInterval"));

    static {
        assert LAYOUT.byteSize() == 7;
    }
}
