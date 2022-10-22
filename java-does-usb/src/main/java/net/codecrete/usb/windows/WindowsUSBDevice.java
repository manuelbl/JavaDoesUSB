//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.*;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.usbstandard.SetupPacket;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.winusb.WinUSB;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.windows.WindowsUSBException.throwException;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

/**
 * Windows implementation for USB device.
 */
public class WindowsUSBDevice extends USBDeviceImpl {

    private List<InterfaceHandle> interfaceHandles_;
    private boolean isOpen_;

    WindowsUSBDevice(String devicePath, Map<Integer, String> children, int vendorId, int productId, MemorySegment configDesc) {
        super(devicePath, vendorId, productId);
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
            if (function == null) {
                intfHandle.firstInterfaceNumber = interfaceNumber;
                intfHandle.devicePath = devicePath;
                devicePath = null;
            } else if (function.firstInterfaceNumber() == interfaceNumber) {
                intfHandle.firstInterfaceNumber = interfaceNumber;
                if (children != null) {
                    intfHandle.devicePath = children.get(interfaceNumber);
                } else {
                    intfHandle.devicePath = devicePath;
                    devicePath = null;
                }
            } else {
                intfHandle.firstInterfaceNumber = function.firstInterfaceNumber();
            }
            interfaceHandles_.add(intfHandle);
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen_;
    }

    @Override
    public void open() {
        if (isOpen())
            throwException("the device is already open");

        isOpen_ = true;
    }

    @Override
    public void close() {
        if (!isOpen())
            return;

        for (var intf : interfaces_) {
            if (intf.isClaimed())
                releaseInterface(intf.number());
        }

        isOpen_ = false;
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var intfHandle = getInterfaceHandle(interfaceNumber);
        if (intfHandle.interfaceHandle != null)
            throwException("Interface %d has already been claimed", interfaceNumber);

        var firstIntfHandle = intfHandle;
        if (intfHandle.firstInterfaceNumber != interfaceNumber)
            firstIntfHandle = getInterfaceHandle(intfHandle.firstInterfaceNumber);

        if (firstIntfHandle.devicePath == null)
            throwException("Interface number %d cannot be claimed (non WinUSB device?)", interfaceNumber);

        try (var session = MemorySession.openConfined()) {

            MemoryAddress deviceHandle;

            // open Windows device if needed
            if (firstIntfHandle.deviceHandle == null) {
                var pathSegment = Win.createSegmentFromString(firstIntfHandle.devicePath, session);
                deviceHandle = Kernel32.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                        Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                        Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL);

                if (Win.IsInvalidHandle(deviceHandle))
                    throwLastError("Cannot open USB device %s", firstIntfHandle.devicePath);
            } else {
                deviceHandle = firstIntfHandle.deviceHandle;
            }

            try {
                // open interface
                var interfaceHandleHolder = session.allocate(ADDRESS);
                if (WinUSB.WinUsb_Initialize(deviceHandle, interfaceHandleHolder) == 0)
                    throwLastError("Cannot open WinUSB device");
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
    public void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        checkIsOpen();

        var intfHandle = getInterfaceHandle(interfaceNumber);
        if (intfHandle.interfaceHandle == null)
            throwException("Interface %d has not been claimed", interfaceNumber);

        var intf = getInterface(interfaceNumber);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        if (altSetting == null)
            throwException("Interface %d does not have an alternate interface setting %d", interfaceNumber, alternateNumber);

        if (WinUSB.WinUsb_SetCurrentAlternateSetting(intfHandle.interfaceHandle, (byte) alternateNumber) == 0)
            throwLastError("Failed to set alternate interface");

        intf.setAlternate(altSetting);
    }

    public void releaseInterface(int interfaceNumber) {
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

    private MemorySegment createSetupPacket(MemorySession session, USBDirection direction, USBControlTransfer setup,
                                            MemorySegment data) {
        var setupPacket = new SetupPacket(session);
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

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(length);
            var setupPacket = createSetupPacket(session, USBDirection.IN, setup, buffer);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ControlTransfer(intfHandle.interfaceHandle, setupPacket, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throwLastError("Control transfer IN failed");

            int rxLength = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, rxLength).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        checkIsOpen();
        var intfHandle = findControlTransferInterface(setup);

        try (var session = MemorySession.openConfined()) {

            // copy data to native memory
            int dataLength = data != null ? data.length : 0;
            MemorySegment buffer = session.allocate(dataLength);
            if (dataLength != 0)
                buffer.copyFrom(MemorySegment.ofArray(data));

            // create setup packet
            var setupPacket = createSetupPacket(session, USBDirection.OUT, setup, buffer);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ControlTransfer(intfHandle.interfaceHandle, setupPacket, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throwLastError("Control transfer OUT failed");
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {
        checkIsOpen();

        var endpoint = getEndpoint(endpointNumber, USBDirection.OUT, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var session = MemorySession.openConfined()) {
            // set timeout
            var timeoutHolder = session.allocate(JAVA_INT, timeout);
            if (WinUSB.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder) == 0)
                throwLastError("Setting timeout failed");

            // copy data to native heap
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var lengthHolder = session.allocate(JAVA_INT);

            // send data
            if (WinUSB.WinUsb_WritePipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0) {
                int err = Kernel32.GetLastError();
                if (err == Kernel32.ERROR_SEM_TIMEOUT())
                    throw new USBTimeoutException("Transfer out aborted due to timeout");
                throwException(err, "Bulk/interrupt transfer OUT failed");
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength, int timeout) {
        var endpoint = getEndpoint(endpointNumber, USBDirection.IN, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var session = MemorySession.openConfined()) {
            // set timeout
            var timeoutHolder = session.allocate(JAVA_INT, timeout);
            if (WinUSB.WinUsb_SetPipePolicy(intfHandle.interfaceHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder) == 0)
                throwLastError("Setting timeout failed");

            // create native heap buffer for data
            var buffer = session.allocate(maxLength);
            var lengthHolder = session.allocate(JAVA_INT);

            // receive data
            if (WinUSB.WinUsb_ReadPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0) {
                int err = Kernel32.GetLastError();
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
    public void clearHalt(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(endpointNumber, direction, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());
        if (WinUSB.WinUsb_ResetPipe(intfHandle.interfaceHandle, endpoint.endpointAddress()) == 0)
            throwLastError("Clearing halt failed");
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
