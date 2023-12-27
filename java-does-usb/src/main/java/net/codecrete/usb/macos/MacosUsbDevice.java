//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.*;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.UsbDeviceImpl;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.iokit.IOUSBDevRequest;
import net.codecrete.usb.macos.gen.iokit.IOUSBFindInterfaceRequest;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;
import net.codecrete.usb.usbstandard.Constants;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.common.ForeignMemory.dereference;
import static net.codecrete.usb.macos.MacosUsbException.throwException;

/**
 * MacOS implementation of {@link UsbDevice}.
 * <p>
 * All read and write operations on endpoints are submitted through synchronized methods in order to control
 * concurrency. If it wasn't controlled, the danger is that device and interface pointers are used, which have
 * just been closed and thus deallocated by another thread, likely leading to crashes.
 * </p>
 * <p>
 * As a consequence of the synchronized submission, blocking operations consists of submitting an
 * asynchronous transfer and waiting for the completion.
 * </p>
 */
@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "java:S2160"})
public class MacosUsbDevice extends UsbDeviceImpl {

    private final MacosAsyncTask asyncTask;
    // Native USB device interface (IOUSBDeviceInterface**)
    private MemorySegment device;
    // Currently selected configuration
    private int configurationValue;
    // Details about interfaces that have been claimed
    private List<InterfaceInfo> claimedInterfaces;
    // Details about endpoints of current alternate settings (for claimed interfaces)
    private Map<Byte, EndpointInfo> endpoints;

    private final long discoveryTime;

    MacosUsbDevice(MemorySegment device, Object id, int vendorId, int productId) {
        super(id, vendorId, productId);
        discoveryTime = System.currentTimeMillis();
        asyncTask = MacosAsyncTask.INSTANCE;

        loadDescription(device);

        this.device = device;
        IoKitUsb.AddRef(device);
    }

    @Override
    public void detachStandardDrivers() {
        if (isOpened())
            throwException("detachStandardDrivers() must not be called while the device is open");
        var ret = IoKitUsb.USBDeviceReEnumerate(device, IOKit.kUSBReEnumerateCaptureDeviceMask());
        if (ret != 0)
            throwException(ret, "detaching standard drivers failed");
    }

    @Override
    public void attachStandardDrivers() {
        if (isOpened())
            throwException("attachStandardDrivers() must not be called while the device is open");
        var ret = IoKitUsb.USBDeviceReEnumerate(device, IOKit.kUSBReEnumerateReleaseDeviceMask());
        if (ret != 0)
            throwException(ret, "attaching standard drivers failed");
    }

    @Override
    public boolean isOpened() {
        return claimedInterfaces != null;
    }

