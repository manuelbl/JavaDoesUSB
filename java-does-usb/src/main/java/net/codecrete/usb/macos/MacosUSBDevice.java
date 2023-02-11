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
import net.codecrete.usb.common.ForeignMemory;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.iokit.IOUSBDevRequest;
import net.codecrete.usb.macos.gen.iokit.IOUSBFindInterfaceRequest;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.macos.MacosUSBException.throwException;

public class MacosUSBDevice extends USBDeviceImpl {

    private final MacosUSBDeviceRegistry registry;
    private MemorySegment device;
    private int configurationValue;
    private List<InterfaceInfo> claimedInterfaces;
    private Map<Byte, EndpointInfo> endpoints;

    MacosUSBDevice(MacosUSBDeviceRegistry registry, MemorySegment device, Object id, int vendorId, int productId) {
        super(id, vendorId, productId);
        this.registry = registry;

        loadDescription(device);

        this.device = device;
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

        claimedInterfaces = new ArrayList<>();

        // set configuration
        ret = IoKitUSB.SetConfiguration(device, (byte) configurationValue);
        if (ret != 0)
            throwException(ret, "failed to set configuration");

        updateEndpointList();
    }

    @Override
    public void close() {
        if (!isOpen())
            return;

        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            IoKitUSB.USBInterfaceClose(interfaceInfo.iokitInterface());
            IoKitUSB.Release(interfaceInfo.iokitInterface());
            setClaimed(interfaceInfo.interfaceNumber, false);
        }

