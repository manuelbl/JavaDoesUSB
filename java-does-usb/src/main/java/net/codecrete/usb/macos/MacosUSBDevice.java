//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBControlTransfer;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBTimeoutException;
import net.codecrete.usb.USBTransferType;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.iokit.IOUSBDevRequest;
import net.codecrete.usb.macos.gen.iokit.IOUSBFindInterfaceRequest;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.common.ForeignMemory.UNBOUNDED_ADDRESS;
import static net.codecrete.usb.macos.MacosUSBException.throwException;

public class MacosUSBDevice extends USBDeviceImpl {

    private final MemorySegment device;
    private int configurationValue;
    private List<InterfaceInfo> claimedInterfaces;
    private Map<Byte, EndpointInfo> endpoints;

    MacosUSBDevice(MemorySegment device, Object id, int vendorId, int productId) {
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
            throwException("the device is already open");

        // open device
        int ret = IoKitUSB.USBDeviceOpen(device);
        if (ret != 0)
            throwException(ret, "unable to open USB device");

        // set configuration
        ret = IoKitUSB.SetConfiguration(device, (byte) configurationValue);
        if (ret != 0)
            throwException(ret, "failed to set configuration");

        claimedInterfaces = new ArrayList<>();
        updateEndpointList();
    }

    @Override
    public void close() {
        if (!isOpen())
            return;

        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            IoKitUSB.USBInterfaceClose(interfaceInfo.segment());
            IoKitUSB.Release(interfaceInfo.segment());
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
        try (var arena = Arena.openConfined()) {

            configurationValue = 0;

            // retrieve information of first configuration
            var descPtrHolder = arena.allocate(ADDRESS);
            int ret = IoKitUSB.GetConfigurationDescriptorPtr(device, (byte) 0, descPtrHolder);
            if (ret != 0)
                throwException(ret, "failed to query first configuration");

            // get value of first configuration
            var configDesc = descPtrHolder.get(UNBOUNDED_ADDRESS, 0);
            var configDescHeader = new ConfigurationDescriptor(configDesc);
            int totalLength = configDescHeader.totalLength();
            configDesc = configDesc.asSlice(0, totalLength);

            var configuration = setConfigurationDescriptor(configDesc);
            configurationValue = 255 & configuration.configValue();
        }
    }

    private InterfaceInfo findInterface(int interfaceNumber) {

        try (var outerArena = Arena.openConfined(); var outerCleanup = new ScopeCleanup()) {
            var request = outerArena.allocate(IOUSBFindInterfaceRequest.$LAYOUT());
            IOUSBFindInterfaceRequest.bInterfaceClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceSubClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceProtocol$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bAlternateSetting$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());

            var iterHolder = outerArena.allocate(JAVA_INT);
            int ret = IoKitUSB.CreateInterfaceIterator(device, request, iterHolder);
            final var iter = iterHolder.get(JAVA_INT, 0);
            if (ret != 0)
                throwException("CreateInterfaceIterator failed");
            outerCleanup.add(() -> IOKit.IOObjectRelease(iter));

            int service;
            while ((service = IOKit.IOIteratorNext(iter)) != 0) {
                try (var arena = Arena.openConfined(); var cleanup = new ScopeCleanup()) {

                    final int service_final = service;
                    cleanup.add(() -> IOKit.IOObjectRelease(service_final));

                    final var intf = IoKitHelper.getInterface(service,
                            IoKitHelper.kIOUSBInterfaceUserClientTypeID, IoKitHelper.kIOUSBInterfaceInterfaceID100);
                    if (intf == null) continue;

                    var intfNumberHolder = arena.allocate(JAVA_INT);
                    IoKitUSB.GetInterfaceNumber(intf, intfNumberHolder);
                    if (intfNumberHolder.get(JAVA_INT, 0) != interfaceNumber) {
                        IoKitUSB.Release(intf);
                        continue;
                    }

                    return new InterfaceInfo(intf, interfaceNumber);
                }
            }
        }

        throwException("Invalid interface number: %d", interfaceNumber);
        throw new AssertionError("not reached");
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var interfaceInfo = findInterface(interfaceNumber);

        try {
            var ret = IoKitUSB.USBInterfaceOpen(interfaceInfo.segment());
            if (ret != 0)
                throwException(ret, "Failed to claim interface");
            setClaimed(interfaceNumber, true);

        } catch (Throwable t) {
            IoKitUSB.Release(interfaceInfo.segment());
            throw t;
        }

        claimedInterfaces.add(interfaceInfo);

