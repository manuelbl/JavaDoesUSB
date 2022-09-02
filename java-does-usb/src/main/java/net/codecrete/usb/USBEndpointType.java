//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB endpoint type.
 */
public enum USBEndpointType {
    /**
     * Bulk transfer
     */
    BULK,
    /**
     * Interrupt transfer
     */
    INTERRUPT,
    /**
     * Isochronous transfer
     */
    ISOCHRONOUS
}
