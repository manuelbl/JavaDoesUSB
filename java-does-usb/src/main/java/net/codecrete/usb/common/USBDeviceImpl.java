//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBControlTransfer;
import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBRecipient;
import net.codecrete.usb.USBRequestType;

public abstract class USBDeviceImpl implements USBDevice {

    protected final Object id;
    protected final int vendorId;
    protected final int productId;
    protected final String manufacturer;
    protected final String product;
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
    protected USBDeviceImpl(Object id, int vendorId, int productId, String manufacturer, String product,
                            String serial, int classCode, int subclassCode, int protocolCode) {

        assert id != null;

        this.id = id;
        this.vendorId = vendorId;
        this.productId = productId;
        this.manufacturer = manufacturer;
        this.product = product;
        this.serial = serial;
        this.classCode = classCode;
        this.subclassCode = subclassCode;
        this.protocolCode = protocolCode;
    }

    public abstract void open();

    public abstract void close();

    public abstract boolean isOpen();

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

    public Object getUniqueId() {
        return id;
    }

    public abstract void claimInterface(int interfaceNumber);

    public abstract void releaseInterface(int interfaceNumber);

    public abstract byte[] controlTransferIn(USBControlTransfer setup, int length);

    public abstract void controlTransferOut(USBControlTransfer setup, byte[] data);


    public abstract void transferOut(int endpointNumber, byte[] data);

    public abstract byte[] transferIn(int endpointNumber, int maxLength);

    protected byte[] getDescriptor(int descriptorType, int index, int language) {
        // get descriptor header
        var result = controlTransferIn(new USBControlTransfer(USBRequestType.STANDARD, USBRecipient.DEVICE,
                (byte) 0x06, (short) (descriptorType << 8 | index), (short) language), 9);

        // get effective length from header
        int length;
        if (descriptorType == USBDescriptors.CONFIGURATION_DESCRIPTOR_TYPE)
            length = (result[2] & 255) + 256 * (result[3] & 255);
        else length = result[0] & 255;

        // get full descriptor
        result = controlTransferIn(new USBControlTransfer(USBRequestType.STANDARD, USBRecipient.DEVICE, (byte) 0x06,
                (short) (descriptorType << 8 | index), (short) language), length);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        USBDeviceImpl that = (USBDeviceImpl) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "VID: 0x" + String.format("%04x", vendorId) + ", PID: 0x" + String.format("%04x", productId) + ", " +
                "manufacturer: " + manufacturer + ", product: " + product + ", serial: " + serial + ", ID: " + id;
    }
}
