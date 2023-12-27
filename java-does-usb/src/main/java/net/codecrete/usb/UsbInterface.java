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
 * USB interface.
 * <p>
 * Instances of this class describe an interface of a USB device.
 * </p>
 */
public interface UsbInterface {

    /**
     * Gets the interface number.
     * <p>
     * It is equal to the {@code bInterfaceNumber} field of the interface descriptor.
     * </p>
     *
     * @return the interface number
     */
    int getNumber();

    /**
     * Indicates if this interface has been claimed by this program for exclusive access.
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
    @NotNull UsbAlternateInterface getCurrentAlternate();

    /**
     * Gets the alternate interface settings with the specified number.
     *
     * @param alternateNumber alternate setting number
     * @return alternate interface setting
     * @throws UsbException if the alternate setting does not exist
     */
    @NotNull UsbAlternateInterface getAlternate(int alternateNumber);

    /**
     * Gets all alternate settings of this interface.
     * <p>
     * The returned list is sorted by alternate setting number.
     * </p>
     *
     * @return a list of the alternate settings
     */
    @NotNull @Unmodifiable List<UsbAlternateInterface> getAlternates();
}
