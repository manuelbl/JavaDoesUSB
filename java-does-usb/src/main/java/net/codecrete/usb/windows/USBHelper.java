//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.usbstandard.DeviceDescriptor;
import net.codecrete.usb.usbstandard.SetupPacket;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.*;

/**
 * USB constants and struct
 * <p>
 * Mainly used to work around an alignment problem with the generated
 * USB_NODE_CONNECTION_INFORMATION_EX struct.
 * </p>
 */
public class USBHelper {

    public static final byte USB_REQUEST_GET_DESCRIPTOR = 0x06;


    // typedef struct _USB_NODE_CONNECTION_INFORMATION_EX {
    //    ULONG ConnectionIndex;  /* INPUT */
    //    /* usb device descriptor returned by this device
    //       during enumeration */
    //    USB_DEVICE_DESCRIPTOR DeviceDescriptor;/* OUTPUT */
    //    UCHAR CurrentConfigurationValue;/* OUTPUT */
    //    /* values for the speed field are defined in USB200.h */
    //    UCHAR Speed;/* OUTPUT */
    //    BOOLEAN DeviceIsHub;/* OUTPUT */
    //    USHORT DeviceAddress;/* OUTPUT */
    //    ULONG NumberOfOpenPipes;/* OUTPUT */
    //    USB_CONNECTION_STATUS ConnectionStatus;/* OUTPUT */
    //    USB_PIPE_INFO PipeList[0];/* OUTPUT */
    //} USB_NODE_CONNECTION_INFORMATION_EX, *PUSB_NODE_CONNECTION_INFORMATION_EX;
    public static final GroupLayout USB_NODE_CONNECTION_INFORMATION_EX$Struct = structLayout(
            JAVA_INT.withName("ConnectionIndex"),
            DeviceDescriptor.LAYOUT.withName("DeviceDescriptor"),
            JAVA_BYTE.withName("CurrentConfigurationValue"),
            JAVA_BYTE.withName("Speed"),
            JAVA_BYTE.withName("DeviceIsHub"),
            JAVA_SHORT_UNALIGNED.withName("DeviceAddress"),
            JAVA_INT_UNALIGNED.withName("NumberOfOpenPipes"),
            JAVA_INT_UNALIGNED.withName("ConnectionStatus")
            // USB_PIPE_INFO PipeList[0]
    );
    public static final VarHandle USB_NODE_CONNECTION_INFORMATION_EX_ConnectionIndex =
            USB_NODE_CONNECTION_INFORMATION_EX$Struct.varHandle(groupElement("ConnectionIndex"));
    public static final long USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$Offset =
            USB_NODE_CONNECTION_INFORMATION_EX$Struct.byteOffset(groupElement("DeviceDescriptor"));

    public static MemorySegment USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$slice(MemorySegment seg) {
        return seg.asSlice(USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$Offset,
                DeviceDescriptor.LAYOUT.byteSize());
    }

    // typedef struct _USB_DESCRIPTOR_REQUEST {
    //    ULONG ConnectionIndex;
    //    struct {
    //        UCHAR bmRequest;
    //        UCHAR bRequest;
    //        USHORT wValue;
    //        USHORT wIndex;
    //        USHORT wLength;
    //    } SetupPacket;
    //    UCHAR Data[0];
    //} USB_DESCRIPTOR_REQUEST, *PUSB_DESCRIPTOR_REQUEST;
    public static final GroupLayout USB_DESCRIPTOR_REQUEST$Struct = structLayout(JAVA_INT.withName("ConnectionIndex")
            , SetupPacket.LAYOUT.withName("SetupPacket"));
    public static final VarHandle USB_DESCRIPTOR_REQUEST_ConnectionIndex =
            USB_DESCRIPTOR_REQUEST$Struct.varHandle(groupElement("ConnectionIndex"));
    public static final long USB_DESCRIPTOR_REQUEST_SetupPacket$Offset =
            USB_DESCRIPTOR_REQUEST$Struct.byteOffset(groupElement("SetupPacket"));
    public static final long USB_DESCRIPTOR_REQUEST_Data$Offset = USB_DESCRIPTOR_REQUEST$Struct.byteSize();

    // A5DCBF10-6530-11D2-901F-00C04FB951ED
    public static final MemorySegment GUID_DEVINTERFACE_USB_DEVICE = Win.CreateGUID(0xA5DCBF10, (short) 0x6530,
            (short) 0x11D2, (byte) 0x90, (byte) 0x1F, (byte) 0x00, (byte) 0xC0, (byte) 0x4F, (byte) 0xB9, (byte) 0x51
            , (byte) 0xED);

    // f18a0e88-c30c-11d0-8815-00a0c906bed8
    public static final MemorySegment GUID_DEVINTERFACE_USB_HUB = Win.CreateGUID(0xf18a0e88, (short) 0xc30c,
            (short) 0x11d0, (byte) 0x88, (byte) 0x15, (byte) 0x00, (byte) 0xa0, (byte) 0xc9, (byte) 0x06, (byte) 0xbe
            , (byte) 0xd8);

    static {
        assert USB_NODE_CONNECTION_INFORMATION_EX$Struct.byteSize() == 35;
    }
}
