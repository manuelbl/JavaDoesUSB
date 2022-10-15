//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.*;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.macos.gen.iokit.IOKit;
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
            throw new USBException("the device is already open");

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
            throw new USBException(String.format("Interface %d has already been claimed", interfaceNumber));

        var firstIntfHandle = intfHandle;
        if (intfHandle.firstInterfaceNumber != interfaceNumber)
            firstIntfHandle = getInterfaceHandle(intfHandle.firstInterfaceNumber);

        if (firstIntfHandle.devicePath == null)
            throw new USBException(String.format("Interface number %d cannot be claimed (non WinUSB device?)", interfaceNumber));

        try (var session = MemorySession.openConfined()) {

            MemoryAddress deviceHandle;

            // open Windows device if needed
            if (firstIntfHandle.deviceHandle == null) {
                var pathSegment = Win.createSegmentFromString(firstIntfHandle.devicePath, session);
                deviceHandle = Kernel32.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                        Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                        Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL);

                if (Win.IsInvalidHandle(deviceHandle))
                    throw new WindowsUSBException(
                            String.format("Cannot open USB device %s", firstIntfHandle.devicePath), Kernel32.GetLastError());
            } else {
                deviceHandle = firstIntfHandle.deviceHandle;
            }

            try {
                // open interface
                var interfaceHandleHolder = session.allocate(ADDRESS);
                if (WinUSB.WinUsb_Initialize(deviceHandle, interfaceHandleHolder) == 0)
                    throw new WindowsUSBException("Cannot open WinUSB device", Kernel32.GetLastError());
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

    public void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var intfHandle = getInterfaceHandle(interfaceNumber);
        if (intfHandle.interfaceHandle == null)
            throw new USBException(String.format("Interface %d has not been claimed", interfaceNumber));

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
                throw new WindowsUSBException("Control transfer IN failed", Kernel32.GetLastError());

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
                throw new WindowsUSBException("Control transfer OUT failed", Kernel32.GetLastError());
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data) {
        transferOut(endpointNumber, data, 0);
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
                throw new WindowsUSBException("Setting timeout failed", Kernel32.GetLastError());

            // copy data to native heap
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var lengthHolder = session.allocate(JAVA_INT);

            // send data
            if (WinUSB.WinUsb_WritePipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0) {
                int err = Kernel32.GetLastError();
                if (err == Kernel32.ERROR_SEM_TIMEOUT())
                    throw new TimeoutException("Transfer out aborted due to timeout");
                throw new WindowsUSBException("Bulk/interrupt transfer OUT failed", err);
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength) {
        return transferIn(endpointNumber, maxLength, 0);
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
                throw new WindowsUSBException("Setting timeout failed", Kernel32.GetLastError());

            // create native heap buffer for data
            var buffer = session.allocate(maxLength);
            var lengthHolder = session.allocate(JAVA_INT);

            // receive data
            if (WinUSB.WinUsb_ReadPipe(intfHandle.interfaceHandle, endpoint.endpointAddress(), buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0) {
                int err = Kernel32.GetLastError();
                if (err == Kernel32.ERROR_SEM_TIMEOUT())
                    throw new TimeoutException("Transfer in aborted due to timeout");
                throw new WindowsUSBException("Bulk/interrupt transfer IN failed", err);
            }

            // copy data
            int len = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, len).toArray(JAVA_BYTE);
        }
    }

    private InterfaceHandle getInterfaceHandle(int interfaceNumber) {
        for (var intfHandle : interfaceHandles_) {
            if (intfHandle.interfaceNumber == interfaceNumber)
                return intfHandle;
        }

        throw new USBException(String.format("Invalid interface number: %s", interfaceNumber));
    }

    private InterfaceHandle findControlTransferInterface(USBControlTransfer setup) {

        int interfaceNumber = -1;
        int endpointNumber;

        if (setup.recipient() == USBRecipient.INTERFACE) {

            interfaceNumber = setup.index() & 0xff;

        } else if (setup.recipient() == USBRecipient.ENDPOINT) {

            endpointNumber = setup.index() & 0xff;
            if (endpointNumber != 0) {
                interfaceNumber = getInterfaceNumber(endpointNumber);
                if (interfaceNumber == -1)
                    throw new USBException(String.format("Invalid endpoint number %d or interface not claimed", endpointNumber));
            }
        }

        if (interfaceNumber >= 0) {
            var intfHandle = getInterfaceHandle(interfaceNumber);
            if (intfHandle.interfaceHandle == null)
                throw new USBException(String.format("Interface number %d has not been claimed", interfaceNumber));
            return intfHandle;
        }

        // for control transfer to device, use any claimed interface
        for (var intfHandle : interfaceHandles_) {
            if (intfHandle.interfaceHandle != null)
                return intfHandle;
        }

        throw new USBException("Control transfer failed as no interface has been claimed");
    }
}
