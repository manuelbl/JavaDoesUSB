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
 * USB configuration descriptor
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class ConfigurationDescriptor {

    private final MemorySegment descriptor;

    public ConfigurationDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public int descriptorType() {
        return 0xff & descriptor.get(JAVA_BYTE, bDescriptorType$OFFSET);
    }

    public int totalLength() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, wTotalLength$OFFSET);
    }

    public int numInterfaces() {
        return 0xff & descriptor.get(JAVA_BYTE, bNumInterfaces$OFFSET);
    }

    public int configurationValue() {
        return 0xff & descriptor.get(JAVA_BYTE, bConfigurationValue$OFFSET);
    }

    public int iConfiguration() {
        return 0xff & descriptor.get(JAVA_BYTE, iConfiguration$OFFSET);
    }

    public int attributes() {
        return 0xff & descriptor.get(JAVA_BYTE, bmAttributes$OFFSET);
    }

    public int maxPower() {
        return 0xff & descriptor.get(JAVA_BYTE, bMaxPower$OFFSET);
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

    private static final long bDescriptorType$OFFSET = 1;
    private static final long wTotalLength$OFFSET = 2;
    private static final long bNumInterfaces$OFFSET = 4;
    private static final long bConfigurationValue$OFFSET = 5;
    private static final long iConfiguration$OFFSET = 6;
    private static final long bmAttributes$OFFSET = 7;
    private static final long bMaxPower$OFFSET = 8;

    static {
        assert LAYOUT.byteSize() == 9;
    }
}
