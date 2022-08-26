//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBControlTransfer;
import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.common.USBDescriptors;
import net.codecrete.usb.common.USBDeviceImpl;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.MemoryAddress.ofLong;
import static java.lang.foreign.ValueLayout.*;

public class MacosUSBDevice extends USBDeviceImpl {

    private final MemoryAddress device;
    private final int configurationValue;
    private List<InterfaceInfo> claimedInterfaces;
    private List<EndpointInfo> endpoints;

    MacosUSBDevice(Object id, USBDeviceInfo info) {
        super(id, info);

        try (var session = MemorySession.openConfined()) {

//            // get service from IO registry
//            final int service = IoKit.IORegistryEntryFromPath(0, path);
            var matching = IoKit.IORegistryEntryIDMatching((Long) id);
            if (matching == NULL)
                throw new MacosUSBException("IORegistryEntryIDMatching failed");

            // Consumes matching instance
            int service = IoKit.IOServiceGetMatchingService(IoKit.kIOMasterPortDefault, matching);
            if (service == 0)
                throw new MacosUSBException("unable to open USB device (IOServiceGetMatchingService)");
            session.addCloseAction(() -> IoKit.IOObjectRelease(service));

            // get user client interface
            device = IoKitHelper.GetInterface(service, IoKit.kIOUSBDeviceUserClientTypeID, IoKit.kIOUSBDeviceInterfaceID100);
            if (device == null)
                throw new MacosUSBException("unable to open USB device (get client interface)");

            // open device
            int ret = IoKitUSB.USBDeviceOpen(device);
            if (ret != 0) {
                IoKit.Release(device);
                throw new MacosUSBException("unable to open USB device", ret);
            }

            try {
                // retrieve information of first configuration
                var descPtrHolder = session.allocate(ADDRESS);
                ret = IoKitUSB.GetConfigurationDescriptorPtr(device, (byte) 0, descPtrHolder);
                if (ret != 0)
                    throw new MacosUSBException("failed to query first configuration", ret);

                // get value of first configuration
                var configDesc = MemorySegment.ofAddress(descPtrHolder.get(ADDRESS, 0), USBDescriptors.Configuration.byteSize(), session);
                configurationValue = 255 & (byte) USBDescriptors.Configuration_bConfigurationValue.get(configDesc);

                // set configuration
                ret = IoKitUSB.SetConfiguration(device, (byte) configurationValue);
                if (ret != 0)
                    throw new MacosUSBException("failed to set configuration", ret);

            } catch (Throwable e) {
                IoKitUSB.USBDeviceClose(device);
                IoKit.Release(device);
                throw e;
            }
        }
    }

    private InterfaceInfo findInterface(int interfaceNumber) {

        try (var outerSession = MemorySession.openConfined()) {
            var request = outerSession.allocate(IoKitUSB.IOUSBFindInterfaceRequest$Struct);
            IoKitUSB.IOUSBFindInterfaceRequest_bInterfaceClass.set(request, IoKitUSB.kIOUSBFindInterfaceDontCare);
            IoKitUSB.IOUSBFindInterfaceRequest_bInterfaceSubClass.set(request, IoKitUSB.kIOUSBFindInterfaceDontCare);
            IoKitUSB.IOUSBFindInterfaceRequest_bInterfaceProtocol.set(request, IoKitUSB.kIOUSBFindInterfaceDontCare);
            IoKitUSB.IOUSBFindInterfaceRequest_bAlternateSetting.set(request, IoKitUSB.kIOUSBFindInterfaceDontCare);

            var iterHolder = outerSession.allocate(JAVA_INT);
            int ret = IoKitUSB.CreateInterfaceIterator(device, request, iterHolder);
            final var iter = iterHolder.get(JAVA_INT, 0);
            if (ret != 0)
                throw new RuntimeException("CreateInterfaceIterator failed");
            outerSession.addCloseAction(() -> IoKit.IOObjectRelease(iter));

            int service;
            while ((service = IoKit.IOIteratorNext(iter)) != 0) {
                try (var session = MemorySession.openConfined()) {

                    final int service_final = service;
                    session.addCloseAction(() -> IoKit.IOObjectRelease(service_final));

                    final MemoryAddress intf = IoKitHelper.GetInterface(service, IoKit.kIOUSBInterfaceUserClientTypeID, IoKit.kIOUSBInterfaceInterfaceID100);
                    if (intf == null)
                        continue;

                    var intfNumberHolder = session.allocate(JAVA_INT);
                    IoKitUSB.GetInterfaceNumber(intf, intfNumberHolder);
                    if (intfNumberHolder.get(JAVA_INT, 0) != interfaceNumber) {
                        IoKit.Release(intf);
                        continue;
                    }

                    var info = new InterfaceInfo();
                    info.addr = intf.toRawLongValue();
                    info.interfaceNumber = interfaceNumber;
                    return info;
                }
            }
        }

        throw new MacosUSBException(String.format("Invalid interface number: %d", interfaceNumber));
    }

