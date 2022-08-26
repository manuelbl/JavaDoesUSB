//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * Information about a USB device.
 *
 * <p>
 * Instances of this class are created by {@link USB},
 * e.g. {@link USB#getAllDevices()}.
 * </p>
 * <p>
 * Instances of this class are not associated with any native resources
 * and can thus be easily handled in Java.
 * </p>
 * <p>
 * Multiple instances of the class might exist for the same USB device.
 * Use {@code equals()} to test if they refer to the same device.
 * </p>
 */
public interface USBDeviceInfo {
    /**
     * USB product ID.
     *
     * @return product ID
     */
    int getProductId();

    /**
     * USB vendor ID.
     *
     * @return vendor ID
     */
    int getVendorId();

    /**
     * Product name.
     *
     * @return product name or {@code null} if not provided by the device
     */
    String getProduct();

    /**
     * Manufacturer name
     *
     * @return manufacturer name or {@code null} if not provided by the device
     */
    String getManufacturer();

    /**
     * Serial number
     * <p>
     * Even though this is supposed to be a human-readable string,
     * some devices are known to provide binary data.
     * </p>
     *
     * @return serial number or {@code null} if not provided by the device
     */
    String getSerial();

    /**
     * USB device class code ({@code bDeviceClass} from device descriptor).
     *
     * @return class code
     */
    int getClassCode();

    /**
     * USB device subclass code ({@code bDeviceSubClass} from device descriptor).
     *
     * @return subclass code
     */
    int getSubclassCode();

    /**
     * USB device protocol ({@code bDeviceProtocol} from device descriptor).
     *
     * @return protocol code
     */
    int getProtocolCode();

    /**
     * Opens the device for communication.
     *
     * <p>
     * As part of opening the device, the first configuration is activated.
     * </p>
     *
     * @return USB device
     */
    USBDevice open();
}
