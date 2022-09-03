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
    private Integer vendorId_;
    private Integer productId_;
    private Integer classCode_;
    private Integer subclassCode_;
    private Integer protocolCode_;
    private String serialNumber_;

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
        vendorId_ = vendorId;
        productId_ = productId;
    }

    /**
     * Creates a new instance that matches the specified vendor ID, product ID and serial number.
     *
     * @param vendorId     vendor ID
     * @param productId    product ID
     * @param serialNumber serial number
     */
    public USBDeviceFilter(int vendorId, int productId, String serialNumber) {
        vendorId_ = vendorId;
        productId_ = productId;
        serialNumber_ = serialNumber;
    }

    /**
     * Gets the USB vendor ID.
     *
     * @return vendor ID, or {@code null} if the vendor ID is not relevant for matching
     */
    public Integer vendorId() {
        return vendorId_;
    }

    /**
     * Sets the USB vendor ID.
     *
     * @param vendorId vendor ID, or {@code null} if the vendor ID is not relevant for matching
     */
    public void setVendorId(Integer vendorId) {
        vendorId_ = vendorId;
    }

    /**
     * Gets the USB product ID.
     *
     * @return product ID, or {@code null} if the product ID is not relevant for matching
     */
    public Integer productId() {
        return productId_;
    }

    /**
     * Sets the USB product ID.
     *
     * @param productId product ID, or {@code null} if the product ID is not relevant for matching
     */
    public void setProductId(Integer productId) {
        productId_ = productId;
    }

    /**
     * Gets the USB device class code.
     *
     * @return class code, or {@code null} if the class code is not relevant for matching
     */
    public Integer classCode() {
        return classCode_;
    }

    /**
     * Sets the USB device class code.
     *
     * @param classCode class code, or {@code null} if the class code is not relevant for matching
     */
    public void setClassCode(Integer classCode) {
        classCode_ = classCode;
    }

    /**
     * Gets the USB device subclass code.
     *
     * @return subclass code, or {@code null} if the subclass code is not relevant for matching
     */
    public Integer subclassCode() {
        return subclassCode_;
    }

    /**
     * Sets the USB device subclass code.
     *
     * @param subclassCode subclass code, or {@code null} if the subclass code is not relevant for matching
     */
    public void setSubclassCode(Integer subclassCode) {
        subclassCode_ = subclassCode;
    }

    /**
     * Gets the USB device protocol code.
     *
     * @return protocol code, or {@code null} if the protocol code is not relevant for matching
     */
    public Integer protocolCode() {
        return protocolCode_;
    }

    /**
     * Sets the USB device protocol code.
     *
     * @param protocolCode_ protocol code, or {@code null} if the protocol code is not relevant for matching
     */
    public void setProtocolCode_(Integer protocolCode_) {
        protocolCode_ = protocolCode_;
    }

    /**
     * Gets the device serial number.
     *
     * @return serial number, or {@code null} if the serial number is not relevant for matching
     */
    public String serialNumber() {
        return serialNumber_;
    }

    /**
     * Sets the device serial number.
     *
     * @param serialNumber serial number, or {@code null} if the serial number is not relevant for matching
     */
    public void setSerialNumber(String serialNumber) {
        serialNumber_ = serialNumber;
    }

    /**
     * Tests if the specified USB device matches this filter.
     *
     * @param device USB device
     * @return {@code true} if it matches, {@code false} otherwise
     */
    public boolean matches(USBDevice device) {
        if (vendorId_ != null && device.vendorId() != vendorId_)
            return false;
        if (productId_ != null && device.productId() != productId_)
            return false;
        if (serialNumber_ != null && !serialNumber_.equals(device.serialNumber()))
            return false;
        if (classCode_ != null && device.classCode() != classCode_)
            return false;
        if (subclassCode_ != null && device.subclassCode() != subclassCode_)
            return false;
        return protocolCode_ == null || device.protocolCode() == protocolCode_;
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
