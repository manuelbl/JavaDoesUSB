//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.*;

import java.util.Collections;
import java.util.List;

public abstract class USBDeviceImpl implements USBDevice {

    protected final Object id;
    protected final int vendorId;
    protected final int productId;
    protected final String manufacturer;
    protected final String product;
    protected final String serial;
    protected int classCode;
    protected int subclassCode;
    protected int protocolCode;

    protected List<USBInterface> interfaces;

    /**
     * Creates a new instance.
     *
     * @param id           unique device ID
     * @param vendorId     USB vendor ID
     * @param productId    USB product ID
     * @param manufacturer manufacturer name
     * @param product      product name
     * @param serial       serial number
     */
    protected USBDeviceImpl(Object id, int vendorId, int productId, String manufacturer, String product,
                            String serial) {

        assert id != null;

        this.id = id;
        this.vendorId = vendorId;
        this.productId = productId;
        this.manufacturer = manufacturer;
        this.product = product;
        this.serial = serial;
    }

    @Override
    public abstract void open();

    @Override
    public abstract void close();

    @Override
    public abstract boolean isOpen();

    protected void checkIsOpen() {
        if (!isOpen())
            throw new USBException("The device needs to be open to call this method");
    }

    @Override
    public int getProductId() {
        return productId;
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    @Override
    public String getProduct() {
        return product;
    }

    @Override
    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getSerial() {
        return serial;
    }

    @Override
    public int getClassCode() {
        return classCode;
    }

    @Override
    public int getSubclassCode() {
        return subclassCode;
    }

    @Override
    public int getProtocolCode() {
        return protocolCode;
    }

    public Object getUniqueId() {
        return id;
    }

    @Override
    public List<USBInterface> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    public void setInterfaces(List<USBInterface> interfaces) {
        this.interfaces = interfaces;
    }

    @Override
    public abstract void claimInterface(int interfaceNumber);

    @Override
    public abstract void releaseInterface(int interfaceNumber);

    @Override
    public abstract byte[] controlTransferIn(USBControlTransfer setup, int length);

    @Override
    public abstract void controlTransferOut(USBControlTransfer setup, byte[] data);


    @Override
    public abstract void transferOut(int endpointNumber, byte[] data);

    @Override
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
