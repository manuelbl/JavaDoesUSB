//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBDeviceInfo;

public abstract class USBDeviceInfoImpl implements USBDeviceInfo {


    protected final Object id;
    protected final int productId;
    protected final int vendorId;
    protected final String product;
    protected final String manufacturer;
    protected final String serial;
    protected final int classCode;
    protected final int subclassCode;
    protected final int protocolCode;

    /**
     * Creates a new instance.
     *
     * @param id           unique device ID
     * @param vendorId     USB vendor ID
     * @param productId    USB product ID
     * @param manufacturer manufacturer name
     * @param product      product name
     * @param serial       serial number
     * @param classCode    USB device class code
     * @param subclassCode USB device subclass code
     * @param protocolCode USB device protocol
     */
    protected USBDeviceInfoImpl(
            Object id, int vendorId, int productId,
            String manufacturer, String product, String serial,
            int classCode, int subclassCode, int protocolCode) {

        assert id != null;

        this.id = id;
        this.productId = productId;
        this.vendorId = vendorId;
        this.product = product;
        this.manufacturer = manufacturer;
        this.serial = serial;
        this.classCode = classCode;
        this.subclassCode = subclassCode;
        this.protocolCode = protocolCode;
    }

    public abstract USBDevice open();

    public int getProductId() {
        return productId;
    }

    public int getVendorId() {
        return vendorId;
    }

    public String getProduct() {
        return product;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getSerial() {
        return serial;
    }

    public int getClassCode() {
        return classCode;
    }

    public int getSubclassCode() {
        return subclassCode;
    }

    public int getProtocolCode() {
        return protocolCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        USBDeviceInfoImpl that = (USBDeviceInfoImpl) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "VID: 0x" +
                String.format("%04x", vendorId) +
                ", PID: 0x" +
                String.format("%04x", productId) +
                ", manufacturer: " +
                manufacturer +
                ", product: " +
                product +
                ", serial: " +
                serial +
                ", ID: " +
                id;
    }
}
