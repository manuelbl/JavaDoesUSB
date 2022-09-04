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

    protected final Object id_;
    protected final int vendorId_;
    protected final int productId_;
    protected final String manufacturer_;
    protected final String product_;
    protected final String serialNumber_;
    protected int classCode_;
    protected int subclassCode_;
    protected int protocolCode_;

    protected List<USBInterface> interfaces_;

    /**
     * Creates a new instance.
     *
     * @param id           unique device ID
     * @param vendorId     USB vendor ID
     * @param productId    USB product ID
     * @param manufacturer manufacturer name
     * @param product      product name
     * @param serialNumber serial number
     */
    protected USBDeviceImpl(Object id, int vendorId, int productId, String manufacturer, String product,
                            String serialNumber) {

        assert id != null;

        id_ = id;
        vendorId_ = vendorId;
        productId_ = productId;
        manufacturer_ = manufacturer;
        product_ = product;
        serialNumber_ = serialNumber;
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
    public int productId() {
        return productId_;
    }

    @Override
    public int vendorId() {
        return vendorId_;
    }

    @Override
    public String product() {
        return product_;
    }

    @Override
    public String manufacturer() {
        return manufacturer_;
    }

    @Override
    public String serialNumber() {
        return serialNumber_;
    }

    @Override
    public int classCode() {
        return classCode_;
    }

    @Override
    public int subclassCode() {
        return subclassCode_;
    }

    @Override
    public int protocolCode() {
        return protocolCode_;
    }

    public Object getUniqueId() {
        return id_;
    }

    public void setClassCodes(int classCode, int subclassCode, int protocolCode) {
        classCode_ = classCode;
        subclassCode_ = subclassCode;
        protocolCode_ = protocolCode;
    }

    @Override
    public List<USBInterface> interfaces() {
        return Collections.unmodifiableList(interfaces_);
    }

    public void setInterfaces(List<USBInterface> interfaces) {
        interfaces_ = interfaces;
    }

    @Override
    public abstract void claimInterface(int interfaceNumber);

    @Override
    public abstract void releaseInterface(int interfaceNumber);

    public void setClaimed(int interfaceNumber, boolean claimed) {
        for (var intf : interfaces_) {
            if (intf.number() == interfaceNumber) {
                ((USBInterfaceImpl)intf).setClaimed(claimed);
                return;
            }
        }
        throw new USBException("Internal error (interface not found)");
    }

    /**
     * Returns the interface with the specified number.
     * 
     * @param interfaceNumber the interface number
     * @return the interface
     */
    protected USBInterfaceImpl getInterface(int interfaceNumber) {
        return (USBInterfaceImpl) interfaces_.stream()
                .filter((intf) -> intf.number() == interfaceNumber).findFirst().orElse(null);
    }
    
    /**
     * Checks if the specified endpoint is valid for communication and returns the endpoint address.
     *
     * @param endpointNumber endpoint number (1 to 127)
     * @param direction transfer direction
     * @return endpoint address
     */
    protected byte getEndpointAddress(int endpointNumber, USBDirection direction) {

        checkIsOpen();

        if (endpointNumber >= 1 && endpointNumber <= 127) {
            for (var intf : interfaces_) {
                if (intf.isClaimed()) {
                    for (var ep : intf.alternate().endpoints()) {
                        if (ep.number() == endpointNumber && ep.direction() == direction)
                            return (byte) (endpointNumber | (direction == USBDirection.IN ? 0x80 : 0));
                    }
                }
            }
        }

        throw new USBException(String.format("Endpoint number %d is not part of a claimed interface, the endpoint " +
                "does not operate in the %s direction or is otherwise invalid", endpointNumber, direction.name()));
    }

    @Override
    public abstract byte[] controlTransferIn(USBControlTransfer setup, int length);

    @Override
    public abstract void controlTransferOut(USBControlTransfer setup, byte[] data);


    @Override
    public abstract void transferOut(int endpointNumber, byte[] data);

    @Override
    public abstract byte[] transferIn(int endpointNumber, int maxLength);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        USBDeviceImpl that = (USBDeviceImpl) o;
        return id_.equals(that.id_);
    }

    @Override
    public int hashCode() {
        return id_.hashCode();
    }

    @Override
    public String toString() {
        return "VID: 0x" + String.format("%04x", vendorId_) + ", PID: 0x" + String.format("%04x", productId_) + ", " +
                "manufacturer: " + manufacturer_ + ", product: " + product_ + ", serial: " + serialNumber_ + ", ID: " + id_;
    }
}