    public void claimInterface(int interfaceNumber) {
        var interfaceInfo = findInterface(interfaceNumber);

        try {
            var ret = IoKitUSB.USBInterfaceOpen(interfaceInfo.asMemoryAddress());
            if (ret != 0)
                throw new MacosUSBException("Failed to claim interface", ret);
        } catch (Throwable t) {
            IoKit.Release(interfaceInfo.asMemoryAddress());
            throw t;
        }

        if (claimedInterfaces == null)
            claimedInterfaces = new ArrayList<>();
        claimedInterfaces.add(interfaceInfo);

        updateEndpointList();
    }

    public void releaseInterface(int interfaceNumber) {
        var interfaceInfoOptional =
                claimedInterfaces.stream().filter(info -> info.interfaceNumber == interfaceNumber).findFirst();
        if (interfaceInfoOptional.isEmpty())
            throw new MacosUSBException(String.format("Invalid interface number: %d", interfaceNumber));

        var interfaceInfo = interfaceInfoOptional.get();

        int ret = IoKitUSB.USBInterfaceClose(interfaceInfo.asMemoryAddress());
        if (ret != 0)
            throw new MacosUSBException("Failed to release interface", ret);

        claimedInterfaces.remove(interfaceInfo);
        IoKit.Release(interfaceInfo.asMemoryAddress());

        updateEndpointList();
    }

