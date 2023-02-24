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
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * USB interface descriptor
 */
public class InterfaceDescriptor {

    private final MemorySegment descriptor;

    public InterfaceDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public InterfaceDescriptor(MemorySegment segment, long offset) {
        this(segment.asSlice(offset, LAYOUT.byteSize()));
    }

    public int interfaceNumber() {
        return 0xff & (byte) bInterfaceNumber$VH.get(descriptor);
    }

    public int alternateSetting() {
        return 0xff & (byte) bAlternateSetting$VH.get(descriptor);
    }

    public int numEndpoints() {
        return 0xff & (byte) bNumEndpoints$VH.get(descriptor);
    }

    public int interfaceClass() {
        return 0xff & (byte) bInterfaceClass$VH.get(descriptor);
    }

    public int interfaceSubClass() {
        return 0xff & (byte) bInterfaceSubClass$VH.get(descriptor);
    }

    public int interfaceProtocol() {
        return 0xff & (byte) bInterfaceProtocol$VH.get(descriptor);
    }

    public int iInterface() {
        return 0xff & (byte) iInterface$VH.get(descriptor);
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

    private static final VarHandle bInterfaceNumber$VH = LAYOUT.varHandle(groupElement("bInterfaceNumber"));
    private static final VarHandle bAlternateSetting$VH = LAYOUT.varHandle(groupElement("bAlternateSetting"));
    private static final VarHandle bNumEndpoints$VH = LAYOUT.varHandle(groupElement("bNumEndpoints"));
    private static final VarHandle bInterfaceClass$VH = LAYOUT.varHandle(groupElement("bInterfaceClass"));
    private static final VarHandle bInterfaceSubClass$VH = LAYOUT.varHandle(groupElement("bInterfaceSubClass"));
    private static final VarHandle bInterfaceProtocol$VH = LAYOUT.varHandle(groupElement("bInterfaceProtocol"));
    private static final VarHandle iInterface$VH = LAYOUT.varHandle(groupElement("iInterface"));


    static {
        assert LAYOUT.byteSize() == 9;
    }
}
