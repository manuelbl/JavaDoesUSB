//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import java.lang.foreign.GroupLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.paddingLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.*;

/**
 * Data structures and constants related to the USB device file system.
 */
public class USBDevFS {

    public static final long CONTROL = 0xc0185500;
    public static final long BULK = 0xc0185502;
    public static final long CLAIMINTERFACE = 0x8004550F;
    public static final long RELEASEINTERFACE = 0x80045510;

    // struct usbdevfs_ctrltransfer {
    //	__u8 bRequestType;
    //	__u8 bRequest;
    //	__u16 wValue;
    //	__u16 wIndex;
    //	__u16 wLength;
    //	__u32 timeout;  /* in milliseconds */
    // 	void *data;
    //};
    public static final GroupLayout ctrltransfer$Struct = structLayout(
            JAVA_BYTE.withName("bRequestType"),
            JAVA_BYTE.withName("bRequest"),
            JAVA_SHORT.withName("wValue"),
            JAVA_SHORT.withName("wIndex"),
            JAVA_SHORT.withName("wLength"),
            JAVA_INT.withName("timeout"),
            paddingLayout(4 * 8),
            ADDRESS.withName("data")
    );

    public static final VarHandle ctrltransfer_bRequestType = ctrltransfer$Struct.varHandle(groupElement("bRequestType"));
    public static final VarHandle ctrltransfer_bRequest = ctrltransfer$Struct.varHandle(groupElement("bRequest"));
    public static final VarHandle ctrltransfer_wValue = ctrltransfer$Struct.varHandle(groupElement("wValue"));
    public static final VarHandle ctrltransfer_wIndex = ctrltransfer$Struct.varHandle(groupElement("wIndex"));
    public static final VarHandle ctrltransfer_wLength = ctrltransfer$Struct.varHandle(groupElement("wLength"));
    public static final VarHandle ctrltransfer_timeout = ctrltransfer$Struct.varHandle(groupElement("timeout"));
    public static final VarHandle ctrltransfer_data = ctrltransfer$Struct.varHandle(groupElement("data"));

    // struct usbdevfs_bulktransfer {
    //	unsigned int ep;
    //	unsigned int len;
    //	unsigned int timeout; /* in milliseconds */
    //	void *data;
    //};
    public static final GroupLayout bulktransfer$Struct = structLayout(
            JAVA_INT.withName("ep"),
            JAVA_INT.withName("len"),
            JAVA_INT.withName("timeout"),
            paddingLayout(4 * 8),
            ADDRESS.withName("data")
    );

    public static final VarHandle bulktransfer_ep = bulktransfer$Struct.varHandle(groupElement("ep"));
    public static final VarHandle bulktransfer_len = bulktransfer$Struct.varHandle(groupElement("len"));
    public static final VarHandle bulktransfer_timeout = bulktransfer$Struct.varHandle(groupElement("timeout"));
    public static final VarHandle bulktransfer_data = bulktransfer$Struct.varHandle(groupElement("data"));
}
