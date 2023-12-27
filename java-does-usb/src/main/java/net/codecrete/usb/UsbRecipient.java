//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB control transfer recipient enumeration.
 */
public enum UsbRecipient {
    /**
     * USB device
     */
    DEVICE,
    /**
     * USB interface
     */
    INTERFACE,
    /**
     * USB endpoint
     */
    ENDPOINT,
    /**
     * Other recipient
     */
    OTHER
}
