//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.*;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.usbstandard.SetupPacket;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.winusb.WinUSB;
import net.codecrete.usb.windows.winsdk.Kernel32B;
import net.codecrete.usb.windows.winsdk.WinUSB2;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.windows.WindowsUSBException.throwException;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

/**
 * Windows implementation for USB device.
 */
public class WindowsUSBDevice extends USBDeviceImpl {

    private final WindowsUSBDeviceRegistry registry;
    private List<InterfaceHandle> interfaceHandles_;
    private boolean isOpen_;

    WindowsUSBDevice(WindowsUSBDeviceRegistry registry, String devicePath, Map<Integer, String> children,
                     int vendorId, int productId, MemorySegment configDesc) {
        super(devicePath, vendorId, productId);
        this.registry = registry;
        readDescription(configDesc, devicePath, children);
    }

    private void readDescription(MemorySegment configDesc, String devicePath, Map<Integer, String> children) {
        var configuration = setConfigurationDescriptor(configDesc);

        // build list of interface handles
        interfaceHandles_ = new ArrayList<>();
        for (var intf : configuration.interfaces()) {
            var interfaceNumber = intf.number();
            var function = configuration.findFunction(interfaceNumber);

            var intfHandle = new InterfaceHandle();
            intfHandle.interfaceNumber = interfaceNumber;
            if (function.firstInterfaceNumber() == interfaceNumber) {
                if (children == null) {
                    intfHandle.devicePath = devicePath;
                } else {
                    intfHandle.devicePath = children.get(interfaceNumber);
                }
            }
            intfHandle.firstInterfaceNumber = function.firstInterfaceNumber();
            interfaceHandles_.add(intfHandle);
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen_;
    }

    @Override
    public synchronized void open() {
        if (isOpen())
            throwException("the device is already open");

        isOpen_ = true;
    }

    @Override
    public synchronized void close() {
        if (!isOpen())
            return;

        for (var intf : interfaces_) {
            if (intf.isClaimed())
                releaseInterface(intf.number());
        }

        isOpen_ = false;
    }

    public synchronized void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var intfHandle = getInterfaceHandle(interfaceNumber);
        if (intfHandle.interfaceHandle != null)
            throwException("Interface %d has already been claimed", interfaceNumber);

        var firstIntfHandle = intfHandle;
        if (intfHandle.firstInterfaceNumber != interfaceNumber)
            firstIntfHandle = getInterfaceHandle(intfHandle.firstInterfaceNumber);

        if (firstIntfHandle.devicePath == null)
            throwException("Interface number %d cannot be claimed (non WinUSB device?)", interfaceNumber);

        try (var arena = Arena.openConfined()) {

            MemorySegment deviceHandle;
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());

            // open Windows device if needed
            if (firstIntfHandle.deviceHandle == null) {
                var pathSegment = Win.createSegmentFromString(firstIntfHandle.devicePath, arena);
                deviceHandle = Kernel32B.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                        Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                        Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL, lastErrorState);

                if (Win.IsInvalidHandle(deviceHandle))
                    throwLastError(lastErrorState, "Cannot open USB device %s", firstIntfHandle.devicePath);

                registry.addToCompletionPort(deviceHandle, this);

            } else {
                deviceHandle = firstIntfHandle.deviceHandle;
            }

