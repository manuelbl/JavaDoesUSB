//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbDevice;
import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbEndpoint;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbInterface;
import net.codecrete.usb.UsbTimeoutException;
import net.codecrete.usb.UsbTransferType;
import net.codecrete.usb.Version;
import net.codecrete.usb.usbstandard.DeviceDescriptor;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public abstract class UsbDeviceImpl implements UsbDevice {

    /**
     * Operating system-specific device ID used for {@link #equals(Object)} and {@link #hashCode()}.
     * <p>
     * Can be a String instance (such as a file path) or a Long instance (such as an internal ID).
     * It only needs to be valid for the duration the device is connected.
     * </p>
     */
    protected final Object uniqueDeviceId;

    protected List<UsbInterface> interfaceList;

    protected byte[] rawDeviceDescriptor;

    protected byte[] rawConfigurationDescriptor;

    // Information from the device descriptor
    protected final int vid;
    protected final int pid;
    protected String manufacturerString;
    protected String productString;
    protected String serialString;
    protected int deviceClass;
    protected int deviceSubclass;
    protected int deviceProtocol;
    protected Version versionUsb;
    protected Version versionDevice;

    /**
     * Creates a new instance.
     *
     * @param id        unique device ID
     * @param vendorId  USB vendor ID
     * @param productId USB product ID
     */
    protected UsbDeviceImpl(Object id, int vendorId, int productId) {

        assert id != null;

        uniqueDeviceId = id;
        vid = vendorId;
        pid = productId;
    }

    @Override
    public void detachStandardDrivers() {
        if (isOpened())
            throw new UsbException("detachStandardDrivers() must not be called while the device is open");

        // default implementation: do nothing
    }

    @Override
    public void attachStandardDrivers() {
        if (isOpened())
            throw new UsbException("attachStandardDrivers() must not be called while the device is open");

        // default implementation: do nothing
    }

    protected void checkIsOpen() {
        if (!isOpened())
            throw new UsbException("device needs to be opened first for this operation");
    }

    @Override
    public int getProductId() {
        return pid;
    }

    @Override
    public int getVendorId() {
        return vid;
    }

    @Override
    public String getProduct() {
        return productString;
    }

    @Override
    public String getManufacturer() {
        return manufacturerString;
    }

    @Override
    public String getSerialNumber() {
        return serialString;
    }

    @Override
    public int getClassCode() {
        return deviceClass;
    }

    @Override
    public int getSubclassCode() {
        return deviceSubclass;
    }

    @Override
    public int getProtocolCode() {
        return deviceProtocol;
    }

    @Override
    public @NotNull Version getUsbVersion() {
        return versionUsb;
    }

    @Override
    public @NotNull Version getDeviceVersion() {
        return versionDevice;
    }

    @Override
    public byte @NotNull [] getConfigurationDescriptor() {
        return rawConfigurationDescriptor;
    }

    @Override
    public byte @NotNull [] getDeviceDescriptor() {
        return rawDeviceDescriptor;
    }

    public Object getUniqueId() {
        return uniqueDeviceId;
    }

    /**
     * Sets the class codes and version for the device descriptor.
     *
     * @param descriptor the device descriptor
     */
    public void setFromDeviceDescriptor(MemorySegment descriptor) {
        rawDeviceDescriptor = descriptor.toArray(JAVA_BYTE);
        var deviceDescriptor = new DeviceDescriptor(descriptor);
        deviceClass = deviceDescriptor.deviceClass() & 255;
        deviceSubclass = deviceDescriptor.deviceSubClass() & 255;
        deviceProtocol = deviceDescriptor.deviceProtocol() & 255;
        versionUsb = new Version(deviceDescriptor.usbVersion());
        versionDevice = new Version(deviceDescriptor.deviceVersion());
    }

    /**
     * Sets the configuration descriptor and derives the interface and endpoint descriptions.
     *
     * @param descriptor configuration descriptor
     * @return parsed configuration
     */
    protected Configuration setConfigurationDescriptor(MemorySegment descriptor) {
        rawConfigurationDescriptor = descriptor.toArray(JAVA_BYTE);
        var configuration = ConfigurationParser.parseConfigurationDescriptor(descriptor);
        interfaceList = configuration.interfaces();
        interfaceList.sort(Comparator.comparingInt(UsbInterface::getNumber));
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
        manufacturerString = manufacturer;
        productString = product;
        serialString = serialNumber;
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
    public void setProductString(MemorySegment descriptor, IntFunction<String> stringLookup) {
        var deviceDescriptor = new DeviceDescriptor(descriptor);
        manufacturerString = stringLookup.apply(deviceDescriptor.iManufacturer());
        productString = stringLookup.apply(deviceDescriptor.iProduct());
        serialString = stringLookup.apply(deviceDescriptor.iSerialNumber());
    }

    public void setClassCodes(int classCode, int subclassCode, int protocolCode) {
        deviceClass = classCode;
        deviceSubclass = subclassCode;
        deviceProtocol = protocolCode;
    }

    public void setVersions(int usbVersion, int deviceVersion) {
        versionUsb = new Version(usbVersion);
        versionDevice = new Version(deviceVersion);
    }

    @Override
    public @NotNull List<UsbInterface> getInterfaces() {
        return Collections.unmodifiableList(interfaceList);
    }

    public void setClaimed(int interfaceNumber, boolean claimed) {
        for (var intf : interfaceList) {
            if (intf.getNumber() == interfaceNumber) {
                ((UsbInterfaceImpl) intf).setClaimed(claimed);
                return;
            }
        }
        throw new UsbException("internal error (interface not found)");
    }

    @Override
    public @NotNull UsbInterfaceImpl getInterface(int interfaceNumber) {
        return (UsbInterfaceImpl) interfaceList.stream()
                .filter(intf -> intf.getNumber() == interfaceNumber).findFirst()
                .orElseThrow(() -> new UsbException(String.format("USB device has no interface %d", interfaceNumber)));
    }

    public UsbInterfaceImpl getInterfaceWithCheck(int interfaceNumber, boolean isClaimed) {
        var intf = getInterface(interfaceNumber);
        if (isClaimed && !intf.isClaimed()) {
            throw new UsbException(String.format("interface %d must be claimed first", interfaceNumber));
        } else if (!isClaimed && intf.isClaimed()) {
            throw new UsbException(String.format("interface %d has already been claimed", interfaceNumber));
        }
        return intf;
    }

    @Override
    public @NotNull UsbEndpoint getEndpoint(UsbDirection direction, int endpointNumber) {
        for (var intf : interfaceList) {
            for (var endpoint : intf.getCurrentAlternate().getEndpoints()) {
                if (endpoint.getDirection() == direction && endpoint.getNumber() == endpointNumber)
                    return endpoint;
            }
        }
        throw new UsbException(String.format("endpoint %d (%s) does not exist", endpointNumber, direction.name()));
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
    @SuppressWarnings("java:S3776")
    protected EndpointInfo getEndpoint(UsbDirection direction, int endpointNumber, UsbTransferType transferType1,
                                       UsbTransferType transferType2) {

        checkIsOpen();

        if (endpointNumber >= 1 && endpointNumber <= 127) {
            for (var intf : interfaceList) {
                if (intf.isClaimed()) {
                    for (var ep : intf.getCurrentAlternate().getEndpoints()) {
                        if (ep.getNumber() == endpointNumber && ep.getDirection() == direction
                                && (ep.getTransferType() == transferType1 || ep.getTransferType() == transferType2))
                            return new EndpointInfo(intf.getNumber(), ep.getNumber(),
                                    (byte) (endpointNumber | (direction == UsbDirection.IN ? 0x80 : 0)),
                                    ep.getPacketSize(), ep.getTransferType());
                    }
                }
            }
        }

        throwInvalidEndpointException(direction, endpointNumber, transferType1, transferType2);
        return null; // will never be reached
    }

    protected void throwInvalidEndpointException(UsbDirection direction, int endpointNumber,
                                                 UsbTransferType transferType1, UsbTransferType transferType2) {
        String transferTypeDesc;
        if (transferType2 == null)
            transferTypeDesc = transferType1.name();
        else
            transferTypeDesc = String.format("%s or %s", transferType1.name(), transferType2.name());
        throw new UsbException(String.format(
                "endpoint number %d does not exist, is not part of a claimed interface or is not valid for %s transfer in %s direction",
                endpointNumber, transferTypeDesc, direction.name()));
    }

    protected int getInterfaceNumber(UsbDirection direction, int endpointNumber) {
        if (endpointNumber < 1 || endpointNumber > 127)
            return -1;

        for (var intf : interfaceList) {
            if (intf.isClaimed()) {
                for (var ep : intf.getCurrentAlternate().getEndpoints()) {
                    if (ep.getNumber() == endpointNumber && ep.getDirection() == direction)
                        return intf.getNumber();
                }
            }
        }

        return -1;
    }

    @Override
    public void transferOut(int endpointNumber, byte @NotNull [] data) {
        transferOut(endpointNumber, data, 0, data.length, 0);
    }

    @Override
    public void transferOut(int endpointNumber, byte @NotNull [] data, int timeout) {
        transferOut(endpointNumber, data, 0, data.length, timeout);
    }

    @Override
    public byte @NotNull [] transferIn(int endpointNumber) {
        return transferIn(endpointNumber, 0);
    }

    protected void waitForTransfer(Transfer transfer, int timeout, UsbDirection direction, int endpointNumber) {
        if (timeout <= 0) {
            waitNoTimeout(transfer);

        } else {
            var hasTimedOut = waitWithTimeout(transfer, timeout);

            // test for timeout
            if (hasTimedOut && transfer.resultCode() == 0) {
                abortTransfers(direction, endpointNumber);
                waitNoTimeout(transfer);
                throw new UsbTimeoutException(getOperationDescription(direction, endpointNumber) + " aborted due to timeout");
            }
        }

        // test for error
        if (transfer.resultCode() != 0) {
            var operation = getOperationDescription(direction, endpointNumber);
            throwOSException(transfer.resultCode(), operation + " failed");
        }
    }

    @SuppressWarnings("java:S2273")
    private static void waitNoTimeout(Transfer transfer) {
        // wait for transfer
        while (transfer.resultSize() == -1) {
            try {
                transfer.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressWarnings("java:S2273")
    private static boolean waitWithTimeout(Transfer transfer, int timeout) {
        // wait for transfer to complete, or abort when timeout occurs
        var expiration = System.currentTimeMillis() + timeout;
        long remainingTimeout = timeout;
        while (remainingTimeout > 0 && transfer.resultSize() == -1) {
            try {
                transfer.wait(remainingTimeout);
                remainingTimeout = expiration - System.currentTimeMillis();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return remainingTimeout <= 0;
    }

    protected static String getOperationDescription(UsbDirection direction, int endpointNumber) {
        if (endpointNumber == 0) {
            return "control transfer";
        } else {
            return String.format("transfer %s on endpoint %d", direction.name(), endpointNumber);
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
    @SuppressWarnings({"java:S2445", "java:S2446"})
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
        var that = (UsbDeviceImpl) o;
        return uniqueDeviceId.equals(that.uniqueDeviceId);
    }

    @Override
    public int hashCode() {
        return uniqueDeviceId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VID: 0x%04x, PID: 0x%04x, manufacturer: %s, product: %s, serial: %s, ID: %s",
                vid, pid, manufacturerString, productString, serialString, uniqueDeviceId);
    }

    public record EndpointInfo(int interfaceNumber, int endpointNumber, byte endpointAddress, int packetSize,
                               UsbTransferType transferType) {
    }
}