        updateEndpointList();
    }

    public void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        // check interface
        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throwException("Interface %d does not exist", interfaceNumber);
        if (!intf.isClaimed())
            throwException("Interface %d has not been claimed", interfaceNumber);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        if (altSetting == null)
            throwException("Interface %d does not have an alternate interface setting %d", interfaceNumber, alternateNumber);

        var intfInfo = claimedInterfaces.stream()
                .filter((interf) -> interf.interfaceNumber() == interfaceNumber).findFirst().get();

        int ret = IoKitUSB.SetAlternateInterface(intfInfo.segment(), (byte) alternateNumber);
        if (ret != 0)
            throwException(ret, "Failed to set alternate interface");

        intf.setAlternate(altSetting);
        updateEndpointList();
    }

    public void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var interfaceInfoOptional =
                claimedInterfaces.stream().filter(info -> info.interfaceNumber == interfaceNumber).findFirst();
        if (interfaceInfoOptional.isEmpty())
            throwException("Invalid interface number: %d", interfaceNumber);

        var interfaceInfo = interfaceInfoOptional.get();

        int ret = IoKitUSB.USBInterfaceClose(interfaceInfo.segment());
        if (ret != 0)
            throwException(ret, "Failed to release interface");

        claimedInterfaces.remove(interfaceInfo);
        IoKitUSB.Release(interfaceInfo.segment());
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
            try (var arena = Arena.openConfined()) {

                var intf = interfaceInfo.segment();
                var numEndpointsHolder = arena.allocate(JAVA_BYTE);
                int ret = IoKitUSB.GetNumEndpoints(intf, numEndpointsHolder);
                if (ret != 0)
                    throwException(ret, "Failed to get number of endpoints");
                int numEndpoints = numEndpointsHolder.get(JAVA_BYTE, 0) & 255;

                for (int pipeIndex = 1; pipeIndex <= numEndpoints; pipeIndex++) {

                    var directionHolder = arena.allocate(JAVA_BYTE);
                    var numberHolder = arena.allocate(JAVA_BYTE);
                    var transferTypeHolder = arena.allocate(JAVA_BYTE);
                    var maxPacketSizeHolder = arena.allocate(JAVA_SHORT);
                    var intervalHolder = arena.allocate(JAVA_BYTE);

                    ret = IoKitUSB.GetPipeProperties(intf, (byte) pipeIndex, directionHolder, numberHolder,
                            transferTypeHolder, maxPacketSizeHolder, intervalHolder);
                    if (ret != 0)
                        throwException(ret, "Failed to get pipe properties");

                    int endpointNumber = numberHolder.get(JAVA_BYTE, 0) & 0xff;
                    int direction = directionHolder.get(JAVA_BYTE, 0) & 0xff;
                    byte endpointAddress = (byte) (endpointNumber | (direction << 7));
                    byte transferType = transferTypeHolder.get(JAVA_BYTE, 0);
                    int maxPacketSize = maxPacketSizeHolder.get(JAVA_SHORT, 0) & 0xffff;
                    var endpointInfo = new EndpointInfo(interfaceInfo.segment(), (byte) pipeIndex,
                            getTransferType(transferType), maxPacketSize);
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

        throwException(
                "Endpoint number %d does not exist, is not part of a claimed interface  or is not valid for %s transfer in %s direction",
                endpointNumber, transferTypeDesc, direction.name());
        throw new AssertionError("not reached");
    }

    private static MemorySegment createDeviceRequest(Arena arena, USBDirection direction,
                                                     USBControlTransfer setup, MemorySegment data) {
        var deviceRequest = arena.allocate(IOUSBDevRequest.$LAYOUT());
        var bmRequestType =
                (direction == USBDirection.IN ? 0x80 : 0x00) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        IOUSBDevRequest.bmRequestType$set(deviceRequest, (byte) bmRequestType);
        IOUSBDevRequest.bRequest$set(deviceRequest, (byte) setup.request());
        IOUSBDevRequest.wValue$set(deviceRequest, (short) setup.value());
        IOUSBDevRequest.wIndex$set(deviceRequest, (short) setup.index());
        IOUSBDevRequest.wLength$set(deviceRequest, (short) data.byteSize());
        IOUSBDevRequest.pData$set(deviceRequest, data);
        return deviceRequest;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        checkIsOpen();

        try (var arena = Arena.openConfined()) {
            var data = arena.allocate(length);
            var deviceRequest = createDeviceRequest(arena, USBDirection.IN, setup, data);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest);
            if (ret != 0)
                throwException(ret, "Control IN transfer failed");

            int lenDone = IOUSBDevRequest.wLenDone$get(deviceRequest);
            return data.asSlice(0, lenDone).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        checkIsOpen();

        try (var arena = Arena.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var dataSegment = arena.allocate(dataLength);
            if (dataLength > 0) dataSegment.copyFrom(MemorySegment.ofArray(data));
            var deviceRequest = createDeviceRequest(arena, USBDirection.OUT, setup, dataSegment);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest);
            if (ret != 0)
                throwException(ret, "Control IN transfer failed");
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {

        var endpointInfo = getEndpointInfo(endpointNumber, USBDirection.OUT,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var nativeData = arena.allocateArray(JAVA_BYTE, data.length);
            nativeData.copyFrom(MemorySegment.ofArray(data));
            int ret;

            if (timeout <= 0) {
                // transfer without timeout
                ret = IoKitUSB.WritePipe(endpointInfo.segment(), endpointInfo.pipeIndex, nativeData, data.length);

            } else if (endpointInfo.transferType == USBTransferType.BULK) {

                // bulk transfer with timeout
                ret = IoKitUSB.WritePipeTO(endpointInfo.segment(), endpointInfo.pipeIndex, nativeData,
                        data.length, timeout, timeout);
                if (ret == IOKit.kIOUSBTransactionTimeout())
                    throw new USBTimeoutException("Transfer out aborted due to timeout");

            } else {

                // interrupt transfer with timeout
                var transferTimeout = new TransferTimeout(endpointInfo, timeout);
                ret = IoKitUSB.WritePipe(endpointInfo.segment(), endpointInfo.pipeIndex, nativeData, data.length);
                if (ret == IOKit.kIOReturnAborted())
                    throw new USBTimeoutException("Transfer out aborted due to timeout");
                transferTimeout.markCompleted();
            }

            if (ret != 0)
                throwException(ret, "Sending data to endpoint %d failed", endpointNumber);
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int timeout) {

        var endpoint = getEndpointInfo(endpointNumber, USBDirection.IN,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var nativeData = arena.allocateArray(JAVA_BYTE, endpoint.packetSize());
            var sizeHolder = arena.allocate(JAVA_INT, endpoint.packetSize());
            int ret;

            if (timeout <= 0) {
                // transfer without timeout
                ret = IoKitUSB.ReadPipe(endpoint.segment(), endpoint.pipeIndex, nativeData, sizeHolder);

            } else if (endpoint.transferType == USBTransferType.BULK) {

                // bulk transfer with timeout
                ret = IoKitUSB.ReadPipeTO(endpoint.segment(), endpoint.pipeIndex, nativeData,
                        sizeHolder, timeout, timeout);
                if (ret == IOKit.kIOUSBTransactionTimeout())
                    throw new USBTimeoutException("Transfer in aborted due to timeout");

            } else {

                // interrupt transfer with timeout
                var transferTimeout = new TransferTimeout(endpoint, timeout);
                ret = IoKitUSB.ReadPipe(endpoint.segment(), endpoint.pipeIndex, nativeData, sizeHolder);
                if (ret == IOKit.kIOReturnAborted())
                    throw new USBTimeoutException("Transfer in aborted due to timeout");
                transferTimeout.markCompleted();
            }
            if (ret != 0)
                throwException(ret, "Receiving data from endpoint %d failed", endpointNumber);

            int size = sizeHolder.get(JAVA_INT, 0);
            var result = new byte[size];
            var resultSegment = MemorySegment.ofArray(result);
            resultSegment.copyFrom(nativeData.asSlice(0, size));

            return result;
        }
    }

    @Override
    public void clearHalt(USBDirection direction, int endpointNumber) {
        var endpointInfo = getEndpointInfo(endpointNumber, direction,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        int ret = IoKitUSB.ClearPipeStallBothEnds(endpointInfo.segment(), endpointInfo.pipeIndex);
        if (ret != 0)
            throwException(ret, "Clearing halt condition failed");
    }

    private static USBTransferType getTransferType(byte macosTransferType) {
        return switch (macosTransferType) {
            case 1 -> USBTransferType.ISOCHRONOUS;
            case 2 -> USBTransferType.BULK;
            case 3 -> USBTransferType.INTERRUPT;
            default -> null;
        };
    }

    record InterfaceInfo(MemorySegment segment, int interfaceNumber) {
    }

    record EndpointInfo(MemorySegment segment, byte pipeIndex, USBTransferType transferType, int packetSize) {
    }
}
