//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import java.util.List;

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
public class USBDeviceFilter {
    private Integer vendorId;
    private Integer productId;
    private Integer classCode;
    private Integer subclassCode;
    private Integer protocolCode;
    private String serialNumber;

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
        this.vendorId = vendorId;
        this.productId = productId;
    }

    /**
     * Creates a new instance that matches the specified vendor ID, product ID and serial number.
     *
     * @param vendorId     vendor ID
     * @param productId    product ID
     * @param serialNumber serial number
     */
    public USBDeviceFilter(int vendorId, int productId, String serialNumber) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.serialNumber = serialNumber;
    }

    /**
     * Gets the USB vendor ID.
     *
     * @return vendor ID, or {@code null} if the vendor ID is not relevant for matching
     */
    public Integer getVendorId() {
        return vendorId;
    }

    /**
     * Sets the USB vendor ID.
     *
     * @param vendorId vendor ID, or {@code null} if the vendor ID is not relevant for matching
     */
    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    /**
     * Gets the USB product ID.
     *
     * @return product ID, or {@code null} if the product ID is not relevant for matching
     */
    public Integer getProductId() {
        return productId;
    }

    /**
     * Sets the USB product ID.
     *
     * @param productId product ID, or {@code null} if the product ID is not relevant for matching
     */
    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    /**
     * Gets the USB device class code.
     *
     * @return class code, or {@code null} if the class code is not relevant for matching
     */
    public Integer getClassCode() {
        return classCode;
    }

    /**
     * Sets the USB device class code.
     *
     * @param classCode class code, or {@code null} if the class code is not relevant for matching
     */
    public void setClassCode(Integer classCode) {
        this.classCode = classCode;
    }

    /**
     * Gets the USB device subclass code.
     *
     * @return subclass code, or {@code null} if the subclass code is not relevant for matching
     */
    public Integer getSubclassCode() {
        return subclassCode;
    }

    /**
     * Sets the USB device subclass code.
     *
     * @param subclassCode subclass code, or {@code null} if the subclass code is not relevant for matching
     */
    public void setSubclassCode(Integer subclassCode) {
        this.subclassCode = subclassCode;
    }

    /**
     * Gets the USB device protocol code.
     *
     * @return protocol code, or {@code null} if the protocol code is not relevant for matching
     */
    public Integer getProtocolCode() {
        return protocolCode;
    }

    /**
     * Sets the USB device protocol code.
     *
     * @param protocolCode protocol code, or {@code null} if the protocol code is not relevant for matching
     */
    public void setProtocolCode(Integer protocolCode) {
        this.protocolCode = protocolCode;
    }

    /**
     * Gets the device serial number.
     *
     * @return serial number, or {@code null} if the serial number is not relevant for matching
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets the device serial number.
     *
     * @param serialNumber serial number, or {@code null} if the serial number is not relevant for matching
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Tests if the specified USB device matches this filter.
     *
     * @param device USB device
     * @return {@code true} if it matches, {@code false} otherwise
     */
    public boolean matches(USBDevice device) {
        if (vendorId != null && device.getVendorId() != vendorId)
            return false;
        if (productId != null && device.getProductId() != productId)
            return false;
        if (serialNumber != null && !serialNumber.equals(device.getSerial()))
            return false;
        if (classCode != null && device.getClassCode() != classCode)
            return false;
        if (subclassCode != null && device.getSubclassCode() != subclassCode)
            return false;
        return protocolCode == null || device.getProtocolCode() == protocolCode;
    }

    /**
     * Test if the USB devices matches any of the filter conditions.
     * @param device the USB device
     * @param filters a list of filter conditions
     * @return {@code true} if it matches, {@code false} otherwise
     */
    public static boolean matchesAny(USBDevice device, List<USBDeviceFilter> filters) {
        return filters.stream().anyMatch(filter -> filter.matches(device));
    }
}
