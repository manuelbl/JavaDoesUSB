//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB endpoint transfer type enumeration.
 */
public enum USBTransferType {
    /**
     * Control transfer
     */
    CONTROL,
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