    @SuppressWarnings("java:S2276")
    @Override
    public synchronized void open() {
        if (isOpened())
            throwException("device is already open");

        // open device (several retries if device has just been connected/discovered)
        var duration = System.currentTimeMillis() - discoveryTime;
        var numTries = duration < 1000 ? 4 : 1;
        var ret = 0;
        while (numTries > 0) {
            numTries -= 1;
            ret = IoKitUsb.USBDeviceOpenSeize(device);
            if (ret != IOKit.kIOReturnExclusiveAccess())
                break;

            // sleep and retry
            try {
                Thread.sleep(90);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (ret != 0)
            throwException(ret, "opening USB device failed");

        claimedInterfaces = new ArrayList<>();
        addDeviceEventSource();

        // set configuration
        ret = IoKitUsb.SetConfiguration(device, (byte) configurationValue);
        if (ret != 0)
            throwException(ret, "setting configuration failed");

        updateEndpointList();
    }

    @Override
    public synchronized void close() {
        if (!isOpened())
            return;

        for (var interfaceInfo : claimedInterfaces) {
            IoKitUsb.USBInterfaceClose(interfaceInfo.iokitInterface);
            IoKitUsb.Release(interfaceInfo.iokitInterface);
            setClaimed(interfaceInfo.interfaceNumber, false);
        }

        claimedInterfaces = null;
        endpoints = null;

        var source = IoKitUsb.GetDeviceAsyncEventSource(device);
        if (source.address() != 0)
            asyncTask.removeEventSource(source);

        IoKitUsb.USBDeviceClose(device);
    }

    synchronized void closeFully() {
        close();
        IoKitUsb.Release(device);
        device = null;
    }

    private void loadDescription(MemorySegment device) {
        try (var arena = Arena.ofConfined()) {

            // retrieve device descriptor using synchronous control transfer
            var data = arena.allocate(255);
            var deviceRequest = createDeviceRequest(arena, UsbDirection.IN, new UsbControlTransfer(
                    UsbRequestType.STANDARD,
                    UsbRecipient.DEVICE,
                    6, // get descriptor
                    Constants.DEVICE_DESCRIPTOR_TYPE << 8,
                    0
            ), data);
            var ret = IoKitUsb.DeviceRequest(device, deviceRequest);
            if (ret != 0)
                throwException(ret, "querying device descriptor failed");

            var len = IOUSBDevRequest.wLenDone$get(deviceRequest);
            rawDeviceDescriptor = data.asSlice(0, len).toArray(JAVA_BYTE);

            configurationValue = 0;

            // retrieve information of first configuration
            var descPtrHolder = arena.allocate(ADDRESS);
            ret = IoKitUsb.GetConfigurationDescriptorPtr(device, (byte) 0, descPtrHolder);
            if (ret != 0)
                throwException(ret, "querying first configuration failed");

            var configDesc = dereference(descPtrHolder).reinterpret(999999);
            var configDescHeader = new ConfigurationDescriptor(configDesc);
            configDesc = configDesc.asSlice(0, configDescHeader.totalLength());

            var configuration = setConfigurationDescriptor(configDesc);
            configurationValue = 255 & configuration.configValue();
        }
    }

    @SuppressWarnings("java:S135")
    private InterfaceInfo findInterfaceInfo(int interfaceNumber) {

        try (var arena = Arena.ofConfined(); var outerCleanup = new ScopeCleanup()) {
            var request = IOUSBFindInterfaceRequest.allocate(arena);
            IOUSBFindInterfaceRequest.bInterfaceClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceSubClass$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bInterfaceProtocol$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());
            IOUSBFindInterfaceRequest.bAlternateSetting$set(request, (short) IOKit.kIOUSBFindInterfaceDontCare());

            var iterHolder = arena.allocate(JAVA_INT);
            var ret = IoKitUsb.CreateInterfaceIterator(device, request, iterHolder);
            if (ret != 0)
                throwException("internal error (CreateInterfaceIterator)");

            final var iter = iterHolder.get(JAVA_INT, 0);
            outerCleanup.add(() -> IOKit.IOObjectRelease(iter));

            var intfNumberHolder = arena.allocate(JAVA_INT);

            int service;
            while ((service = IOKit.IOIteratorNext(iter)) != 0) {
                try (var cleanup = new ScopeCleanup()) {

                    final var service_final = service;
                    cleanup.add(() -> IOKit.IOObjectRelease(service_final));

                    final var intf = IoKitHelper.getInterface(service, IoKitHelper.kIOUSBInterfaceUserClientTypeID,
                            IoKitHelper.kIOUSBInterfaceInterfaceID190);
                    if (intf == null)
                        continue;

                    cleanup.add(() -> IoKitUsb.Release(intf));

                    IoKitUsb.GetInterfaceNumber(intf, intfNumberHolder);
                    if (intfNumberHolder.get(JAVA_INT, 0) != interfaceNumber)
                        continue;

                    IoKitUsb.AddRef(intf);
                    return new InterfaceInfo(intf, interfaceNumber);
                }
            }
        }

        throwException("invalid interface number: %d", interfaceNumber);
        throw new AssertionError("not reached");
    }

    public synchronized void claimInterface(int interfaceNumber) {
        checkIsOpen();
        getInterfaceWithCheck(interfaceNumber, false);

        try (var cleanup = new ScopeCleanup()) {

            var interfaceInfo = findInterfaceInfo(interfaceNumber);
            cleanup.add(() -> IoKitUsb.Release(interfaceInfo.iokitInterface()));

            var ret = IoKitUsb.USBInterfaceOpen(interfaceInfo.iokitInterface());
            if (ret != 0)
                throwException(ret, "claiming interface failed");

            IoKitUsb.AddRef(interfaceInfo.iokitInterface());
            claimedInterfaces.add(interfaceInfo);
            setClaimed(interfaceNumber, true);
            addInterfaceEventSource(interfaceInfo);
        }

        updateEndpointList();
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "java:S3655"})
    public synchronized void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        // check interface
        var intf = getInterfaceWithCheck(interfaceNumber, true);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        var intfInfo =
                claimedInterfaces.stream().filter(interf -> interf.interfaceNumber() == interfaceNumber).findFirst().get();

