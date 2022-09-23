//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.GroupLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Memory layout of USB descriptors.
 */
public class USBDescriptors {

    public static final byte DEVICE_DESCRIPTOR_TYPE = 0x01;
    public static final byte CONFIGURATION_DESCRIPTOR_TYPE = 0x02;
    public static final byte STRING_DESCRIPTOR_TYPE = 0x03;
    public static final byte INTERFACE_DESCRIPTOR_TYPE = 0x04;
    public static final byte ENDPOINT_DESCRIPTOR_TYPE = 0x05;
    public static final byte DEVICE_QUALIFIER_DESCRIPTOR_TYPE = 0x06;
    public static final byte OTHER_SPEED_CONFIGURATION_DESCRIPTOR_TYPE = 0x07;
    public static final byte INTERFACE_POWER_DESCRIPTOR_TYPE = 0x08;
    public static final byte OTG_DESCRIPTOR_TYPE = 0x09;
    public static final byte DEBUG_DESCRIPTOR_TYPE = 0x0a;
    public static final byte INTERFACE_ASSOCIATION_DESCRIPTOR_TYPE = 0x0b;
    public static final byte BOS_DESCRIPTOR_TYPE = 0x0f;
    public static final byte DEVICE_CAPABILITY_DESCRIPTOR_TYPE = 0x10;
    public static final byte HID_DESCRIPTOR_TYPE = 0x21;
    public static final byte CS_INTERFACE_DESCRIPTOR_TYPE = 0x24;
    public static final byte CS_ENDPOINT_DESCRIPTOR_TYPE = 0x25;
    public static final byte USB_20_HUB_DESCRIPTOR_TYPE = 0x29;
    public static final byte USB_30_HUB_DESCRIPTOR_TYPE = 0x2a;
    public static final byte SUPERSPEED_ENDPOINT_COMPANION_DESCRIPTOR_TYPE = 0x30;
    public static final byte SUPERSPEEDPLUS_ISOCH_ENDPOINT_COMPANION_DESCRIPTOR_TYPE = 0x31;

    public static final short DEFAULT_LANGUAGE = 0x0409;

    // struct USBConfigurationDescriptor
    //{
    //    uint8_t  bLength;
    //    uint8_t  bDescriptorType;
    //    uint16_t wTotalLength;
    //    uint8_t  bNumInterfaces;
    //    uint8_t  bConfigurationValue;
    //    uint8_t  iConfiguration;
    //    uint8_t  bmAttributes;
    //    uint8_t  MaxPower;
    //} __attribute__((packed));

    /**
     * USB configuration descriptor
     */
    public static final GroupLayout Configuration = structLayout(JAVA_BYTE.withName("bLength"), JAVA_BYTE.withName(
            "bDescriptorType"), JAVA_SHORT.withName("wTotalLength"), JAVA_BYTE.withName("bNumInterfaces"),
            JAVA_BYTE.withName("bConfigurationValue"), JAVA_BYTE.withName("iConfiguration"), JAVA_BYTE.withName(
                    "bmAttributes"), JAVA_BYTE.withName("bMaxPower"));

    public static final VarHandle Configuration_bLength = Configuration.varHandle(groupElement("bLength"));
    public static final VarHandle Configuration_bDescriptorType = Configuration.varHandle(groupElement(
            "bDescriptorType"));
    public static final VarHandle Configuration_wTotalLength = Configuration.varHandle(groupElement("wTotalLength"));
    public static final VarHandle Configuration_bNumInterfaces = Configuration.varHandle(groupElement("bNumInterfaces"
    ));
    public static final VarHandle Configuration_bConfigurationValue = Configuration.varHandle(groupElement(
            "bConfigurationValue"));
    public static final VarHandle Configuration_iConfiguration = Configuration.varHandle(groupElement("iConfiguration"
    ));
    public static final VarHandle Configuration_bmAttributes = Configuration.varHandle(groupElement("bmAttributes"));
    public static final VarHandle Configuration_bMaxPower = Configuration.varHandle(groupElement("bMaxPower"));

