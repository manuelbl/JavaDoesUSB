//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.*;
import net.codecrete.usb.usbstandard.DeviceDescriptor;

import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public abstract class USBDeviceImpl implements USBDevice {

    protected final Object id_;
    protected final int vendorId_;
    protected final int productId_;
    protected String manufacturer_;
    protected String product_;
    protected String serialNumber_;
    protected int classCode_;
    protected int subclassCode_;
    protected int protocolCode_;
    protected Version usbVersion_;
    protected Version deviceVersion_;

    protected List<USBInterface> interfaces_;
    protected byte[] configurationDescriptor_;

    /**
     * Creates a new instance.
     *
     * @param id        unique device ID
     * @param vendorId  USB vendor ID
     * @param productId USB product ID
     */
    protected USBDeviceImpl(Object id, int vendorId, int productId) {

        assert id != null;

        id_ = id;
        vendorId_ = vendorId;
        productId_ = productId;
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

    @Override
    public Version usbVersion() {
        return usbVersion_;
    }

    @Override
    public Version deviceVersion() {
        return deviceVersion_;
    }

    @Override
    public byte[] configurationDescriptor() {
        return configurationDescriptor_;
    }

    public Object getUniqueId() {
        return id_;
    }

    /**
     * Sets the class codes and version for the device descriptor.
     *
     * @param descriptor the device descriptor
     */
    public void setFromDeviceDescriptor(MemorySegment descriptor) {
        var deviceDescriptor = new DeviceDescriptor(descriptor);
        classCode_ = deviceDescriptor.deviceClass() & 255;
        subclassCode_ = deviceDescriptor.deviceSubClass() & 255;
        protocolCode_ = deviceDescriptor.deviceProtocol() & 255;
        usbVersion_ = new Version(deviceDescriptor.usbVersion());
        deviceVersion_ = new Version(deviceDescriptor.deviceVersion());
    }

    /**
     * Sets the configuration descriptor and derives the interface and endpoint descriptions.
     *
     * @param descriptor configuration descriptor
     * @return parsed configuration
     */
    protected Configuration setConfigurationDescriptor(MemorySegment descriptor) {
        configurationDescriptor_ = descriptor.toArray(JAVA_BYTE);
        var configuration = ConfigurationParser.parseConfigurationDescriptor(descriptor);
        interfaces_ = configuration.interfaces();
        return configuration;
    }

    /**
     * Set the product strings.
     *
     * @param manufacturer manufacturer name
     * @param product      product name
     * @param serialNumber serial number
     */
    public void setProductStrings(String manufacturer, String product, String serialNumber) {
        manufacturer_ = manufacturer;
        product_ = product;
        serialNumber_ = serialNumber;
    }

    /**
     * Sets the product strings from the device descriptor.
     * <p>
     * To lookup the string, a lookup function is provided. It takes the
     * string ID and returns the string from the string descriptor.
     * </p>
     *
     * @param descriptor   device descriptor
     * @param stringLookup string lookup function
     */
    public void setProductString(MemorySegment descriptor, Function<Integer, String> stringLookup) {
        var deviceDescriptor = new DeviceDescriptor(descriptor);
        manufacturer_ = stringLookup.apply(deviceDescriptor.iManufacturer());
        product_ = stringLookup.apply(deviceDescriptor.iProduct());
        serialNumber_ = stringLookup.apply(deviceDescriptor.iSerialNumber());
    }

    public void setClassCodes(int classCode, int subclassCode, int protocolCode) {
        classCode_ = classCode;
        subclassCode_ = subclassCode;
        protocolCode_ = protocolCode;
    }

    public void setVersions(int usbVersion, int deviceVersion) {
        usbVersion_ = new Version(usbVersion);
        deviceVersion_ = new Version(deviceVersion);
    }

    @Override
    public List<USBInterface> interfaces() {
        return Collections.unmodifiableList(interfaces_);
    }

    @Override
    public abstract void claimInterface(int interfaceNumber);

    @Override
    public abstract void releaseInterface(int interfaceNumber);

    public void setClaimed(int interfaceNumber, boolean claimed) {
        for (var intf : interfaces_) {
            if (intf.number() == interfaceNumber) {
                ((USBInterfaceImpl) intf).setClaimed(claimed);
                return;
            }
        }
        throw new USBException("Internal error (interface not found)");
    }

    @Override
    public USBInterfaceImpl getInterface(int interfaceNumber) {
        return (USBInterfaceImpl) interfaces_.stream().filter((intf) -> intf.number() == interfaceNumber).findFirst().orElse(null);
    }

    @Override
    public USBEndpoint getEndpoint(USBDirection direction, int endpointNumber) {
        for (var intf : interfaces_) {
            for (var endpoint : intf.alternate().endpoints()) {
                if (endpoint.direction() == direction && endpoint.number() == endpointNumber)
                    return endpoint;
            }
        }
        return null;
    }

    /**
     * Checks if the specified endpoint is valid for communication and returns the endpoint.
     *
     * @param direction      transfer direction
     * @param endpointNumber endpoint number (1 to 127)
     * @param transferType1  transfer type 1
     * @param transferType2  transfer type 2 (or {@code null})
     * @return endpoint
     */
    protected EndpointInfo getEndpoint(USBDirection direction, int endpointNumber, USBTransferType transferType1,
                                       USBTransferType transferType2) {

        checkIsOpen();

        if (endpointNumber >= 1 && endpointNumber <= 127) {
            for (var intf : interfaces_) {
                if (intf.isClaimed()) {
                    for (var ep : intf.alternate().endpoints()) {
                        if (ep.number() == endpointNumber && ep.direction() == direction && (ep.transferType() == transferType1 || ep.transferType() == transferType2))
                            return new EndpointInfo(intf.number(), ep.number(),
                                    (byte) (endpointNumber | (direction == USBDirection.IN ? 0x80 : 0)),
                                    ep.packetSize(), ep.transferType());
                    }
                }
            }
        }

        throwInvalidEndpointException(direction, endpointNumber, transferType1, transferType2);
        return null; // will never be reached
    }

    protected void throwInvalidEndpointException(USBDirection direction, int endpointNumber,
                                                 USBTransferType transferType1, USBTransferType transferType2) {
        String transferTypeDesc;
        if (transferType2 == null)
            transferTypeDesc = transferType1.name();
        else
            transferTypeDesc = String.format("%s or %s", transferType1.name(), transferType2.name());
        throw new USBException(String.format("Endpoint number %d does not exist, is not part of a claimed interface " + "or is not valid for %s transfer in %s direction", endpointNumber, transferTypeDesc, direction.name()));
    }

    protected int getInterfaceNumber(USBDirection direction, int endpointNumber) {
        if (endpointNumber < 1 || endpointNumber > 127)
            return -1;

        for (var intf : interfaces_) {
            if (intf.isClaimed()) {
                for (var ep : intf.alternate().endpoints()) {
                    if (ep.number() == endpointNumber && ep.direction() == direction)
                        return intf.number();
                }
            }
        }

        return -1;
    }

    @Override
    public abstract byte[] controlTransferIn(USBControlTransfer setup, int length);

    @Override
    public abstract void controlTransferOut(USBControlTransfer setup, byte[] data);

    @Override
    public void transferOut(int endpointNumber, byte[] data) {
        transferOut(endpointNumber, data, 0);
    }

    @Override
    public abstract void transferOut(int endpointNumber, byte[] data, int timeout);

    @Override
    public byte[] transferIn(int endpointNumber) {
        return transferIn(endpointNumber, 0);
    }

    @Override
    public abstract byte[] transferIn(int endpointNumber, int timeout);

    protected void waitForTransfer(Transfer transfer, int timeout, USBDirection direction, int endpointNumber) {
        if (timeout <= 0) {
            waitNoTimeout(transfer);

        } else {
            boolean hasTimedOut = waitWithTimeout(transfer, timeout);

            // test for timeout
            if (hasTimedOut && transfer.resultCode == 0) {
                abortTransfers(direction, endpointNumber);
                waitNoTimeout(transfer);
                throw new USBTimeoutException(getOperationDescription(direction, endpointNumber) + "aborted due to " +
                        "timeout");
            }
        }

        // test for error
        if (transfer.resultCode != 0) {
            var operation = getOperationDescription(direction, endpointNumber);
            throwOSException(transfer.resultCode, operation + " failed");
        }
    }

    private static void waitNoTimeout(Transfer transfer) {
        // wait for transfer
        while (transfer.resultSize == -1) {
            try {
                transfer.wait();
            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
    }

    private static boolean waitWithTimeout(Transfer transfer, int timeout) {
        // wait for transfer to complete, or abort when timeout occurs
        long expiration = System.currentTimeMillis() + timeout;
        long remainingTimeout = timeout;
        while (remainingTimeout > 0 && transfer.resultSize == -1) {
            try {
                transfer.wait(remainingTimeout);
                remainingTimeout = expiration - System.currentTimeMillis();

            } catch (InterruptedException e) {
                // ignore and retry
            }
        }

        return remainingTimeout <= 0;
    }

    protected static String getOperationDescription(USBDirection direction, int endpointNumber) {
        if (endpointNumber == 0) {
            return "Control transfer";
        } else {
            return String.format("Transfer %s on endpoint %d", direction.name(), endpointNumber);
        }

    }

    /**
     * Create a transfer object suitable for this device.
     *
     * @return transfer object
     */
    protected abstract Transfer createTransfer();

    /**
     * Completion handler used for synchronous, blocking transfers.
     * <p>
     * Calls {@link Object#notify()} so the caller can use
     * {@link Object#wait()} to wait for completion.
     * </p>
     *
     * @param transfer the transfer that has completed
     */
    protected static void onSyncTransferCompleted(Transfer transfer) {
        synchronized (transfer) {
            transfer.notify();
        }
    }

    /**
     * Throws an exception for the specified operating-specific error code.
     *
     * @param errorCode error code, operating specific
     * @param message   exception message format ({@link String#format(String, Object...)} style)
     * @param args      arguments for exception message
     */
    protected abstract void throwOSException(int errorCode, String message, Object... args);

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        USBDeviceImpl that = (USBDeviceImpl) o;
        return id_.equals(that.id_);
    }

    @Override
    public int hashCode() {
        return id_.hashCode();
    }

    @Override
    public String toString() {
        return "VID: 0x" + String.format("%04x", vendorId_) + ", PID: 0x" + String.format("%04x", productId_) + ", " + "manufacturer: " + manufacturer_ + ", product: " + product_ + ", serial: " + serialNumber_ + ", ID: " + id_;
    }

    public record EndpointInfo(int interfaceNumber, int endpointNumber, byte endpointAddress, int packetSize,
                               USBTransferType transferType) {
    }
}