    private void updateEndpointList() {
        endpoints = new ArrayList<>();

        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            try (var session = MemorySession.openConfined()) {

                var intf = interfaceInfo.asMemoryAddress();
                var numEndpointsHolder = session.allocate(JAVA_BYTE);
                int ret = IoKitUSB.GetNumEndpoints(intf, numEndpointsHolder);
                if (ret != 0)
                    throw new MacosUSBException("Failed to get number of endpoints", ret);
                int numEndpoints = numEndpointsHolder.get(JAVA_BYTE, 0) & 255;

                for (int pipeIndex = 1; pipeIndex <= numEndpoints; pipeIndex++) {

                    var directionHolder = session.allocate(JAVA_BYTE);
                    var numberHolder = session.allocate(JAVA_BYTE);
                    var transferTypeHolder = session.allocate(JAVA_BYTE);
                    var maxPacketSizeHolder = session.allocate(JAVA_SHORT);
                    var intervalHolder = session.allocate(JAVA_BYTE);

                    ret = IoKitUSB.GetPipeProperties(intf, (byte) pipeIndex, directionHolder,
                            numberHolder, transferTypeHolder, maxPacketSizeHolder, intervalHolder);
                    if (ret != 0)
                        throw new MacosUSBException("Failed to get pipe properties", ret);

                    var endpointInfo = new EndpointInfo();
                    endpointInfo.pipeIndex = pipeIndex;
                    endpointInfo.endpointNumber = numberHolder.get(JAVA_BYTE, 0) & 255;
                    int direction = directionHolder.get(JAVA_BYTE, 0) & 255;
                    endpointInfo.endpointAddress = endpointInfo.endpointNumber | (direction << 7);
                    endpointInfo.transferType = transferTypeHolder.get(JAVA_BYTE, 0) & 255;
                    endpointInfo.interfaceInfo = interfaceInfo;
                    endpoints.add(endpointInfo);
                }
            }
        }
    }

    private EndpointInfo getEndpointInfo(int endpointNumber) {
        if (endpoints != null) {
            var endpointOptional = endpoints.stream().filter(info -> info.endpointNumber == endpointNumber).findFirst();
            if (endpointOptional.isPresent())
                return endpointOptional.get();
        }

        throw new MacosUSBException(String.format("Endpoint number %d is not part of a claimed interface or invalid", endpointNumber));
    }

    private static MemorySegment createDeviceRequest(MemorySession session, USBDirection direction, USBControlTransfer setup, MemorySegment data) {
        var deviceRequest = session.allocate(IoKitUSB.IOUSBDevRequest$Struct);
        var bmRequestType = (direction == USBDirection.IN ? 0x80 : 0x00) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        IoKitUSB.IOUSBDevRequest_bmRequestType.set(deviceRequest, (byte) bmRequestType);
        IoKitUSB.IOUSBDevRequest_bRequest.set(deviceRequest, setup.request());
        IoKitUSB.IOUSBDevRequest_wValue.set(deviceRequest, setup.value());
        IoKitUSB.IOUSBDevRequest_wIndex.set(deviceRequest, setup.index());
        IoKitUSB.IOUSBDevRequest_wLength.set(deviceRequest, (short) data.byteSize());
        IoKitUSB.IOUSBDevRequest_pData.set(deviceRequest, data.address());
        return deviceRequest;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var session = MemorySession.openConfined()) {
            var data = session.allocate(length);
            var deviceRequest = createDeviceRequest(session, USBDirection.IN, setup, data);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest);
            if (ret != 0)
                throw new MacosUSBException("Control IN transfer failed", ret);

            int lenDone = (int) IoKitUSB.IOUSBDevRequest_wLenDone.get(deviceRequest);
            return data.asSlice(0, lenDone).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var session = MemorySession.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var dataSegment = session.allocate(dataLength);
            if (dataLength > 0)
                dataSegment.copyFrom(MemorySegment.ofArray(data));
            var deviceRequest = createDeviceRequest(session, USBDirection.OUT, setup, dataSegment);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest);
            if (ret != 0)
                throw new MacosUSBException("Control IN transfer failed", ret);
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data) {

        var endpointInfo = getEndpointInfo(endpointNumber);

        try (var session = MemorySession.openConfined()) {
            var nativeData = session.allocateArray(JAVA_BYTE, data.length);
            nativeData.copyFrom(MemorySegment.ofArray(data));
            int ret = IoKitUSB.WritePipe(endpointInfo.interfaceInfo.asMemoryAddress(), (byte) endpointInfo.pipeIndex,
                    nativeData, data.length);
            if (ret != 0)
                throw new MacosUSBException(String.format("Sending data to endpoint %d failed", endpointNumber), ret);
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength) {
        var endpointInfo = getEndpointInfo(endpointNumber);

        try (var session = MemorySession.openConfined()) {
            var nativeData = session.allocateArray(JAVA_BYTE, maxLength);
            var sizeHolder = session.allocate(JAVA_INT, maxLength);
            int ret = IoKitUSB.ReadPipe(endpointInfo.interfaceInfo.asMemoryAddress(), (byte) endpointInfo.pipeIndex,
                    nativeData, sizeHolder);
            if (ret != 0)
                throw new MacosUSBException(String.format("Receiving data from endpoint %d failed", endpointNumber), ret);

            int size = sizeHolder.get(JAVA_INT, 0);
            var result = new byte[size];
            var resultSegment = MemorySegment.ofArray(result);
            resultSegment.copyFrom(nativeData.asSlice(0, size));

            return result;
        }
    }

    @Override
    public void close() throws Exception {
        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            IoKitUSB.USBInterfaceClose(interfaceInfo.asMemoryAddress());
            IoKit.Release(interfaceInfo.asMemoryAddress());
        }

        IoKitUSB.USBDeviceClose(device);
        IoKit.Release(device);
    }

    static class InterfaceInfo {
        long addr;
        int interfaceNumber;

        MemoryAddress asMemoryAddress() {
            return ofLong(addr);
        }
    }

    static class EndpointInfo {
        int endpointNumber;
        int endpointAddress;
        int transferType;
        int pipeIndex;
        InterfaceInfo interfaceInfo;
    }
}
