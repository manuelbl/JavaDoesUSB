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

/**
 * USB interface descriptor
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class InterfaceDescriptor {

    private final MemorySegment descriptor;

    public InterfaceDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public InterfaceDescriptor(MemorySegment segment, long offset) {
        this(segment.asSlice(offset, LAYOUT.byteSize()));
    }

    public int interfaceNumber() {
        return 0xff & descriptor.get(JAVA_BYTE, bInterfaceNumber$OFFSET);
    }

    public int alternateSetting() {
        return 0xff & descriptor.get(JAVA_BYTE, bAlternateSetting$OFFSET);
    }

    public int numEndpoints() {
        return 0xff & descriptor.get(JAVA_BYTE, bNumEndpoints$OFFSET);
    }

    public int interfaceClass() {
        return 0xff & descriptor.get(JAVA_BYTE, bInterfaceClass$OFFSET);
    }

    public int interfaceSubClass() {
        return 0xff & descriptor.get(JAVA_BYTE, bInterfaceSubClass$OFFSET);
    }

    public int interfaceProtocol() {
        return 0xff & descriptor.get(JAVA_BYTE, bInterfaceProtocol$OFFSET);
    }

    public int iInterface() {
        return 0xff & descriptor.get(JAVA_BYTE, iInterface$OFFSET);
    }

    // struct USBInterfaceDescriptor {
    //     uint8_t bLength;
    //     uint8_t bDescriptorType;
    //     uint8_t bInterfaceNumber;
    //     uint8_t bAlternateSetting;
    //     uint8_t bNumEndpoints;
    //     uint8_t bInterfaceClass;
    //     uint8_t bInterfaceSubClass;
    //     uint8_t bInterfaceProtocol;
    //     uint8_t iInterface;
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_BYTE.withName("bInterfaceNumber"),
            JAVA_BYTE.withName("bAlternateSetting"),
            JAVA_BYTE.withName("bNumEndpoints"),
            JAVA_BYTE.withName("bInterfaceClass"),
            JAVA_BYTE.withName("bInterfaceSubClass"),
            JAVA_BYTE.withName("bInterfaceProtocol"),
            JAVA_BYTE.withName("iInterface")
    );

    private static final long bInterfaceNumber$OFFSET = 2;
    private static final long bAlternateSetting$OFFSET = 3;
    private static final long bNumEndpoints$OFFSET = 4;
    private static final long bInterfaceClass$OFFSET = 5;
    private static final long bInterfaceSubClass$OFFSET = 6;
    private static final long bInterfaceProtocol$OFFSET = 7;
    private static final long iInterface$OFFSET = 8;


    static {
        assert LAYOUT.byteSize() == 9;
    }
}
