//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * Filter condition for matching USB devices.
 * <p>
 * In order to match this condition, all non-null properties
 * of this instance must be equal to the same properties of the USB device.
 * </p>
 * <p>
 * For a well implemented USB device, the combination of vendor ID,
 * product ID and serial number is globally unique.
 * </p>
 */
public class USBDeviceFilter implements USBDevicePredicate {
    private Integer vid;
    private Integer pid;
    private Integer deviceClass;
    private Integer deviceSubclass;
    private Integer deviceProtocol;
    private String serialString;

    /**
     * Creates a new instance.
     */
    public USBDeviceFilter() {
    }

    /**
     * Creates a new instance that matches the specified vendor and product ID.
     *
     * @param vendorId  vendor ID
     * @param productId product ID
     */
    public USBDeviceFilter(int vendorId, int productId) {
        vid = vendorId;
        pid = productId;
    }

    /**
     * Creates a new instance that matches the specified vendor ID, product ID and serial number.
     *
     * @param vendorId     vendor ID
     * @param productId    product ID
     * @param serialNumber serial number
     */
    public USBDeviceFilter(int vendorId, int productId, String serialNumber) {
        vid = vendorId;
        pid = productId;
        serialString = serialNumber;
    }

    /**
     * Gets the USB vendor ID.
     *
     * @return vendor ID, or {@code null} if the vendor ID is not relevant for matching
     */
    public Integer vendorId() {
        return vid;
    }

    /**
     * Sets the USB vendor ID.
     *
     * @param vendorId vendor ID, or {@code null} if the vendor ID is not relevant for matching
     */
    public void setVendorId(Integer vendorId) {
        vid = vendorId;
    }

    /**
     * Gets the USB product ID.
     *
     * @return product ID, or {@code null} if the product ID is not relevant for matching
     */
    public Integer productId() {
        return pid;
    }

    /**
     * Sets the USB product ID.
     *
     * @param productId product ID, or {@code null} if the product ID is not relevant for matching
     */
    public void setProductId(Integer productId) {
        pid = productId;
    }

    /**
     * Gets the USB device class code.
     *
     * @return class code, or {@code null} if the class code is not relevant for matching
     */
    public Integer classCode() {
        return deviceClass;
    }

    /**
     * Sets the USB device class code.
     *
     * @param classCode class code, or {@code null} if the class code is not relevant for matching
     */
    public void setClassCode(Integer classCode) {
        deviceClass = classCode;
    }

    /**
     * Gets the USB device subclass code.
     *
     * @return subclass code, or {@code null} if the subclass code is not relevant for matching
     */
    public Integer subclassCode() {
        return deviceSubclass;
    }

    /**
     * Sets the USB device subclass code.
     *
     * @param subclassCode subclass code, or {@code null} if the subclass code is not relevant for matching
     */
    public void setSubclassCode(Integer subclassCode) {
        deviceSubclass = subclassCode;
    }

    /**
     * Gets the USB device protocol code.
     *
     * @return protocol code, or {@code null} if the protocol code is not relevant for matching
     */
    public Integer protocolCode() {
        return deviceProtocol;
    }

    /**
     * Sets the USB device protocol code.
     *
     * @param protocolCode protocol code, or {@code null} if the protocol code is not relevant for matching
     */
    public void setProtocolCode(Integer protocolCode) {
        deviceProtocol = protocolCode;
    }

    /**
     * Gets the device serial number.
     *
     * @return serial number, or {@code null} if the serial number is not relevant for matching
     */
    public String serialNumber() {
        return serialString;
    }

    /**
     * Sets the device serial number.
     *
     * @param serialNumber serial number, or {@code null} if the serial number is not relevant for matching
     */
    public void setSerialNumber(String serialNumber) {
        serialString = serialNumber;
    }

    /**
     * Tests if the specified USB device matches this filter.
     *
     * @param device USB device
     * @return {@code true} if it matches, {@code false} otherwise
     */
    public boolean matches(USBDevice device) {
        if (vid != null && device.vendorId() != vid)
            return false;
        if (pid != null && device.productId() != pid)
            return false;
        if (serialString != null && !serialString.equals(device.serialNumber()))
            return false;
        if (deviceClass != null && device.classCode() != deviceClass)
            return false;
        if (deviceSubclass != null && device.subclassCode() != deviceSubclass)
            return false;
        return deviceProtocol == null || device.protocolCode() == deviceProtocol;
    }
}
