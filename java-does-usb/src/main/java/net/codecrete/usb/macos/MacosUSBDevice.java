//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.*;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.iokit.IOUSBDevRequest;
import net.codecrete.usb.macos.gen.iokit.IOUSBFindInterfaceRequest;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.foreign.MemoryAddress.ofLong;
import static java.lang.foreign.ValueLayout.*;

public class MacosUSBDevice extends USBDeviceImpl {

    private final MemoryAddress device;
    private int configurationValue;
    private List<InterfaceInfo> claimedInterfaces;
    private Map<Byte, EndpointInfo> endpoints;

    private ScheduledExecutorService scheduledExecutorService;

    MacosUSBDevice(MemoryAddress device, Object id, int vendorId, int productId) {
        super(id, vendorId, productId);
        this.device = device;

        loadDescription();

        IoKitUSB.AddRef(device);
    }

    @Override
    public boolean isOpen() {
        return claimedInterfaces != null;
    }

    @Override
    public void open() {
        if (isOpen())
            throw new USBException("the device is already open");

        // open device
        int ret = IoKitUSB.USBDeviceOpen(device);
        if (ret != 0)
            throw new MacosUSBException("unable to open USB device", ret);

        // set configuration
        ret = IoKitUSB.SetConfiguration(device, (byte) configurationValue);
        if (ret != 0)
            throw new MacosUSBException("failed to set configuration", ret);

        claimedInterfaces = new ArrayList<>();
        updateEndpointList();
    }

    @Override
    public void close() {
        if (!isOpen())
            return;

        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            IoKitUSB.USBInterfaceClose(interfaceInfo.asAddress());
            IoKitUSB.Release(interfaceInfo.asAddress());
            setClaimed(interfaceInfo.interfaceNumber, false);
        }

