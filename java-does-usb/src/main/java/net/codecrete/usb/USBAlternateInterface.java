//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import java.util.List;

/**
 * USB alternate interface setting.
 * <p>
 * Instances of this class describe an alternate setting of a USB interface.
 * </p>
 */
public interface USBAlternateInterface {

    /**
     * Gets the alternate setting number.
     * <p>
     * It is equal to the {@code bAlternateSetting} field of the interface descriptor.
     *
     * @return the alternate setting number
     */
    int number();

    /**
     * Gets the interface class.
     * <p>
     * Interface classes are defined by the USB standard.
     * </p>
     *
     * @return the interface class
     */
    int classCode();

    /**
     * Gets the interface subclass.
     * <p>
     * Interface subclasses are defined by the USB standard.
     * </p>
     *
     * @return the interface subclass
     */
    int subclassCode();

    /**
     * Gets the interface protocol.
     * <p>
     * Interface protocols are defined by the USB standard.
     * </p>
     *
     * @return the interface protocol
     */
    int protocolCode();

    /**
     * Gets the endpoints of this alternate interface settings.
     * <p>
     * The endpoint list does not include endpoint 0, which
     * is always available and reserved for control transfers.
     * </p>
     *
     * @return a list of endpoints.
     */
    List<USBEndpoint> endpoints();
}
