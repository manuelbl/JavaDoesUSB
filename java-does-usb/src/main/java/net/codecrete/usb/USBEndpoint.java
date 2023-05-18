//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB endpoint.
 * <p>
 * Instances of this class describe a USB endpoint.
 * </p>
 */
public interface USBEndpoint {

    /**
     * Gets the USB endpoint number.
     * <p>
     * It is extracted from the {@code bEndpointAddress} field in the
     * endpoint descriptor. The value is in the range 1 to 127 and does
     * not include the bit indicating the direction.
     * </p>
     * <p>
     * Use this number when calling any of the transfer methods of a
     * {@link USBDevice} instance.
     * </p>
     *
     * @return the endpoint number
     */
    int number();

    /**
     * Gets the direction of the endpoint.
     * <p>
     * This information is derived from the direction bit of the
     * {@code bEndpointAddress} field in the endpoint descriptor.
     * </p>
     *
     * @return the direction
     */
    USBDirection direction();

    /**
     * Gets the USB endpoint transfer type.
     *
     * @return the transfer type
     */
    USBTransferType transferType();

    /**
     * Gets the packet size.
     * <p>
     * USB data transfer is divided into packets. This value
     * indicates the maximum packet size.
     * </p>
     *
     * @return the packet size, in bytes.
     */
    int packetSize();
}
