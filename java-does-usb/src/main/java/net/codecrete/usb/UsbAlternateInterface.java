//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * USB alternate interface setting.
 * <p>
 * Instances of this class describe an alternate setting of a USB interface.
 * </p>
 */
public interface UsbAlternateInterface {

    /**
     * Gets the alternate setting number.
     * <p>
     * It is equal to the {@code bAlternateSetting} field of the interface descriptor.
     *
     * @return the alternate setting number
     */
    int getNumber();

    /**
     * Gets the interface class.
     * <p>
     * Interface classes are defined by the USB standard.
     * </p>
     *
     * @return the interface class
     */
    int getClassCode();

    /**
     * Gets the interface subclass.
     * <p>
     * Interface subclasses are defined by the USB standard.
     * </p>
     *
     * @return the interface subclass
     */
    int getSubclassCode();

    /**
     * Gets the interface protocol.
     * <p>
     * Interface protocols are defined by the USB standard.
     * </p>
     *
     * @return the interface protocol
     */
    int getProtocolCode();

    /**
     * Gets the endpoints of this alternate interface settings.
     * <p>
     * The endpoint list does not include endpoint 0, which
     * is always available and reserved for control transfers.
     * </p>
     * <p>
     * The returned list is sorted by endpoint number.
     * </p>
     *
     * @return a list of endpoints.
     */
    @NotNull
    @Unmodifiable
    List<UsbEndpoint> getEndpoints();

    /**
     * Gets the endpoint with the specified number and direction.
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127, without the direction bit)
     * @param direction      endpoint direction
     * @return the endpoint
     * @exception UsbException if the endpoint does not exist
     */
    @NotNull UsbEndpoint getEndpoint(int endpointNumber, UsbDirection direction);

}
