//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.usbstandard;

/**
 * Memory layout of USB descriptors.
 */
public class Constants {

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
}
