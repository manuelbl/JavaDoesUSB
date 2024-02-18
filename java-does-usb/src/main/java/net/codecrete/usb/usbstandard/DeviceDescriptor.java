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
 * USB device descriptor
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class DeviceDescriptor {

    private final MemorySegment descriptor;

    public DeviceDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public int usbVersion() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, bcdUSB$OFFSET);
    }

    public int deviceClass() {
        return 0xff & descriptor.get(JAVA_BYTE, bDeviceClass$OFFSET);
    }

    public int deviceSubClass() {
        return 0xff & descriptor.get(JAVA_BYTE, bDeviceSubClass$OFFSET);
    }

    public int deviceProtocol() {
        return 0xff & descriptor.get(JAVA_BYTE, bDeviceProtocol$OFFSET);
    }

    public int vendorID() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, idVendor$OFFSET);
    }

    public int productID() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, idProduct$OFFSET);
    }

    public int deviceVersion() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, bcdDevice$OFFSET);
    }

    public int iManufacturer() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, iManufacturer$OFFSET);
    }

    public int iProduct() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, iProduct$OFFSET);
    }

    public int iSerialNumber() {
        return 0xffff & descriptor.get(JAVA_SHORT_UNALIGNED, iSerialNumber$OFFSET);
    }

    // struct USBDeviceDescriptor {
    //     uint8_t   bLength;
    //     uint8_t   bDescriptorType;
    //     uint16_t  bcdUSB;
    //     uint8_t   bDeviceClass;
    //     uint8_t   bDeviceSubClass;
    //     uint8_t   bDeviceProtocol;
    //     uint8_t   bMaxPacketSize0;
    //     uint16_t  idVendor;
    //     uint16_t  idProduct;
    //     uint16_t  bcdDevice;
    //     uint8_t   iManufacturer;
    //     uint8_t   iProduct;
    //     uint8_t   iSerialNumber;
    //     uint8_t   bNumConfigurations;
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_SHORT_UNALIGNED.withName("bcdUSB"),
            JAVA_BYTE.withName("bDeviceClass"),
            JAVA_BYTE.withName("bDeviceSubClass"),
            JAVA_BYTE.withName("bDeviceProtocol"),
            JAVA_BYTE.withName("bMaxPacketSize0"),
            JAVA_SHORT_UNALIGNED.withName("idVendor"),
            JAVA_SHORT_UNALIGNED.withName("idProduct"),
            JAVA_SHORT_UNALIGNED.withName("bcdDevice"),
            JAVA_BYTE.withName("iManufacturer"),
            JAVA_BYTE.withName("iProduct"),
            JAVA_BYTE.withName("iSerialNumber"),
            JAVA_BYTE.withName("bNumConfigurations")
    );

    private static final long bcdUSB$OFFSET = 2;
    private static final long bDeviceClass$OFFSET = 4;
    private static final long bDeviceSubClass$OFFSET = 5;
    private static final long bDeviceProtocol$OFFSET = 6;
    private static final long idVendor$OFFSET = 8;
    private static final long idProduct$OFFSET = 10;
    private static final long bcdDevice$OFFSET = 12;
    private static final long iManufacturer$OFFSET = 14;
    private static final long iProduct$OFFSET = 15;
    private static final long iSerialNumber$OFFSET = 16;


    static {
        assert LAYOUT.byteSize() == 18;
    }
}
