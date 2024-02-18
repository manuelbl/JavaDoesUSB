//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.usbstandard;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * USB endpoint descriptor
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class EndpointDescriptor {

    private final MemorySegment descriptor;

    public EndpointDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public EndpointDescriptor(MemorySegment segment, long offset) {
        this(segment.asSlice(offset, LAYOUT.byteSize()));
    }

    public int endpointAddress() {
        return 0xff & descriptor.get(JAVA_BYTE, bEndpointAddress$OFFSET);
    }

    public int attributes() {
        return 0xff & descriptor.get(JAVA_BYTE, bmAttributes$OFFSET);
    }

    public int maxPacketSize() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, wMaxPacketSize$OFFSET);
    }

    public int interval() {
        return 0xff & descriptor.get(JAVA_BYTE, bInterval$OFFSET);
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

    private static final long bEndpointAddress$OFFSET = 2;
    private static final long bmAttributes$OFFSET = 3;
    private static final long wMaxPacketSize$OFFSET = 4;
    private static final long bInterval$OFFSET = 6;

    static {
        assert LAYOUT.byteSize() == 7;
    }
}
