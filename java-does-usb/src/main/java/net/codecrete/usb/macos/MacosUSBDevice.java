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
import net.codecrete.usb.common.AsyncIOCompletion;
import net.codecrete.usb.common.ForeignMemory;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.macos.gen.iokit.IOKit;
import net.codecrete.usb.macos.gen.iokit.IOUSBDevRequest;
import net.codecrete.usb.macos.gen.iokit.IOUSBFindInterfaceRequest;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.macos.MacosUSBException.throwException;

/**
 * MacOS implementation of {@link net.codecrete.usb.USBDevice}.
 * <p>
 *     All read and write operations on endpoints are submitted through synchronized methods in order to control
 *     concurrency. If it wasn't controlled, the danger is that device and interface pointers are used, which have
 *     just been closed and thus deallocated by another thread, likely leading to crashes.
 * </p>
 * <p>
 *     As a consequence of the synchronized submission, blocking operations consists of submitting an
 *     asynchronous request and waiting for the completion.
 * </p>
 * <p>
 *     For the completion callback, an upcall stub is created for each device instance. The device instance is bound
 *     to this stub. The stub is used as the {@code callback} parameter in the native functions. For each USB
 *     interface, an event source is registered in the registry's asynchronous IO completion background thread.
 *     To map the callback to a specific request, the request provides a completion handler. It is assigned a unique
 *     number. The number is passed as {@code refcon} to the native call while the mapping between the number and the
 *     completion handler is maintained in a hash map.
 * </p>
 */
public class MacosUSBDevice extends USBDeviceImpl {

