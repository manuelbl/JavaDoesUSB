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
 * USB configuration descriptor
 */
public class ConfigurationDescriptor {

    private final MemorySegment descriptor;

    public ConfigurationDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public int descriptorType() {
        return 0xff & (byte) bDescriptorType$VH.get(descriptor);
    }
    public int totalLength() {
        return 0xffff & (short) wTotalLength$VH.get(descriptor);
    }

    public int numInterfaces() {
        return 0xff & (byte) bNumInterfaces$VH.get(descriptor);
    }

    public int configurationValue() {
        return 0xff & (byte) bConfigurationValue$VH.get(descriptor);
    }

    public int iConfiguration() {
        return 0xff & (byte) iConfiguration$VH.get(descriptor);
    }

    public int attributes() {
        return 0xff & (byte) bmAttributes$VH.get(descriptor);
    }

    public int maxPower() {
        return 0xff & (byte) bMaxPower$VH.get(descriptor);
    }


    // struct USBConfigurationDescriptor {
    //     uint8_t  bLength;
    //     uint8_t  bDescriptorType;
    //     uint16_t wTotalLength;
    //     uint8_t  bNumInterfaces;
    //     uint8_t  bConfigurationValue;
    //     uint8_t  iConfiguration;
    //     uint8_t  bmAttributes;
    //     uint8_t  bMaxPower;
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_SHORT_UNALIGNED.withName("wTotalLength"),
            JAVA_BYTE.withName("bNumInterfaces"),
            JAVA_BYTE.withName("bConfigurationValue"),
            JAVA_BYTE.withName("iConfiguration"),
            JAVA_BYTE.withName("bmAttributes"),
            JAVA_BYTE.withName("bMaxPower")
    );

    private static final VarHandle bDescriptorType$VH = LAYOUT.varHandle(groupElement("bDescriptorType"));
    private static final VarHandle wTotalLength$VH = LAYOUT.varHandle(groupElement("wTotalLength"));
    private static final VarHandle bNumInterfaces$VH = LAYOUT.varHandle(groupElement("bNumInterfaces"));
    private static final VarHandle bConfigurationValue$VH = LAYOUT.varHandle(groupElement("bConfigurationValue"));
    private static final VarHandle iConfiguration$VH = LAYOUT.varHandle(groupElement("iConfiguration"));
    private static final VarHandle bmAttributes$VH = LAYOUT.varHandle(groupElement("bmAttributes"));
    private static final VarHandle bMaxPower$VH = LAYOUT.varHandle(groupElement("bMaxPower"));

    static {
        assert LAYOUT.byteSize() == 9;
    }
}