            try {
                // open interface
                var interfaceHandleHolder = arena.allocate(ADDRESS);
                if (WinUSB2.WinUsb_Initialize(deviceHandle, interfaceHandleHolder, lastErrorState) == 0)
                    throwLastError(lastErrorState, "Cannot open WinUSB device");
                var interfaceHandle = interfaceHandleHolder.get(ADDRESS, 0);

                firstIntfHandle.deviceHandle = deviceHandle;
                firstIntfHandle.deviceOpenCount += 1;
                intfHandle.interfaceHandle = interfaceHandle;

            } catch (Throwable e) {
                Kernel32.CloseHandle(deviceHandle);
                throw e;
            }
        }

        setClaimed(interfaceNumber, true);
    }

    @Override
    public synchronized void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        checkIsOpen();

        var intfHandle = getInterfaceHandle(interfaceNumber);
        if (intfHandle.interfaceHandle == null)
            throwException("Interface %d has not been claimed", interfaceNumber);

        var intf = getInterface(interfaceNumber);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        if (altSetting == null)
            throwException("Interface %d does not have an alternate interface setting %d", interfaceNumber,
                    alternateNumber);

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            if (WinUSB2.WinUsb_SetCurrentAlternateSetting(intfHandle.interfaceHandle, (byte) alternateNumber,
                    lastErrorState) == 0)
                throwLastError(lastErrorState, "Failed to set alternate interface");
        }
        intf.setAlternate(altSetting);
    }

    public synchronized void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var intfHandle = getInterfaceHandle(interfaceNumber);
        if (intfHandle.interfaceHandle == null)
            throwException("Interface %d has not been claimed", interfaceNumber);

        var firstIntfHandle = intfHandle;
        if (intfHandle.firstInterfaceNumber != interfaceNumber)
            firstIntfHandle = getInterfaceHandle(intfHandle.firstInterfaceNumber);

        // close interface
        WinUSB.WinUsb_Free(intfHandle.interfaceHandle);
        intfHandle.interfaceHandle = null;

        // close device
        firstIntfHandle.deviceOpenCount -= 1;
        if (firstIntfHandle.deviceOpenCount == 0) {
            Kernel32.CloseHandle(firstIntfHandle.deviceHandle);
            firstIntfHandle.deviceHandle = null;
        }

        setClaimed(interfaceNumber, false);
    }

    private MemorySegment createSetupPacket(USBDirection direction, USBControlTransfer setup, MemorySegment data,
                                            Arena arena) {
        var setupPacket = new SetupPacket(arena);
        var bmRequest =
                (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        setupPacket.setRequestType(bmRequest);
        setupPacket.setRequest(setup.request());
        setupPacket.setValue(setup.value());
        setupPacket.setIndex(setup.index());
        setupPacket.setLength(data != null ? (int) data.byteSize() : 0);
        return setupPacket.segment();
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        checkIsOpen();
        var intfHandle = findControlTransferInterface(setup);

        try (var arena = Arena.openConfined()) {
            var buffer = arena.allocate(length);
            var setupPacket = createSetupPacket(USBDirection.IN, setup, buffer, arena);
            var lengthHolder = arena.allocate(JAVA_INT);
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());

            if (WinUSB2.WinUsb_ControlTransfer(intfHandle.interfaceHandle, setupPacket, buffer,
                    (int) buffer.byteSize(), lengthHolder, NULL, lastErrorState) == 0)
                throwLastError(lastErrorState, "Control transfer IN failed");

            int rxLength = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, rxLength).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        checkIsOpen();
        var intfHandle = findControlTransferInterface(setup);

        try (var arena = Arena.openConfined()) {

            // copy data to native memory
            int dataLength = data != null ? data.length : 0;
            MemorySegment buffer = arena.allocate(dataLength);
            if (dataLength != 0)
                buffer.copyFrom(MemorySegment.ofArray(data));

            // create setup packet
            var setupPacket = createSetupPacket(USBDirection.OUT, setup, buffer, arena);
            var lengthHolder = arena.allocate(JAVA_INT);
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());

            if (WinUSB2.WinUsb_ControlTransfer(intfHandle.interfaceHandle, setupPacket, buffer,
                    (int) buffer.byteSize(), lengthHolder, NULL, lastErrorState) == 0)
                throwLastError(lastErrorState, "Control transfer OUT failed");
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {
        checkIsOpen();

        var endpoint = getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            // set timeout
            var timeoutHolder = arena.allocate(JAVA_INT, timeout);
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());

            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder, lastErrorState) == 0)
                throwLastError(lastErrorState, "Setting timeout failed");

            // copy data to native heap
            var buffer = arena.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var lengthHolder = arena.allocate(JAVA_INT);

            // send data
            if (WinUSB2.WinUsb_WritePipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), buffer,
                    (int) buffer.byteSize(), lengthHolder, NULL, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err == Kernel32.ERROR_SEM_TIMEOUT())
                    throw new USBTimeoutException("Transfer out aborted due to timeout");
                throwException(err, "Bulk/interrupt transfer OUT failed");
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int timeout) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            // set timeout
            var timeoutHolder = arena.allocate(JAVA_INT, timeout);
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder, lastErrorState) == 0)
                throwLastError(lastErrorState, "Setting timeout failed");

            // create native heap buffer for data
            var buffer = arena.allocate(endpoint.packetSize());
            var lengthHolder = arena.allocate(JAVA_INT);

            // receive data
            if (WinUSB2.WinUsb_ReadPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), buffer,
                    (int) buffer.byteSize(), lengthHolder, NULL, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err == Kernel32.ERROR_SEM_TIMEOUT())
                    throw new USBTimeoutException("Transfer in aborted due to timeout");
                throwException(err, "Bulk/interrupt transfer IN failed");
            }

            // copy data
            int len = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, len).toArray(JAVA_BYTE);
        }
    }

    @Override
    protected Transfer createTransfer() {
        return registry.createRequest();
    }

    @Override
    protected void throwOSException(int errorCode, String message, Object... args) {
        throwException(errorCode, message, args);
    }

    synchronized void submitTransferOut(int endpointNumber, WindowsTransfer request) {
        var endpoint = getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            registry.addOverlapped(request);

            // submit transfer
            if (WinUSB2.WinUsb_WritePipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), request.data, request.dataSize, NULL
                    , request.overlapped, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "Submitting transfer OUT failed");
            }
        }
    }

    synchronized void submitTransferIn(int endpointNumber, WindowsTransfer request) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            registry.addOverlapped(request);

            // submit transfer
            if (WinUSB2.WinUsb_ReadPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), request.data, request.dataSize,
                    NULL, request.overlapped, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "Submitting transfer IN failed");
            }
        }
    }

    /**
     * Cancels an asynchronous IO request.
     * <p>
     * If the specific request is not found, the error is silently ignored. Such errors
     * are likely to happen due to concurrency issues.
     * </p>
     *
     * @param direction      endpoint direction
     * @param endpointNumber endpoint number
     * @param cancelHandle   request's cancel handle returned when the request was submitted
     */
    synchronized void cancelTransfer(USBDirection direction, int endpointNumber, long cancelHandle) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            var overlapped = MemorySegment.ofAddress(cancelHandle, 0, SegmentScope.global());

            if (Kernel32B.CancelIoEx(intfHandle.deviceHandle, overlapped, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err != Kernel32.ERROR_NOT_FOUND())
                    throwException(err, "Cancelling transfer failed");
            }
        }
    }

    synchronized void configureForAsyncIo(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());

            var timeoutHolder = arena.allocate(JAVA_INT, 0);
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder, lastErrorState) == 0)
                throwLastError(lastErrorState, "Setting timeout failed");

            var rawIoHolder = arena.allocate(JAVA_BYTE, (byte) 0);
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(), WinUSB.RAW_IO(),
                    (int) rawIoHolder.byteSize(), rawIoHolder, lastErrorState) == 0)
                throwLastError(lastErrorState, "Setting raw IO failed");
        }
    }

    @Override
    public void clearHalt(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            if (WinUSB2.WinUsb_ResetPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), lastErrorState) == 0)
                throwLastError(lastErrorState, "Clearing halt failed");
        }
    }

    @Override
    public void abortTransfers(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.openConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE.layout());
            if (WinUSB2.WinUsb_AbortPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), lastErrorState) == 0)
                throwLastError(lastErrorState, "Aborting transfers on endpoint failed");
        }
    }

    @Override
    public InputStream openInputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, null);

        return new WindowsEndpointInputStream(this, endpointNumber);
    }

    @Override
    public OutputStream openOutputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, null);

        return new WindowsEndpointOutputStream(this, endpointNumber);
    }

    private InterfaceHandle getInterfaceHandle(int interfaceNumber) {
        for (var intfHandle : interfaceHandles_) {
            if (intfHandle.interfaceNumber == interfaceNumber)
                return intfHandle;
        }

        throwException("Invalid interface number: %s", interfaceNumber);
        throw new AssertionError("not reached");
    }

    private InterfaceHandle findControlTransferInterface(USBControlTransfer setup) {

        int interfaceNumber = -1;
        int endpointNumber;

        if (setup.recipient() == USBRecipient.INTERFACE) {

            interfaceNumber = setup.index() & 0xff;

        } else if (setup.recipient() == USBRecipient.ENDPOINT) {

            endpointNumber = setup.index() & 0x7f;
            var direction = (setup.index() & 0x80) != 0 ? USBDirection.IN : USBDirection.OUT;
            if (endpointNumber != 0) {
                interfaceNumber = getInterfaceNumber(direction, endpointNumber);
                if (interfaceNumber == -1)
                    throwException("Invalid endpoint number %d or interface not claimed", endpointNumber);
            }
        }

        if (interfaceNumber >= 0) {
            var intfHandle = getInterfaceHandle(interfaceNumber);
            if (intfHandle.interfaceHandle == null)
                throwException("Interface number %d has not been claimed", interfaceNumber);
            return intfHandle;
        }

        // for control transfer to device, use any claimed interface
        for (var intfHandle : interfaceHandles_) {
            if (intfHandle.interfaceHandle != null)
                return intfHandle;
        }

        throwException("Control transfer failed as no interface has been claimed");
        throw new AssertionError("not reached");
    }
}