    static final MethodHandle asyncIOCompletedMH;
    // Function descriptor for callback functions of type 'IOAsyncCallback1'
    static final FunctionDescriptor completionHandlerFuncDesc = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);

    static {
        try {
            asyncIOCompletedMH = MethodHandles.lookup().findStatic(MacosUSBDevice.class, "asyncIOCompleted",
                    MethodType.methodType(void.class, MacosUSBDevice.class, MemorySegment.class, int.class, MemorySegment.class));
        } catch (IllegalAccessException|NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final MacosUSBDeviceRegistry registry;
    // Native USB device interface (IOUSBDeviceInterface**)
    private MemorySegment device;
    // Currently selected configuration
    private int configurationValue;
    // Details about interfaces that have been claimed
    private List<InterfaceInfo> claimedInterfaces;
    // Details about endpoints of current alternate settings (for claimed interfaces)
    private Map<Byte, EndpointInfo> endpoints;
    // Last used handler ID
    private int lastHandlerId;
    // Completion handlers of currently outstanding asynchronous IO requests,
    // indexed by assigned ID, which is passed in and out as 'refcon'
    private Map<Integer, AsyncIOCompletion> completionHandlers;
    /// Arena used to allocate upcall stub for event source
    private Arena arena;
    /// Upcall stub for IO completion callback
    private MemorySegment completionUpcallStub;

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
    public synchronized void open() {
        if (isOpen())
            throwException("the device is already open");

        // open device
        int ret = IoKitUSB.USBDeviceOpen(device);
        if (ret != 0)
            throwException(ret, "unable to open USB device");

        claimedInterfaces = new ArrayList<>();

        addDeviceEventSource();

        // set configuration
        ret = IoKitUSB.SetConfiguration(device, (byte) configurationValue);
        if (ret != 0)
            throwException(ret, "failed to set configuration");

        updateEndpointList();
    }

    @Override
    public synchronized void close() {
        if (!isOpen())
            return;

        for (InterfaceInfo interfaceInfo : claimedInterfaces) {
            IoKitUSB.USBInterfaceClose(interfaceInfo.iokitInterface());
            IoKitUSB.Release(interfaceInfo.iokitInterface());
            setClaimed(interfaceInfo.interfaceNumber, false);
        }

        claimedInterfaces = null;
        endpoints = null;

        var source = IoKitUSB.GetDeviceAsyncEventSource(device);
        if (source.address() != 0)
            registry.removeEventSource(source);

        IoKitUSB.USBDeviceClose(device);
    }

    synchronized void closeFully() {
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

    public synchronized void claimInterface(int interfaceNumber) {
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
            addInterfaceEventSource(interfaceInfo);
        }

        updateEndpointList();
    }

    public synchronized void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
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

    public synchronized void releaseInterface(int interfaceNumber) {
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

    private synchronized EndpointInfo getEndpointInfo(int endpointNumber, USBDirection direction,
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
                "Endpoint number %d does not exist, is not part of a claimed interface or is not valid for %s transfer in %s direction",
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
        try (var arena = Arena.openConfined()) {
            var data = arena.allocate(length);
            var deviceRequest = createDeviceRequest(arena, USBDirection.IN, setup, data);
            var request = createRequest();

            synchronized (request) {
                submitControlTransfer(deviceRequest, (result, size) -> onRequestCompleted(request, result, size));
                waitForRequest(request, USBDirection.IN, 0, false);
            }

            return data.asSlice(0, request.size).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var arena = Arena.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var dataSegment = arena.allocate(dataLength);
            if (dataLength > 0)
                dataSegment.copyFrom(MemorySegment.ofArray(data));
            var deviceRequest = createDeviceRequest(arena, USBDirection.OUT, setup, dataSegment);
            var request = createRequest();

            synchronized (request) {
                submitControlTransfer(deviceRequest, (result, size) -> onRequestCompleted(request, result, size));
                waitForRequest(request, USBDirection.OUT, 0, false);
            }
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {

        EndpointInfo epInfo = getEndpointInfo(endpointNumber, USBDirection.OUT, USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var nativeData = arena.allocateArray(JAVA_BYTE, data.length);
            nativeData.copyFrom(MemorySegment.ofArray(data));

            var request = createRequest();
            synchronized (request) {
                if (timeout <= 0 || epInfo.transferType() == USBTransferType.BULK) {
                    // no timeout or timeout handled by operating system
                    submitTransferOut(endpointNumber, nativeData, data.length, timeout, (result, size) -> onRequestCompleted(request, result, size));
                    waitForRequest(request, USBDirection.OUT, endpointNumber, false);

                } else {
                    // interrupt transfer with timeout
                    submitTransferOut(endpointNumber, nativeData, data.length, 0, (result, size) -> onRequestCompleted(request, result, size));
                    waitForRequestOrAbort(request, timeout, USBDirection.OUT, endpointNumber);
                }
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int timeout) {

        EndpointInfo epInfo = getEndpointInfo(endpointNumber, USBDirection.IN, USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var nativeData = arena.allocateArray(JAVA_BYTE, epInfo.packetSize());

            var request = createRequest();
            synchronized (request) {
                if (timeout <= 0 || epInfo.transferType() == USBTransferType.BULK) {
                    // no timeout or timeout handled by operating system
                    submitTransferIn(endpointNumber, nativeData, epInfo.packetSize(), timeout, (result, size) -> onRequestCompleted(request, result, size));
                    waitForRequest(request, USBDirection.IN, endpointNumber, false);

                } else {
                    // interrupt transfer with timeout
                    submitTransferIn(endpointNumber, nativeData, epInfo.packetSize(), 0, (result, size) -> onRequestCompleted(request, result, size));
                    waitForRequestOrAbort(request, timeout, USBDirection.IN, endpointNumber);
                }
            }

            return nativeData.asSlice(0, request.size).toArray(JAVA_BYTE);
        }
    }

    /**
     * Submits a transfer IN request to the specified BULK or INTERRUPT endpoint.
     * <p>
     *     A timeout may only be specified for BULK endpoints.
     * </p>
     * @param endpointNumber endpoint number
     * @param buffer buffer that will receive data
     * @param bufferSize buffer length (should be packet size or multiple thereof)
     * @param timeout the timeout, in milliseconds, or 0 for no timeout
     * @param completionHandler handler that will be called when the request completes
     */
    synchronized void submitTransferIn(int endpointNumber, MemorySegment buffer, int bufferSize, int timeout, AsyncIOCompletion completionHandler) {

        EndpointInfo epInfo = getEndpointInfo(endpointNumber, USBDirection.IN, USBTransferType.BULK, USBTransferType.INTERRUPT);

        // submit request
        int ret;
        if (timeout <= 0)
            ret = IoKitUSB.ReadPipeAsync(epInfo.iokitInterface(), epInfo.pipeIndex(), buffer, bufferSize,
                getCompletionUpcallStub(), saveCompletionHandler(completionHandler));
        else
            ret = IoKitUSB.ReadPipeAsyncTO(epInfo.iokitInterface(), epInfo.pipeIndex(), buffer, bufferSize, timeout,
                timeout, getCompletionUpcallStub(), saveCompletionHandler(completionHandler));

        if (ret != 0)
            throwException(ret, "failed to submit transfer IN request");
    }

    /**
     * Submits a transfer OUT request to the specified BULK or INTERRUPT endpoint.
     * <p>
     *     A timeout may only be specified for BULK endpoints.
     * </p>
     * @param endpointNumber endpoint number
     * @param data buffer with data to be transmitted
     * @param dataSize number of bytes to be transmitted
     * @param timeout the timeout, in milliseconds, or 0 for no timeout
     * @param completionHandler handler that will be called when the request completes
     */
    synchronized void submitTransferOut(int endpointNumber, MemorySegment data, int dataSize, int timeout,
                                        AsyncIOCompletion completionHandler) {

        EndpointInfo epInfo = getEndpointInfo(endpointNumber, USBDirection.OUT, USBTransferType.BULK, USBTransferType.INTERRUPT);

        // submit request
        int ret;
        if (timeout <= 0)
            ret = IoKitUSB.WritePipeAsync(epInfo.iokitInterface(), epInfo.pipeIndex(), data, dataSize,
                    getCompletionUpcallStub(), saveCompletionHandler(completionHandler));
        else
            ret = IoKitUSB.WritePipeAsyncTO(epInfo.iokitInterface(), epInfo.pipeIndex(), data, dataSize, timeout,
                    timeout, getCompletionUpcallStub(), saveCompletionHandler(completionHandler));

        if (ret != 0)
            throwException(ret, "failed to submit transfer OUT request");
    }

    /**
     * Submits a control transfer request.
     * @param deviceRequest control transfer request
     * @param completionHandler handler that will be called when the request completes
     */
    synchronized void submitControlTransfer(MemorySegment deviceRequest, AsyncIOCompletion completionHandler) {

        checkIsOpen();

        // submit request
        int ret = IoKitUSB.DeviceRequestAsync(device, deviceRequest, getCompletionUpcallStub(), saveCompletionHandler(completionHandler));

        if (ret != 0)
            throwException(ret, "failed to submit control transfer request");
    }

    private TransferRequest createRequest() {
        var request = new TransferRequest();
        request.size = -1;
        return request;
    }

    private void waitForRequest(TransferRequest request, USBDirection direction, int endpointNumber, boolean ignoreAbort) {
        // wait for request
        while (request.size == -1) {
            try {
                request.wait();
            } catch (InterruptedException e) {
                // ignore and retry
            }
        }

        // test for error
        if (request.result != 0) {
            var operation = getOperationDescription(direction, endpointNumber);
            if (request.result == IOKit.kIOUSBTransactionTimeout())
                throw new USBTimeoutException(operation + "aborted due to timeout");
            if (request.result != IOKit.kIOReturnAborted() || !ignoreAbort)
                throwException(request.result, operation + " failed");
        }
    }

    private void waitForRequestOrAbort(TransferRequest request, int timeout, USBDirection direction, int endpointNumber) {
        // wait for request or abort when timeout occurs
        long expiration = System.currentTimeMillis() + timeout;
        long remainingTimeout = timeout;
        while (remainingTimeout > 0 && request.size == -1) {
            try {
                request.wait(remainingTimeout);
                remainingTimeout = expiration - System.currentTimeMillis();

            } catch (InterruptedException e) {
                // ignore and retry
            }
        }

        // test for timeout
        if (request.result == 0 && remainingTimeout <= 0) {
            abortTransfer(direction, endpointNumber);
            waitForRequest(request, direction, endpointNumber, true);
            throw new USBTimeoutException(getOperationDescription(direction, endpointNumber) + "aborted due to timeout");
        }

        if (request.result != 0) {
            var operation = direction == USBDirection.IN ? "Transfer in" : "Transfer out";
            throwException(request.result, getOperationDescription(direction, endpointNumber) + " failed", operation);
        }
    }

    private static String getOperationDescription(USBDirection direction, int endpointNumber) {
        if (endpointNumber == 0) {
            return "Control transfer";
        } else {
            return String.format("Transfer %s on endpoint %d", direction.name(), endpointNumber);
        }

    }

    private static void onRequestCompleted(TransferRequest request, int result, int size) {
        synchronized (request) {
            request.result = result;
            request.size = size;
            request.notify();
        }
    }

    public void abortTransfer(USBDirection direction, int endpointNumber) {
        EndpointInfo epInfo = getEndpointInfo(endpointNumber, direction, USBTransferType.BULK, USBTransferType.INTERRUPT);

        int ret = IoKitUSB.AbortPipe(epInfo.iokitInterface(), epInfo.pipeIndex());
        if (ret != 0)
            throwException(ret, "Aborting transfer failed");
    }

    @Override
    public void clearHalt(USBDirection direction, int endpointNumber) {
        EndpointInfo epInfo = getEndpointInfo(endpointNumber, direction, USBTransferType.BULK, USBTransferType.INTERRUPT);

        int ret = IoKitUSB.ClearPipeStallBothEnds(epInfo.iokitInterface(), epInfo.pipeIndex());
        if (ret != 0)
            throwException(ret, "Clearing halt condition failed");
    }


    @Override
    public synchronized void abortTransfers(USBDirection direction, int endpointNumber) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public InputStream openInputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpointInfo(endpointNumber, USBDirection.IN, USBTransferType.BULK, null);

        return new MacosEndpointInputStream(this, endpointNumber);
    }

    @Override
    public OutputStream openOutputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpointInfo(endpointNumber, USBDirection.OUT, USBTransferType.BULK, null);

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

    private synchronized MemorySegment getCompletionUpcallStub() {
        if (completionUpcallStub == null) {
            arena = Arena.openShared();
            var methodHandle = asyncIOCompletedMH.bindTo(this);
            completionUpcallStub = Linker.nativeLinker().upcallStub(methodHandle, completionHandlerFuncDesc, arena.scope());
        }

        return completionUpcallStub;
    }

    private synchronized MemorySegment saveCompletionHandler(AsyncIOCompletion handler) {
        if (completionHandlers == null)
            completionHandlers = new HashMap<>();

        while (true) {
            lastHandlerId += 1;
            var key = Integer.valueOf(lastHandlerId);
            if (completionHandlers.containsKey(key))
                continue;

            completionHandlers.put(key, handler);
            return MemorySegment.ofAddress(lastHandlerId, 0, SegmentScope.global());
        }
    }

    private synchronized void addDeviceEventSource() {
        try (Arena innerArena = Arena.openConfined()) {
            var sourceHolder = innerArena.allocate(ADDRESS);
            int ret = IoKitUSB.CreateDeviceAsyncEventSource(device, sourceHolder);
            if (ret != 0)
                throwException(ret, "failed to create event source");
            var source = sourceHolder.get(ADDRESS, 0);
            registry.addEventSource(source);
        }
    }

    private synchronized void addInterfaceEventSource(InterfaceInfo interfaceInfo) {
        try (Arena innerArena = Arena.openConfined()) {
            var sourceHolder = innerArena.allocate(ADDRESS);
            int ret = IoKitUSB.CreateInterfaceAsyncEventSource(interfaceInfo.iokitInterface(), sourceHolder);
            if (ret != 0)
                throwException(ret, "failed to create event source");
            var source = sourceHolder.get(ADDRESS, 0);
            registry.addEventSource(source);
        }
    }

    private synchronized AsyncIOCompletion getCompletionHandler(MemorySegment refcon) {
        var key = Integer.valueOf((int)refcon.address());
        var handler = completionHandlers.remove(key);

        if (!isOpen() && completionHandlers.size() == 0) {
            completionHandlers = null;
            completionUpcallStub = null;
            arena.close();
            arena = null;
        }

        return handler;
    }

    private static void asyncIOCompleted(MacosUSBDevice device, MemorySegment refcon, int result, MemorySegment arg0) {
        device.getCompletionHandler(refcon).completed(result, (int)arg0.address());
    }

    record InterfaceInfo(MemorySegment iokitInterface, int interfaceNumber) {
    }

    record EndpointInfo(MemorySegment iokitInterface, byte pipeIndex, USBTransferType transferType, int packetSize) {
    }

    static class TransferRequest {
        int result;
        int size;
    }
}
