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
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class WindowsUSBDevice extends USBDeviceImpl {

    private final WindowsAsyncTask asyncTask;
    private List<InterfaceHandle> interfaceHandles;
    /**
     * Indicates if {@link #open()} has been called. Since separate interfaces can have separate underlying
     * Windows device, {@link #claimInterface(int)} instead of {@link #open()} will open the Windows device.
     */
    private boolean showAsOpen;

    WindowsUSBDevice(String devicePath, Map<Integer, String> children,
                     int vendorId, int productId, MemorySegment configDesc) {
        super(devicePath, vendorId, productId);
        asyncTask = WindowsAsyncTask.instance();
        readDescription(configDesc, devicePath, children);
    }

    private void readDescription(MemorySegment configDesc, String devicePath, Map<Integer, String> children) {
        var configuration = setConfigurationDescriptor(configDesc);

        // build list of interface handles
        interfaceHandles = new ArrayList<>();
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
            interfaceHandles.add(intfHandle);
        }
    }

    @Override
    public boolean isOpen() {
        return showAsOpen;
    }

    @Override
    public synchronized void open() {
        if (isOpen())
            throwException("the device is already open");

        showAsOpen = true;
    }

    @Override
    public synchronized void close() {
        if (!isOpen())
            return;

        for (var intf : interfaceList) {
            if (intf.isClaimed())
                releaseInterface(intf.number());
        }

        showAsOpen = false;
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

        try (var arena = Arena.ofConfined()) {

            MemorySegment deviceHandle;
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            // open Windows device if needed
            if (firstIntfHandle.deviceHandle == null) {
                var pathSegment = Win.createSegmentFromString(firstIntfHandle.devicePath, arena);
                deviceHandle = Kernel32B.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                        Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                        Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL, lastErrorState);

                if (Win.IsInvalidHandle(deviceHandle))
                    throwLastError(lastErrorState, "Cannot open USB device %s", firstIntfHandle.devicePath);

                asyncTask.addDevice(deviceHandle);

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

        try (var arena = Arena.ofConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
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

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var arena = Arena.ofConfined()) {

            // copy data to native memory
            var transfer = createSyncControlTransfer();
            int dataLength = data != null ? data.length : 0;
            transfer.dataSize = dataLength;
            if (dataLength != 0) {
                var buffer = arena.allocate(data.length);
                buffer.copyFrom(MemorySegment.ofArray(data));
                transfer.data = buffer;
            } else {
                transfer.data = NULL;
            }

            synchronized (transfer) {
                submitControlTransfer(USBDirection.OUT, setup, transfer);
                waitForTransfer(transfer, 0, USBDirection.OUT, 0);
            }
        }
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var arena = Arena.ofConfined()) {
            var transfer = createSyncControlTransfer();
            transfer.data = arena.allocate(length);
            transfer.dataSize = length;

            synchronized (transfer) {
                submitControlTransfer(USBDirection.IN, setup, transfer);
                waitForTransfer(transfer, 0, USBDirection.IN, 0);
            }

            return transfer.data.asSlice(0, transfer.resultSize).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int offset, int length, int timeout) {
        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data).asSlice(offset, length));
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransferOut(endpointNumber, transfer);
                waitForTransfer(transfer, timeout, USBDirection.OUT, endpointNumber);
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int timeout) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(endpoint.packetSize());
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransferIn(endpointNumber, transfer);
                waitForTransfer(transfer, timeout, USBDirection.IN, endpointNumber);
            }

            return buffer.asSlice(0, transfer.resultSize).toArray(JAVA_BYTE);
        }
    }

    private WindowsTransfer createSyncControlTransfer() {
        var transfer = new WindowsTransfer();
        transfer.completion = USBDeviceImpl::onSyncTransferCompleted;
        return transfer;
    }

    private WindowsTransfer createSyncTransfer(MemorySegment data) {
        var transfer = new WindowsTransfer();
        transfer.data = data;
        transfer.dataSize = (int) data.byteSize();
        transfer.completion = USBDeviceImpl::onSyncTransferCompleted;
        return transfer;
    }

    @Override
    protected Transfer createTransfer() {
        return new WindowsTransfer();
    }

    @Override
    protected void throwOSException(int errorCode, String message, Object... args) {
        throwException(errorCode, message, args);
    }

    synchronized void submitControlTransfer(USBDirection direction, USBControlTransfer setup, WindowsTransfer transfer) {
        checkIsOpen();
        var intfHandle = findControlTransferInterface(setup);

        try (var arena = Arena.ofConfined()) {
            var setupPacket = new SetupPacket(arena);
            var bmRequest =
                    (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
            setupPacket.setRequestType(bmRequest);
            setupPacket.setRequest(setup.request());
            setupPacket.setValue(setup.value());
            setupPacket.setIndex(setup.index());
            setupPacket.setLength(transfer.dataSize);

            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUSB2.WinUsb_ControlTransfer(intfHandle.interfaceHandle, setupPacket.segment(), transfer.data,
                    transfer.dataSize, NULL, transfer.overlapped, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "Submitting control transfer failed");
            }
        }
    }

    synchronized void submitTransferOut(int endpointNumber, WindowsTransfer transfer) {
        var endpoint = getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUSB2.WinUsb_WritePipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), transfer.data, transfer.dataSize, NULL
                    , transfer.overlapped, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "Submitting transfer OUT failed");
            }
        }
    }

    synchronized void submitTransferIn(int endpointNumber, WindowsTransfer transfer) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUSB2.WinUsb_ReadPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), transfer.data, transfer.dataSize,
                    NULL, transfer.overlapped, lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "Submitting transfer IN failed");
            }
        }
    }

    synchronized void configureForAsyncIo(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            var timeoutHolder = arena.allocate(JAVA_INT, 0);
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder, lastErrorState) == 0)
                throwLastError(lastErrorState, "Setting timeout failed");

            var rawIoHolder = arena.allocate(JAVA_BYTE, (byte) 1);
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(), WinUSB.RAW_IO(),
                    (int) rawIoHolder.byteSize(), rawIoHolder, lastErrorState) == 0)
                throwLastError(lastErrorState, "Setting raw IO failed");
        }
    }

    @Override
    public synchronized void clearHalt(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            if (WinUSB2.WinUsb_ResetPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), lastErrorState) == 0)
                throwLastError(lastErrorState, "Clearing halt failed");
        }
    }

    @Override
    public synchronized void abortTransfers(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            if (WinUSB2.WinUsb_AbortPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), lastErrorState) == 0)
                throwLastError(lastErrorState, "Aborting transfers on endpoint failed");
        }
    }

    @Override
    public synchronized InputStream openInputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, null);

        return new WindowsEndpointInputStream(this, endpointNumber, bufferSize);
    }

    @Override
    public synchronized OutputStream openOutputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, null);

        return new WindowsEndpointOutputStream(this, endpointNumber, bufferSize);
    }

    private InterfaceHandle getInterfaceHandle(int interfaceNumber) {
        for (var intfHandle : interfaceHandles) {
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
        for (var intfHandle : interfaceHandles) {
            if (intfHandle.interfaceHandle != null)
                return intfHandle;
        }

        throwException("Control transfer failed as no interface has been claimed");
        throw new AssertionError("not reached");
    }
}
