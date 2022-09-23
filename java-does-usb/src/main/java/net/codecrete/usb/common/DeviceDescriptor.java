//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

public class DeviceDescriptor {

    private final MemorySegment descriptor;

    public DeviceDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public int bcdUSB() {
        return 0xffff & (short) bcdUSB$VH.get(descriptor);
    }

    public int bDeviceClass() {
        return 0xff & (byte) bDeviceClass$VH.get(descriptor);
    }

    public int bDeviceSubClass() {
        return 0xff & (byte) bDeviceSubClass$VH.get(descriptor);
    }

    public int bDeviceProtocol() {
        return 0xff & (byte) bDeviceProtocol$VH.get(descriptor);
    }

    public int idVendor() {
        return 0xffff & (short) idVendor$VH.get(descriptor);
    }

    public int idProduct() {
        return 0xffff & (short) idProduct$VH.get(descriptor);
    }

    public int bcdDevice() {
        return 0xffff & (short) bcdDevice$VH.get(descriptor);
    }

    public int iManufacturer() {
        return 0xffff & (short) iManufacturer$VH.get(descriptor);
    }

    public int iProduct() {
        return 0xffff & (short) iProduct$VH.get(descriptor);
    }

    public int iSerialNumber() {
        return 0xffff & (short) iSerialNumber$VH.get(descriptor);
    }

    // typedef struct {
    //    UCHAR   bLength;
    //    UCHAR   bDescriptorType;
    //    USHORT  bcdUSB;
    //    UCHAR   bDeviceClass;
    //    UCHAR   bDeviceSubClass;
    //    UCHAR   bDeviceProtocol;
    //    UCHAR   bMaxPacketSize0;
    //    USHORT  idVendor;
    //    USHORT  idProduct;
    //    USHORT  bcdDevice;
    //    UCHAR   iManufacturer;
    //    UCHAR   iProduct;
    //    UCHAR   iSerialNumber;
    //    UCHAR   bNumConfigurations;
    //} __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_SHORT.withName("bcdUSB"),
            JAVA_BYTE.withName("bDeviceClass"),
            JAVA_BYTE.withName("bDeviceSubClass"),
            JAVA_BYTE.withName("bDeviceProtocol"),
            JAVA_BYTE.withName("bMaxPacketSize0"),
            JAVA_SHORT.withName("idVendor"),
            JAVA_SHORT.withName("idProduct"),
            JAVA_SHORT.withName("bcdDevice"),
            JAVA_BYTE.withName("iManufacturer"),
            JAVA_BYTE.withName("iProduct"),
            JAVA_BYTE.withName("iSerialNumber"),
            JAVA_BYTE.withName("bNumConfigurations")
    );

    private static final VarHandle bcdUSB$VH = LAYOUT.varHandle(groupElement("bcdUSB"));
    private static final VarHandle bDeviceClass$VH = LAYOUT.varHandle(groupElement("bDeviceClass"));
    private static final VarHandle bDeviceSubClass$VH = LAYOUT.varHandle(groupElement("bDeviceSubClass"));
    private static final VarHandle bDeviceProtocol$VH = LAYOUT.varHandle(groupElement("bDeviceProtocol"));
    private static final VarHandle idVendor$VH = LAYOUT.varHandle(groupElement("idVendor"));
    private static final VarHandle idProduct$VH = LAYOUT.varHandle(groupElement("idProduct"));
    private static final VarHandle bcdDevice$VH = LAYOUT.varHandle(groupElement("bcdDevice"));
    private static final VarHandle iManufacturer$VH = LAYOUT.varHandle(groupElement("iManufacturer"));
    private static final VarHandle iProduct$VH = LAYOUT.varHandle(groupElement("iProduct"));
    private static final VarHandle iSerialNumber$VH = LAYOUT.varHandle(groupElement("iSerialNumber"));
}
