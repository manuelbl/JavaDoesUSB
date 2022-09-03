//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import java.util.List;

/**
 * USB interface.
 * <p>
 * Instances of this class describe an interface of a USB device.
 * </p>
 */
public interface USBInterface {

    /**
     * Gets the interface number.
     * <p>
     * It is equal to the {@code bInterfaceNumber} field of the interface descriptor.
     *
     * @return the interface number
     */
    int number();

    /**
     * Indicates if this interface is currently claimed for exclusive access.
     *
     * @return {@code true} if it is claimed, {@code false} otherwise.
     */
    boolean isClaimed();

    /**
     * Gets the currently selected alternate interface setting.
     * <p>
     * Initially, the alternate settings with number 0 is selected.
     * </p>
     *
     * @return the alternate interface setting.
     */
    USBAlternateInterface alternate();

    /**
     * Gets all alternate settings of this interface.
     *
     * @return a list of the alternate settings
     */
    List<USBAlternateInterface> alternates();
}