        claimedInterfaces = null;
        endpoints = null;
        IoKitUSB.USBDeviceClose(device);
    }

    void closeFully() {
        close();
        IoKitUSB.Release(device);
    }

    private void loadDescription() {
        try (var session = MemorySession.openConfined()) {

            try {
                // retrieve information of first configuration
                var descPtrHolder = session.allocate(ADDRESS);
                int ret = IoKitUSB.GetConfigurationDescriptorPtr(device, (byte) 0, descPtrHolder.address());
                if (ret != 0)
                    throw new MacosUSBException("failed to query first configuration", ret);

                // get value of first configuration
                var configDescHeader = new ConfigurationDescriptor(MemorySegment.ofAddress(descPtrHolder.get(ADDRESS, 0),
                        ConfigurationDescriptor.LAYOUT.byteSize(), session));
                int totalLength = configDescHeader.totalLength();
                var configDesc = MemorySegment.ofAddress(descPtrHolder.get(ADDRESS, 0),
                        totalLength, session);

                var configuration = setConfigurationDescriptor(configDesc);
                configurationValue = 255 & configuration.configValue();

            } catch (Throwable e) {
                configurationValue = 0;
                throw e;
            }
        }
    }

    private InterfaceInfo findInterface(int interfaceNumber) {

        try (var outerSession = MemorySession.openConfined()) {
            var request = outerSession.allocate(IOUSBFindInterfaceRequest.$LAYOUT());
            IOUSBFindInterfaceRequest.bInterfaceClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceSubClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceProtocol$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bAlternateSetting$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());

            var iterHolder = outerSession.allocate(JAVA_INT);
            int ret = IoKitUSB.CreateInterfaceIterator(device, request.address(), iterHolder.address());
            final var iter = iterHolder.get(JAVA_INT, 0);
            if (ret != 0) throw new RuntimeException("CreateInterfaceIterator failed");
            outerSession.addCloseAction(() -> IOKit.IOObjectRelease(iter));

            int service;
            while ((service = IOKit.IOIteratorNext(iter)) != 0) {
                try (var session = MemorySession.openConfined()) {

                    final int service_final = service;
                    session.addCloseAction(() -> IOKit.IOObjectRelease(service_final));

                    final MemoryAddress intf = IoKitHelper.getInterface(service,
                            IoKitHelper.kIOUSBInterfaceUserClientTypeID, IoKitHelper.kIOUSBInterfaceInterfaceID100);
                    if (intf == null) continue;

                    var intfNumberHolder = session.allocate(JAVA_INT);
                    IoKitUSB.GetInterfaceNumber(intf, intfNumberHolder.address());
                    if (intfNumberHolder.get(JAVA_INT, 0) != interfaceNumber) {
                        IoKitUSB.Release(intf);
                        continue;
                    }

                    return new InterfaceInfo(intf.toRawLongValue(), interfaceNumber);
                }
            }
        }

        throw new MacosUSBException(String.format("Invalid interface number: %d", interfaceNumber));
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var interfaceInfo = findInterface(interfaceNumber);

        try {
            var ret = IoKitUSB.USBInterfaceOpen(interfaceInfo.asAddress());
            if (ret != 0)
                throw new MacosUSBException("Failed to claim interface", ret);
            setClaimed(interfaceNumber, true);

        } catch (Throwable t) {
            IoKitUSB.Release(interfaceInfo.asAddress());
            throw t;
        }

        claimedInterfaces.add(interfaceInfo);

        updateEndpointList();
    }

    public void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        // check interface
        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throw new MacosUSBException(String.format("Interface %d does not exist", interfaceNumber));
        if (!intf.isClaimed())
            throw new MacosUSBException(String.format("Interface %d has not been claimed", interfaceNumber));

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        if (altSetting == null)
            throw new MacosUSBException(String.format("Interface %d does not have an alternate interface setting %d",
                    interfaceNumber, alternateNumber));

        var intfInfo = claimedInterfaces.stream()
                .filter((interf) -> interf.interfaceNumber() == interfaceNumber).findFirst().get();

        int ret = IoKitUSB.SetAlternateInterface(intfInfo.asAddress(), (byte) alternateNumber);
        if (ret != 0)
            throw new MacosUSBException("Failed to set alternate interface", ret);

        intf.setAlternate(altSetting);
        updateEndpointList();
    }

    public void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var interfaceInfoOptional =
                claimedInterfaces.stream().filter(info -> info.interfaceNumber == interfaceNumber).findFirst();
        if (interfaceInfoOptional.isEmpty())
            throw new MacosUSBException(String.format("Invalid interface number: %d", interfaceNumber));

        var interfaceInfo = interfaceInfoOptional.get();

        int ret = IoKitUSB.USBInterfaceClose(interfaceInfo.asAddress());
        if (ret != 0)
            throw new MacosUSBException("Failed to release interface", ret);

        claimedInterfaces.remove(interfaceInfo);
        IoKitUSB.Release(interfaceInfo.asAddress());
        setClaimed(interfaceNumber, false);

        updateEndpointList();
    }

    /**
     * Update the map of active endpoints.
     * <p>
     * MacOS uses a <i>pipe index</i> to refer to endpoints. This method
     * builds a map from endpoint address to pipe index.
     * </p>
     */
    private void updateEndpointList() {
        endpoints = new HashMap<>();

        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            try (var session = MemorySession.openConfined()) {

                var intf = interfaceInfo.asAddress();
                var numEndpointsHolder = session.allocate(JAVA_BYTE);
                int ret = IoKitUSB.GetNumEndpoints(intf, numEndpointsHolder.address());
                if (ret != 0)
                    throw new MacosUSBException("Failed to get number of endpoints", ret);
                int numEndpoints = numEndpointsHolder.get(JAVA_BYTE, 0) & 255;

                for (int pipeIndex = 1; pipeIndex <= numEndpoints; pipeIndex++) {

                    var directionHolder = session.allocate(JAVA_BYTE);
                    var numberHolder = session.allocate(JAVA_BYTE);
                    var transferTypeHolder = session.allocate(JAVA_BYTE);
                    var maxPacketSizeHolder = session.allocate(JAVA_SHORT);
                    var intervalHolder = session.allocate(JAVA_BYTE);

                    ret = IoKitUSB.GetPipeProperties(intf, (byte) pipeIndex, directionHolder.address(),
                            numberHolder.address(), transferTypeHolder.address(), maxPacketSizeHolder.address(),
                            intervalHolder.address());
                    if (ret != 0) throw new MacosUSBException("Failed to get pipe properties", ret);

                    int endpointNumber = numberHolder.get(JAVA_BYTE, 0) & 255;
                    int direction = directionHolder.get(JAVA_BYTE, 0) & 255;
                    byte endpointAddress = (byte) (endpointNumber | (direction << 7));
                    byte transferType = transferTypeHolder.get(JAVA_BYTE, 0);
                    var endpointInfo = new EndpointInfo(interfaceInfo.addr, (byte) pipeIndex, getTransferType(transferType));
                    endpoints.put(endpointAddress, endpointInfo);
                }
            }
        }
    }

    private EndpointInfo getEndpointInfo(int endpointNumber, USBDirection direction,
                                         USBTransferType transferType1, USBTransferType transferType2) {
        if (endpoints != null) {
            byte endpointAddress = (byte) (endpointNumber | (direction == USBDirection.IN ? 0x80 : 0));
            var endpointInfo = endpoints.get(endpointAddress);
            if (endpointInfo != null
                    && (endpointInfo.transferType == transferType1 || endpointInfo.transferType == transferType2))
                return endpointInfo;
        }

        String transferTypeDesc;
        if (transferType2 == null)
            transferTypeDesc = transferType1.name();
        else
            transferTypeDesc = String.format("%s or %s", transferType1.name(), transferType2.name());
        throw new USBException(String.format("Endpoint number %d does not exist, is not part of a claimed interface " +
                        "or is not valid for %s transfer in %s direction", endpointNumber, transferTypeDesc,
                direction.name()));
    }

    private static MemorySegment createDeviceRequest(MemorySession session, USBDirection direction,
                                                     USBControlTransfer setup, MemorySegment data) {
        var deviceRequest = session.allocate(IOUSBDevRequest.$LAYOUT());
        var bmRequestType =
                (direction == USBDirection.IN ? 0x80 : 0x00) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        IOUSBDevRequest.bmRequestType$set(deviceRequest, (byte) bmRequestType);
        IOUSBDevRequest.bRequest$set(deviceRequest, (byte) setup.request());
        IOUSBDevRequest.wValue$set(deviceRequest, (short) setup.value());
        IOUSBDevRequest.wIndex$set(deviceRequest, (short) setup.index());
        IOUSBDevRequest.wLength$set(deviceRequest, (short) data.byteSize());
        IOUSBDevRequest.pData$set(deviceRequest, data.address());
        return deviceRequest;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        checkIsOpen();

        try (var session = MemorySession.openConfined()) {
            var data = session.allocate(length);
            var deviceRequest = createDeviceRequest(session, USBDirection.IN, setup, data);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest.address());
            if (ret != 0) throw new MacosUSBException("Control IN transfer failed", ret);

            int lenDone = IOUSBDevRequest.wLenDone$get(deviceRequest);
            return data.asSlice(0, lenDone).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        checkIsOpen();

        try (var session = MemorySession.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var dataSegment = session.allocate(dataLength);
            if (dataLength > 0) dataSegment.copyFrom(MemorySegment.ofArray(data));
            var deviceRequest = createDeviceRequest(session, USBDirection.OUT, setup, dataSegment);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest.address());
            if (ret != 0) throw new MacosUSBException("Control IN transfer failed", ret);
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {

        var endpointInfo = getEndpointInfo(endpointNumber, USBDirection.OUT,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var session = MemorySession.openConfined()) {
            var nativeData = session.allocateArray(JAVA_BYTE, data.length);
            nativeData.copyFrom(MemorySegment.ofArray(data));
            int ret;

            if (timeout <= 0) {
                // transfer without timeout
                ret = IoKitUSB.WritePipe(endpointInfo.interfacAddress(), endpointInfo.pipeIndex, nativeData.address(), data.length);

            } else if (endpointInfo.transferType == USBTransferType.BULK) {

                // bulk transfer with timeout
                ret = IoKitUSB.WritePipeTO(endpointInfo.interfacAddress(), endpointInfo.pipeIndex, nativeData.address(),
                        data.length, timeout, timeout);
                if (ret == IOKit.kIOUSBTransactionTimeout())
                    throw new TimeoutException("Transfer out aborted due to timeout");

            } else {

                // interrupt transfer with timeout
                var transferTimeout = new TransferTimeout(endpointInfo, timeout);
                ret = IoKitUSB.WritePipe(endpointInfo.interfacAddress(), endpointInfo.pipeIndex, nativeData.address(), data.length);
                if (ret == IOKit.kIOReturnAborted())
                    throw new TimeoutException("Transfer out aborted due to timeout");
                transferTimeout.markCompleted();
            }

            if (ret != 0)
                throw new MacosUSBException(String.format("Sending data to endpoint %d failed", endpointNumber), ret);
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength, int timeout) {

        var endpointInfo = getEndpointInfo(endpointNumber, USBDirection.IN,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var session = MemorySession.openConfined()) {
            var nativeData = session.allocateArray(JAVA_BYTE, maxLength);
            var sizeHolder = session.allocate(JAVA_INT, maxLength);
            int ret;

            if (timeout <= 0) {
                // transfer without timeout
                ret = IoKitUSB.ReadPipe(endpointInfo.interfacAddress(), endpointInfo.pipeIndex, nativeData.address(), sizeHolder.address());

            } else if (endpointInfo.transferType == USBTransferType.BULK) {

                // bulk transfer with timeout
                ret = IoKitUSB.ReadPipeTO(endpointInfo.interfacAddress(), endpointInfo.pipeIndex, nativeData.address(),
                        sizeHolder.address(), timeout, timeout);
                if (ret == IOKit.kIOUSBTransactionTimeout())
                    throw new TimeoutException("Transfer in aborted due to timeout");

            } else {

                // interrupt transfer with timeout
                var transferTimeout = new TransferTimeout(endpointInfo, timeout);
                ret = IoKitUSB.ReadPipe(endpointInfo.interfacAddress(), endpointInfo.pipeIndex, nativeData.address(), sizeHolder.address());
                if (ret == IOKit.kIOReturnAborted())
                    throw new TimeoutException("Transfer in aborted due to timeout");
                transferTimeout.markCompleted();
            }
            if (ret != 0)
                throw new MacosUSBException(String.format("Receiving data from endpoint %d failed", endpointNumber),
                        ret);

            int size = sizeHolder.get(JAVA_INT, 0);
            var result = new byte[size];
            var resultSegment = MemorySegment.ofArray(result);
            resultSegment.copyFrom(nativeData.asSlice(0, size));

            return result;
        }
    }

    synchronized ScheduledExecutorService getScheduledExecutorService() {
        if (scheduledExecutorService == null)
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
        return scheduledExecutorService;
    }

    private static USBTransferType getTransferType(byte macosTransferType) {
        return switch (macosTransferType) {
            case 1 -> USBTransferType.ISOCHRONOUS;
            case 2 -> USBTransferType.BULK;
            case 3 -> USBTransferType.INTERRUPT;
            default -> null;
        };
    }

    record InterfaceInfo(long addr, int interfaceNumber) {
        MemoryAddress asAddress() {
            return ofLong(addr);
        }
    }

    record EndpointInfo(long interfaceAddr, byte pipeIndex, USBTransferType transferType) {
        MemoryAddress interfacAddress() {
            return ofLong(interfaceAddr);
        }
    }

    /**
     * Manages the timeout for a USB transfer.
     * <p>
     * Not used for bulk transfers as IOKit offers suitable functions ({@code WritePipeTO()}, {@code ReadPipeTO()}).
     * </p>
     */
    class TransferTimeout {
        /**
         * Schedules a timeout for the specified endpoint.
         * <p>
         * If the timeout expires, the transfer is aborted.
         * </p>
         * @param endpointInfo endpoint information
         * @param timeout timeout, in milliseconds
         */
        TransferTimeout(EndpointInfo endpointInfo, int timeout) {
            this.endpointInfo = endpointInfo;
            future = getScheduledExecutorService().schedule(this::abort, timeout, TimeUnit.MILLISECONDS);
        }

        private synchronized void abort() {
            if (completed)
                return;
            int ret = IoKitUSB.AbortPipe(endpointInfo.interfacAddress(), endpointInfo.pipeIndex());
            if (ret != 0)
                throw new MacosUSBException("Failed to abort USB transfer after timeout", ret);
        }

        /**
         * Marks the transfer as completed and cancels the timeout.
         */
        synchronized void markCompleted() {
            if (completed)
                return;
            completed = true;
            future.cancel(false);
        }

        ScheduledFuture<?> future;
        EndpointInfo endpointInfo;
        boolean completed;
    }
}