    // struct USBInterfaceDescriptor
    // {
    //  uint8_t bLength;
    //  uint8_t bDescriptorType;
    //  uint8_t bInterfaceNumber;
    //  uint8_t bAlternateSetting;
    //  uint8_t bNumEndpoints;
    //  uint8_t bInterfaceClass;
    //  uint8_t bInterfaceSubClass;
    //  uint8_t bInterfaceProtocol;
    //  uint8_t iInterface;
    // } __attribute__((packed));

    public static final GroupLayout Interface = structLayout(JAVA_BYTE.withName("bLength"), JAVA_BYTE.withName(
            "bDescriptorType"), JAVA_BYTE.withName("bInterfaceNumber"), JAVA_BYTE.withName("bAlternateSetting"),
            JAVA_BYTE.withName("bNumEndpoints"), JAVA_BYTE.withName("bInterfaceClass"), JAVA_BYTE.withName(
                    "bInterfaceSubClass"), JAVA_BYTE.withName("bInterfaceProtocol"), JAVA_BYTE.withName("iInterface"));

    public static final VarHandle Interface_bLength = Interface.varHandle(groupElement("bLength"));
    public static final VarHandle Interface_bDescriptorType = Interface.varHandle(groupElement("bDescriptorType"));
    public static final VarHandle Interface_bInterfaceNumber = Interface.varHandle(groupElement("bInterfaceNumber"));
    public static final VarHandle Interface_bAlternateSetting = Interface.varHandle(groupElement("bAlternateSetting"));
    public static final VarHandle Interface_bNumEndpoints = Interface.varHandle(groupElement("bNumEndpoints"));
    public static final VarHandle Interface_bInterfaceClass = Interface.varHandle(groupElement("bInterfaceClass"));
    public static final VarHandle Interface_bInterfaceSubClass = Interface.varHandle(groupElement("bInterfaceSubClass"
    ));
    public static final VarHandle Interface_bInterfaceProtocol = Interface.varHandle(groupElement("bInterfaceProtocol"
    ));
    public static final VarHandle Interface_iInterface = Interface.varHandle(groupElement("iInterface"));


    // struct USBInterfaceDescriptor
    // {
    //  uint8_t bLength;
    //  uint8_t bDescriptorType;
    //  uint8_t bEndpointAddress;
    //  uint8_t bmAttributes;
    //  uint16_t wMaxPacketSize;
    //  uint8_t bInterval;
    // } __attribute__((packed));

    public static final GroupLayout Endpoint = structLayout(JAVA_BYTE.withName("bLength"), JAVA_BYTE.withName(
            "bDescriptorType"), JAVA_BYTE.withName("bEndpointAddress"), JAVA_BYTE.withName("bmAttributes"),
            JAVA_SHORT.withName("wMaxPacketSize").withBitAlignment(8), JAVA_BYTE.withName("bInterval"));

    public static final VarHandle Endpoint_bLength = Endpoint.varHandle(groupElement("bLength"));
    public static final VarHandle Endpoint_bDescriptorType = Endpoint.varHandle(groupElement("bDescriptorType"));
    public static final VarHandle Endpoint_bEndpointAddress = Endpoint.varHandle(groupElement("bEndpointAddress"));
    public static final VarHandle Endpoint_bmAttributes = Endpoint.varHandle(groupElement("bmAttributes"));
    public static final VarHandle Endpoint_wMaxPacketSize = Endpoint.varHandle(groupElement("wMaxPacketSize"));
    public static final VarHandle Endpoint_bInterval = Endpoint.varHandle(groupElement("bInterval"));


    static {
        assert Configuration.byteSize() == 9;
        assert Interface.byteSize() == 9;
        assert Endpoint.byteSize() == 7;
    }
}