        var ret = IoKitUsb.SetAlternateInterface(intfInfo.iokitInterface(), (byte) alternateNumber);
        if (ret != 0)
            throwException(ret, "setting alternate interface failed");

        intf.setAlternate(altSetting);
        updateEndpointList();
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "java:S3655"})
    public synchronized void releaseInterface(int interfaceNumber) {
        checkIsOpen();
        getInterfaceWithCheck(interfaceNumber, true);

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        var interfaceInfo =
                claimedInterfaces.stream().filter(info -> info.interfaceNumber == interfaceNumber).findFirst().get();

        var source = IoKitUsb.GetInterfaceAsyncEventSource(interfaceInfo.iokitInterface());
        if (source.address() != 0)
            asyncTask.removeEventSource(source);

        var ret = IoKitUsb.USBInterfaceClose(interfaceInfo.iokitInterface());
        if (ret != 0)
            throwException(ret, "releasing interface failed");

        claimedInterfaces.remove(interfaceInfo);
        IoKitUsb.Release(interfaceInfo.iokitInterface());
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

        try (var arena = Arena.ofConfined()) {

            var directionHolder = arena.allocate(JAVA_BYTE);
            var numberHolder = arena.allocate(JAVA_BYTE);
            var transferTypeHolder = arena.allocate(JAVA_BYTE);
            var maxPacketSizeHolder = arena.allocate(JAVA_SHORT);
            var intervalHolder = arena.allocate(JAVA_BYTE);

            for (var interfaceInfo : claimedInterfaces) {

                var intf = interfaceInfo.iokitInterface();
                var numEndpointsHolder = arena.allocate(JAVA_BYTE);
                var ret = IoKitUsb.GetNumEndpoints(intf, numEndpointsHolder);
                if (ret != 0)
                    throwException(ret, "internal error (GetNumEndpoints)");
                var numEndpoints = numEndpointsHolder.get(JAVA_BYTE, 0) & 255;

                for (var pipeIndex = 1; pipeIndex <= numEndpoints; pipeIndex++) {

                    ret = IoKitUsb.GetPipeProperties(intf, (byte) pipeIndex, directionHolder, numberHolder,
                            transferTypeHolder, maxPacketSizeHolder, intervalHolder);
                    if (ret != 0)
                        throwException(ret, "internal error (GetPipeProperties)");

                    var endpointNumber = numberHolder.get(JAVA_BYTE, 0) & 0xff;
                    var direction = directionHolder.get(JAVA_BYTE, 0) & 0xff;
                    var endpointAddress = (byte) (endpointNumber | (direction << 7));
                    var transferType = transferTypeHolder.get(JAVA_BYTE, 0);
                    var maxPacketSize = maxPacketSizeHolder.get(JAVA_SHORT, 0) & 0xffff;
                    var endpointInfo = new EndpointInfo(interfaceInfo.iokitInterface(), (byte) pipeIndex,
                            getTransferType(transferType), maxPacketSize);
                    endpoints.put(endpointAddress, endpointInfo);
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private synchronized EndpointInfo getEndpointInfo(int endpointNumber, UsbDirection direction,
                                                      UsbTransferType transferType1, UsbTransferType transferType2) {
        if (endpoints != null) {
            var endpointAddress = (byte) (endpointNumber | (direction == UsbDirection.IN ? 0x80 : 0));
            var endpointInfo = endpoints.get(endpointAddress);
            if (endpointInfo != null && (endpointInfo.transferType == transferType1 || endpointInfo.transferType == transferType2))
                return endpointInfo;
        }

        String transferTypeDesc;
        if (transferType2 == null)
            transferTypeDesc = transferType1.name();
        else
            transferTypeDesc = String.format("%s or %s", transferType1.name(), transferType2.name());

        throwException(
                "endpoint number %d does not exist, is not part of a claimed interface or is not valid for %s transfer in %s direction",
                endpointNumber, transferTypeDesc, direction.name());
        throw new AssertionError("not reached");
    }

    private static MemorySegment createDeviceRequest(Arena arena, UsbDirection direction, UsbControlTransfer setup,
                                                     MemorySegment data) {
        var deviceRequest = IOUSBDevRequest.allocate(arena);
        var bmRequestType =
                (direction == UsbDirection.IN ? 0x80 : 0x00) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        IOUSBDevRequest.bmRequestType$set(deviceRequest, (byte) bmRequestType);
        IOUSBDevRequest.bRequest$set(deviceRequest, (byte) setup.request());
        IOUSBDevRequest.wValue$set(deviceRequest, (short) setup.value());
        IOUSBDevRequest.wIndex$set(deviceRequest, (short) setup.index());
        IOUSBDevRequest.wLength$set(deviceRequest, (short) data.byteSize());
        IOUSBDevRequest.pData$set(deviceRequest, data);
        return deviceRequest;
    }

    @Override
    public byte @NotNull [] controlTransferIn(@NotNull UsbControlTransfer setup, int length) {
        try (var arena = Arena.ofConfined()) {
            var data = arena.allocate(length);
            var deviceRequest = createDeviceRequest(arena, UsbDirection.IN, setup, data);

            var transfer = new MacosTransfer();
            transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);

            synchronized (transfer) {
                submitControlTransfer(deviceRequest, transfer);
                waitForTransfer(transfer, 0, UsbDirection.IN, 0);
            }

            return data.asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(@NotNull UsbControlTransfer setup, byte[] data) {
        try (var arena = Arena.ofConfined()) {
            var dataLength = data != null ? data.length : 0;
            var dataSegment = arena.allocate(dataLength);
            if (dataLength > 0)
                dataSegment.copyFrom(MemorySegment.ofArray(data));
            var deviceRequest = createDeviceRequest(arena, UsbDirection.OUT, setup, dataSegment);

            var transfer = new MacosTransfer();
            transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);

            synchronized (transfer) {
                submitControlTransfer(deviceRequest, transfer);
                waitForTransfer(transfer, 0, UsbDirection.OUT, 0);
            }
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte @NotNull [] data, int offset, int length, int timeout) {

        var epInfo = getEndpointInfo(endpointNumber, UsbDirection.OUT, UsbTransferType.BULK,
                UsbTransferType.INTERRUPT);

        try (var arena = Arena.ofConfined()) {
            var nativeData = arena.allocateArray(JAVA_BYTE, length);
            nativeData.copyFrom(MemorySegment.ofArray(data).asSlice(offset, length));

            var transfer = new MacosTransfer();
            transfer.setData(nativeData);
            transfer.setDataSize(length);
            transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);

            synchronized (transfer) {
                if (timeout <= 0 || epInfo.transferType() == UsbTransferType.BULK) {
                    // no timeout or timeout handled by operating system
                    submitTransferOut(endpointNumber, transfer, timeout);
                    waitForTransfer(transfer, 0, UsbDirection.OUT, endpointNumber);

                } else {
                    // interrupt transfer with timeout
                    submitTransferOut(endpointNumber, transfer, 0);
                    waitForTransfer(transfer, timeout, UsbDirection.OUT, endpointNumber);
                }
            }
        }
    }

    @Override
    public byte @NotNull [] transferIn(int endpointNumber, int timeout) {

        var epInfo = getEndpointInfo(endpointNumber, UsbDirection.IN, UsbTransferType.BULK,
                UsbTransferType.INTERRUPT);

        try (var arena = Arena.ofConfined()) {
            var nativeData = arena.allocateArray(JAVA_BYTE, epInfo.packetSize());

            var transfer = new MacosTransfer();
            transfer.setData(nativeData);
            transfer.setDataSize(epInfo.packetSize());
            transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);

            synchronized (transfer) {
                if (timeout <= 0 || epInfo.transferType() == UsbTransferType.BULK) {
                    // no timeout, or timeout handled by operating system
                    submitTransferIn(endpointNumber, transfer, timeout);
                    waitForTransfer(transfer, 0, UsbDirection.IN, endpointNumber);

                } else {
                    // interrupt transfer with timeout
                    submitTransferIn(endpointNumber, transfer, 0);
                    waitForTransfer(transfer, timeout, UsbDirection.IN, endpointNumber);
                }
            }

            return nativeData.asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    /**
     * Submits a transfer IN to the specified BULK or INTERRUPT endpoint.
     * <p>
     * A timeout may only be specified for BULK endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number
     * @param transfer       transfer to execute
     * @param timeout        the timeout, in milliseconds, or 0 for no timeout
     */
    synchronized void submitTransferIn(int endpointNumber, MacosTransfer transfer, int timeout) {

        var epInfo = getEndpointInfo(endpointNumber, UsbDirection.IN, UsbTransferType.BULK,
                UsbTransferType.INTERRUPT);
        asyncTask.prepareForSubmission(transfer);

        // submit transfer
        int ret;
        if (timeout <= 0)
            ret = IoKitUsb.ReadPipeAsync(epInfo.iokitInterface(), epInfo.pipeIndex(), transfer.data(),
                    transfer.dataSize(), asyncTask.nativeCompletionCallback(), MemorySegment.ofAddress(transfer.id()));
        else
            ret = IoKitUsb.ReadPipeAsyncTO(epInfo.iokitInterface(), epInfo.pipeIndex(), transfer.data(),
                    transfer.dataSize(), timeout, timeout, asyncTask.nativeCompletionCallback(),
                    MemorySegment.ofAddress(transfer.id()));

        if (ret != 0)
            throwException(ret, "error occurred while reading from endpoint %d", endpointNumber);
    }

    /**
     * Submits a transfer OUT to the specified BULK or INTERRUPT endpoint.
     * <p>
     * A timeout may only be specified for BULK endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number
     * @param transfer       transfer request to execute
     * @param timeout        the timeout, in milliseconds, or 0 for no timeout
     */
    synchronized void submitTransferOut(int endpointNumber, MacosTransfer transfer, int timeout) {

        var epInfo = getEndpointInfo(endpointNumber, UsbDirection.OUT, UsbTransferType.BULK,
                UsbTransferType.INTERRUPT);
        asyncTask.prepareForSubmission(transfer);

        // submit transfer
        int ret;
        if (timeout <= 0)
            ret = IoKitUsb.WritePipeAsync(epInfo.iokitInterface(), epInfo.pipeIndex(), transfer.data(),
                    transfer.dataSize(), asyncTask.nativeCompletionCallback(), MemorySegment.ofAddress(transfer.id()));
        else
            ret = IoKitUsb.WritePipeAsyncTO(epInfo.iokitInterface(), epInfo.pipeIndex(), transfer.data(),
                    transfer.dataSize(), timeout, timeout, asyncTask.nativeCompletionCallback(),
                    MemorySegment.ofAddress(transfer.id()));

        if (ret != 0)
            throwException(ret, "error occurred while transmitting to endpoint %d", endpointNumber);
    }

    /**
     * Submits a control transfer.
     *
     * @param deviceRequest control transfer request
     * @param transfer      transfer request (for completion handling)
     */
    synchronized void submitControlTransfer(MemorySegment deviceRequest, MacosTransfer transfer) {

        checkIsOpen();
        asyncTask.prepareForSubmission(transfer);

        // submit transfer
        var ret = IoKitUsb.DeviceRequestAsync(device, deviceRequest, asyncTask.nativeCompletionCallback(),
                MemorySegment.ofAddress(transfer.id()));

        if (ret != 0)
            throwException(ret, "control transfer failed");
    }

    @Override
    protected Transfer createTransfer() {
        return new MacosTransfer();
    }

    @Override
    public void abortTransfers(UsbDirection direction, int endpointNumber) {
        var epInfo = getEndpointInfo(endpointNumber, direction, UsbTransferType.BULK,
                UsbTransferType.INTERRUPT);

        var ret = IoKitUsb.AbortPipe(epInfo.iokitInterface(), epInfo.pipeIndex());
        if (ret != 0)
            throwException(ret, "aborting transfers failed");
    }

    @Override
    public void clearHalt(UsbDirection direction, int endpointNumber) {
        var epInfo = getEndpointInfo(endpointNumber, direction, UsbTransferType.BULK,
                UsbTransferType.INTERRUPT);

        var ret = IoKitUsb.ClearPipeStallBothEnds(epInfo.iokitInterface(), epInfo.pipeIndex());
        if (ret != 0)
            throwException(ret, "clearing halt condition failed");
    }

    @Override
    public synchronized @NotNull InputStream openInputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpointInfo(endpointNumber, UsbDirection.IN, UsbTransferType.BULK, null);

        return new MacosEndpointInputStream(this, endpointNumber, bufferSize);
    }

    @Override
    public synchronized @NotNull OutputStream openOutputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpointInfo(endpointNumber, UsbDirection.OUT, UsbTransferType.BULK, null);

        return new MacosEndpointOutputStream(this, endpointNumber, bufferSize);
    }

    @Override
    protected void throwOSException(int errorCode, String message, Object... args) {
        throwException(errorCode, message, args);
    }

    private static UsbTransferType getTransferType(byte macosTransferType) {
        return switch (macosTransferType) {
            case 1 -> UsbTransferType.ISOCHRONOUS;
            case 2 -> UsbTransferType.BULK;
            case 3 -> UsbTransferType.INTERRUPT;
            default -> null;
        };
    }

    private synchronized void addDeviceEventSource() {
        try (var innerArena = Arena.ofConfined()) {
            var sourceHolder = innerArena.allocate(ADDRESS);
            var ret = IoKitUsb.CreateDeviceAsyncEventSource(device, sourceHolder);
            if (ret != 0)
                throwException(ret, "internal error (CreateDeviceAsyncEventSource)");
            var source = dereference(sourceHolder);
            asyncTask.addEventSource(source);
        }
    }

    private synchronized void addInterfaceEventSource(InterfaceInfo interfaceInfo) {
        try (var innerArena = Arena.ofConfined()) {
            var sourceHolder = innerArena.allocate(ADDRESS);
            var ret = IoKitUsb.CreateInterfaceAsyncEventSource(interfaceInfo.iokitInterface(), sourceHolder);
            if (ret != 0)
                throwException(ret, "internal error (CreateInterfaceAsyncEventSource)");
            var source = dereference(sourceHolder);
            asyncTask.addEventSource(source);
        }
    }

    record InterfaceInfo(MemorySegment iokitInterface, int interfaceNumber) {
    }

    record EndpointInfo(MemorySegment iokitInterface, byte pipeIndex, UsbTransferType transferType, int packetSize) {
    }
}