        claimedInterfaces = null;
        endpoints = null;
        IoKitUSB.USBDeviceClose(device);
    }

    void closeFully() {
        close();
        IoKitUSB.Release(device);
        device = null;
    }

    private void loadDescription(MemorySegment device) {
        try (var arena = Arena.openConfined()) {

            configurationValue = 0;

            // retrieve information of first configuration
            var descPtrHolder = arena.allocate(ADDRESS);
            int ret = IoKitUSB.GetConfigurationDescriptorPtr(device, (byte) 0, descPtrHolder);
            if (ret != 0)
                throwException(ret, "failed to query first configuration");

            var configDesc = ForeignMemory.deref(descPtrHolder, 999999);
            var configDescHeader = new ConfigurationDescriptor(configDesc);
            configDesc = configDesc.asSlice(0, configDescHeader.totalLength());

            var configuration = setConfigurationDescriptor(configDesc);
            configurationValue = 255 & configuration.configValue();
        }
    }

    private InterfaceInfo findInterface(int interfaceNumber) {

        try (var arena = Arena.openConfined(); var outerCleanup = new ScopeCleanup()) {
            var request = arena.allocate(IOUSBFindInterfaceRequest.$LAYOUT());
            IOUSBFindInterfaceRequest.bInterfaceClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceSubClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceProtocol$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bAlternateSetting$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());

            var iterHolder = arena.allocate(JAVA_INT);
            int ret = IoKitUSB.CreateInterfaceIterator(device, request, iterHolder);
            if (ret != 0)
                throwException("CreateInterfaceIterator failed");

            final var iter = iterHolder.get(JAVA_INT, 0);
            outerCleanup.add(() -> IOKit.IOObjectRelease(iter));

            var intfNumberHolder = arena.allocate(JAVA_INT);

            int service;
            while ((service = IOKit.IOIteratorNext(iter)) != 0) {
                try (var cleanup = new ScopeCleanup()) {

                    final int service_final = service;
                    cleanup.add(() -> IOKit.IOObjectRelease(service_final));

                    final var intf = IoKitHelper.getInterface(service,
                            IoKitHelper.kIOUSBInterfaceUserClientTypeID, IoKitHelper.kIOUSBInterfaceInterfaceID100);
                    if (intf == null)
                        continue;

                    cleanup.add(() -> IoKitUSB.Release(intf));

                    IoKitUSB.GetInterfaceNumber(intf, intfNumberHolder);
                    if (intfNumberHolder.get(JAVA_INT, 0) != interfaceNumber)
                        continue;

                    IoKitUSB.AddRef(intf);
                    return new InterfaceInfo(intf, interfaceNumber);
                }
            }
        }

        throwException("Invalid interface number: %d", interfaceNumber);
        throw new AssertionError("not reached");
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        try (var cleanup = new ScopeCleanup()) {

            var interfaceInfo = findInterface(interfaceNumber);
            cleanup.add(() -> IoKitUSB.Release(interfaceInfo.iokitInterface()));

            var ret = IoKitUSB.USBInterfaceOpen(interfaceInfo.iokitInterface());
            if (ret != 0)
                throwException(ret, "Failed to claim interface");

            IoKitUSB.AddRef(interfaceInfo.iokitInterface());
            claimedInterfaces.add(interfaceInfo);
            setClaimed(interfaceNumber, true);
        }

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

        int ret = IoKitUSB.SetAlternateInterface(intfInfo.iokitInterface(), (byte) alternateNumber);
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

        var source = IoKitUSB.GetInterfaceAsyncEventSource(interfaceInfo.iokitInterface());
        if (source.address() != 0)
            registry.removeEventSource(source);

        int ret = IoKitUSB.USBInterfaceClose(interfaceInfo.iokitInterface());
        if (ret != 0)
            throwException(ret, "Failed to release interface");

        claimedInterfaces.remove(interfaceInfo);
        IoKitUSB.Release(interfaceInfo.iokitInterface());
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

        try (var arena = Arena.openConfined()) {

            var directionHolder = arena.allocate(JAVA_BYTE);
            var numberHolder = arena.allocate(JAVA_BYTE);
            var transferTypeHolder = arena.allocate(JAVA_BYTE);
            var maxPacketSizeHolder = arena.allocate(JAVA_SHORT);
            var intervalHolder = arena.allocate(JAVA_BYTE);

            for (InterfaceInfo interfaceInfo : claimedInterfaces) {

                var intf = interfaceInfo.iokitInterface();
                var numEndpointsHolder = arena.allocate(JAVA_BYTE);
                int ret = IoKitUSB.GetNumEndpoints(intf, numEndpointsHolder);
                if (ret != 0)
                    throwException(ret, "Failed to get number of endpoints");
                int numEndpoints = numEndpointsHolder.get(JAVA_BYTE, 0) & 255;

                for (int pipeIndex = 1; pipeIndex <= numEndpoints; pipeIndex++) {

                    ret = IoKitUSB.GetPipeProperties(intf, (byte) pipeIndex, directionHolder, numberHolder,
                            transferTypeHolder, maxPacketSizeHolder, intervalHolder);
                    if (ret != 0)
                        throwException(ret, "Failed to get pipe properties");

                    int endpointNumber = numberHolder.get(JAVA_BYTE, 0) & 0xff;
                    int direction = directionHolder.get(JAVA_BYTE, 0) & 0xff;
                    byte endpointAddress = (byte) (endpointNumber | (direction << 7));
                    byte transferType = transferTypeHolder.get(JAVA_BYTE, 0);
                    int maxPacketSize = maxPacketSizeHolder.get(JAVA_SHORT, 0) & 0xffff;
                    var endpointInfo = new EndpointInfo(interfaceInfo.iokitInterface(), (byte) pipeIndex,
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
            if (dataLength > 0)
                dataSegment.copyFrom(MemorySegment.ofArray(data));
            var deviceRequest = createDeviceRequest(arena, USBDirection.OUT, setup, dataSegment);

            int ret = IoKitUSB.DeviceRequest(device, deviceRequest);
            if (ret != 0)
                throwException(ret, "Control OUT transfer failed");
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
                ret = IoKitUSB.WritePipe(endpointInfo.iokitInterface(), endpointInfo.pipeIndex, nativeData, data.length);

            } else if (endpointInfo.transferType == USBTransferType.BULK) {

                // bulk transfer with timeout
                ret = IoKitUSB.WritePipeTO(endpointInfo.iokitInterface(), endpointInfo.pipeIndex, nativeData,
                        data.length, timeout, timeout);
                if (ret == IOKit.kIOUSBTransactionTimeout())
                    throw new USBTimeoutException("Transfer out aborted due to timeout");

            } else {

                // interrupt transfer with timeout
                var transferTimeout = new TransferTimeout(endpointInfo, timeout);
                ret = IoKitUSB.WritePipe(endpointInfo.iokitInterface(), endpointInfo.pipeIndex, nativeData, data.length);
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
                ret = IoKitUSB.ReadPipe(endpoint.iokitInterface(), endpoint.pipeIndex, nativeData, sizeHolder);

            } else if (endpoint.transferType == USBTransferType.BULK) {

                // bulk transfer with timeout
                ret = IoKitUSB.ReadPipeTO(endpoint.iokitInterface(), endpoint.pipeIndex, nativeData,
                        sizeHolder, timeout, timeout);
                if (ret == IOKit.kIOUSBTransactionTimeout())
                    throw new USBTimeoutException("Transfer in aborted due to timeout");

            } else {

                // interrupt transfer with timeout
                var transferTimeout = new TransferTimeout(endpoint, timeout);
                ret = IoKitUSB.ReadPipe(endpoint.iokitInterface(), endpoint.pipeIndex, nativeData, sizeHolder);
                if (ret == IOKit.kIOReturnAborted())
                    throw new USBTimeoutException("Transfer in aborted due to timeout");
                transferTimeout.markCompleted();
            }
            if (ret != 0)
                throwException(ret, "Receiving data from endpoint %d failed", endpointNumber);

            int size = sizeHolder.get(JAVA_INT, 0);
            return nativeData.asSlice(0, size).toArray(JAVA_BYTE);
        }
    }

    static final MethodHandle asyncIOCompletedMH;
    static final FunctionDescriptor completionHandlerFuncDesc = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);

    static {
        try {
            asyncIOCompletedMH = MethodHandles.lookup().findStatic(MacosUSBDevice.class, "asyncIOCompleted",
                    MethodType.methodType(void.class, AsyncIOCallback.class, MemorySegment.class, int.class, MemorySegment.class));
        } catch (IllegalAccessException|NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static void asyncIOCompleted(AsyncIOCallback callback, MemorySegment ignoredRefcon, int result, MemorySegment arg0) {
        callback.completed(result, (int)arg0.address());
    }

    synchronized MemorySegment createCompletionHandler(USBDirection direction, int endpointNumber, Arena arena, AsyncIOCallback callback) {
        var endpoint = getEndpointInfo(endpointNumber, direction, USBTransferType.BULK, null);

        var source = IoKitUSB.GetInterfaceAsyncEventSource(endpoint.iokitInterface());
        if (source.address() == 0) {
            try (Arena innerArena = Arena.openConfined()) {
                var sourceHolder = innerArena.allocate(ADDRESS);
                int ret = IoKitUSB.CreateInterfaceAsyncEventSource(endpoint.iokitInterface(), sourceHolder);
                if (ret != 0)
                    throwException(ret, "failed to create event source");
                source = sourceHolder.get(ADDRESS, 0);
                registry.addEventSource(source);
            }
        }

        var methodHandle = asyncIOCompletedMH.bindTo(callback);
        return Linker.nativeLinker().upcallStub(methodHandle, completionHandlerFuncDesc, arena.scope());
    }

    void submitTransferIn(int endpointNumber, MemorySegment buffer, int bufferSize, MemorySegment completionHandler) {

        var endpoint = getEndpointInfo(endpointNumber, USBDirection.IN,
                USBTransferType.BULK, null);

        // submit request
        int ret = IoKitUSB.ReadPipeAsync(endpoint.iokitInterface(), endpoint.pipeIndex, buffer, bufferSize, completionHandler, MemorySegment.NULL);
        if (ret != 0)
            throwException(ret, "failed to submit async transfer");
    }

    void submitTransferOut(int endpointNumber, MemorySegment data, int dataSize, MemorySegment completionHandler) {

        var endpoint = getEndpointInfo(endpointNumber, USBDirection.OUT,
                USBTransferType.BULK, null);

        // submit request
        int ret = IoKitUSB.WritePipeAsync(endpoint.iokitInterface(), endpoint.pipeIndex, data, dataSize, completionHandler, MemorySegment.NULL);
        if (ret != 0)
            throwException(ret, "failed to submit async transfer");
    }

    public void abortTransfer(USBDirection direction, int endpointNumber) {
        var endpointInfo = getEndpointInfo(endpointNumber, direction,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        int ret = IoKitUSB.AbortPipe(endpointInfo.iokitInterface(), endpointInfo.pipeIndex());
        if (ret != 0)
            throwException(ret, "Aborting transfer failed");
    }

        @Override
    public void clearHalt(USBDirection direction, int endpointNumber) {
        var endpointInfo = getEndpointInfo(endpointNumber, direction,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        int ret = IoKitUSB.ClearPipeStallBothEnds(endpointInfo.iokitInterface(), endpointInfo.pipeIndex);
        if (ret != 0)
            throwException(ret, "Clearing halt condition failed");
    }

    @Override
    public InputStream openInputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpoint(endpointNumber, USBDirection.IN, USBTransferType.BULK, null);
        return new MacosEndpointInputStream(this, endpointNumber);
    }

    @Override
    public OutputStream openOutputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpoint(endpointNumber, USBDirection.OUT, USBTransferType.BULK, null);
        return new MacosEndpointOutputStream(this, endpointNumber);
    }

    private static USBTransferType getTransferType(byte macosTransferType) {
        return switch (macosTransferType) {
            case 1 -> USBTransferType.ISOCHRONOUS;
            case 2 -> USBTransferType.BULK;
            case 3 -> USBTransferType.INTERRUPT;
            default -> null;
        };
    }

    record InterfaceInfo(MemorySegment iokitInterface, int interfaceNumber) {
    }

    record EndpointInfo(MemorySegment iokitInterface, byte pipeIndex, USBTransferType transferType, int packetSize) {
    }

    @FunctionalInterface
    public interface AsyncIOCallback {

        /**
         * Called when the asynchronous IO has completed.
         *
         * @param result the result code of the operation
         * @param size the size of the transferred data (in bytes)
         */
        void completed(int result, int size);
    }
}
