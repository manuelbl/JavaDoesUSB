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
 * USB interface association descriptor (IAD)
 */
@SuppressWarnings("java:S125")
public class InterfaceAssociationDescriptor {

    private final MemorySegment descriptor;

    public InterfaceAssociationDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public InterfaceAssociationDescriptor(MemorySegment segment, long offset) {
        this(segment.asSlice(offset, LAYOUT.byteSize()));
    }

    public int firstInterface() {
        return 0xff & (byte) bFirstInterface$VH.get(descriptor);
    }

    public int interfaceCount() {
        return 0xff & (byte) bInterfaceCount$VH.get(descriptor);
    }

    public int functionClass() {
        return 0xff & (byte) bFunctionClass$VH.get(descriptor);
    }

    public int functionSubClass() {
        return 0xff & (byte) bFunctionSubClass$VH.get(descriptor);
    }

    public int functionProtocol() {
        return 0xff & (byte) bFunctionProtocol$VH.get(descriptor);
    }

    public int function() {
        return 0xff & (byte) iFunction$VH.get(descriptor);
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

    private static final VarHandle bFirstInterface$VH = LAYOUT.varHandle(groupElement("bFirstInterface"));
    private static final VarHandle bInterfaceCount$VH = LAYOUT.varHandle(groupElement("bInterfaceCount"));
    private static final VarHandle bFunctionClass$VH = LAYOUT.varHandle(groupElement("bFunctionClass"));
    private static final VarHandle bFunctionSubClass$VH = LAYOUT.varHandle(groupElement("bFunctionSubClass"));
    private static final VarHandle bFunctionProtocol$VH = LAYOUT.varHandle(groupElement("bFunctionProtocol"));
    private static final VarHandle iFunction$VH = LAYOUT.varHandle(groupElement("iFunction"));

    static {
        assert LAYOUT.byteSize() == 8;
    }
}
