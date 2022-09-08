//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB control transfer request type enumeration.
 */
public enum USBRequestType {
    /**
     * Standard request type
     */
    STANDARD,
    /**
     * USB class specific request type
     */
    CLASS,
    /**
     * Vendor specific request type
     */
    VENDOR
}
