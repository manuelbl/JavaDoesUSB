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
 * USB interface association descriptor (IAD)
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class InterfaceAssociationDescriptor {

    private final MemorySegment descriptor;

    public InterfaceAssociationDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public InterfaceAssociationDescriptor(MemorySegment segment, long offset) {
        this(segment.asSlice(offset, LAYOUT.byteSize()));
    }

    public int firstInterface() {
        return 0xff & descriptor.get(JAVA_BYTE, bFirstInterface$OFFSET);
    }

    public int interfaceCount() {
        return 0xff & descriptor.get(JAVA_BYTE, bInterfaceCount$OFFSET);
    }

    public int functionClass() {
        return 0xff & descriptor.get(JAVA_BYTE, bFunctionClass$OFFSET);
    }

    public int functionSubClass() {
        return 0xff & descriptor.get(JAVA_BYTE, bFunctionSubClass$OFFSET);
    }

    public int functionProtocol() {
        return 0xff & descriptor.get(JAVA_BYTE, bFunctionProtocol$OFFSET);
    }

    public int function() {
        return 0xff & descriptor.get(JAVA_BYTE, iFunction$OFFSET);
    }

    // struct USBInterfaceAssociationDescriptor {
    //     uint8_t  bLength,
    //     uint8_t  bDescriptorType,
    //     uint8_t  bFirstInterface,
    //     uint8_t  bInterfaceCount,
    //     uint8_t  bFunctionClass,
    //     uint8_t  bFunctionSubClass,
    //     uint8_t  bFunctionProtocol,
    //     uint8_t  iFunction
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_BYTE.withName("bFirstInterface"),
            JAVA_BYTE.withName("bInterfaceCount"),
            JAVA_BYTE.withName("bFunctionClass"),
            JAVA_BYTE.withName("bFunctionSubClass"),
            JAVA_BYTE.withName("bFunctionProtocol"),
            JAVA_BYTE.withName("iFunction")
    );

    private static final long bFirstInterface$OFFSET = 2;
    private static final long bInterfaceCount$OFFSET = 3;
    private static final long bFunctionClass$OFFSET = 4;
    private static final long bFunctionSubClass$OFFSET = 5;
    private static final long bFunctionProtocol$OFFSET = 6;
    private static final long iFunction$OFFSET = 7;

    static {
        assert LAYOUT.byteSize() == 8;
    }
}
