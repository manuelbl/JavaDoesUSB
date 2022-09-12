//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.*;
import net.codecrete.usb.common.DescriptorParser;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBStructs;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.winusb.WinUSB;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

/**
 * Windows implementation for USB device.
 */
public class WindowsUSBDevice extends USBDeviceImpl {

    private final List<CompositeFunction> functions;
    private boolean isOpen_;

    WindowsUSBDevice(String devicePath, List<CompositeFunction> functions, int vendorId, int productId, String manufacturer, String product, String serial,
                     MemorySegment configDesc) {
        super(devicePath, vendorId, productId, manufacturer, product, serial);
        this.functions = functions;
        readDescription(configDesc);
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

    private void readDescription(MemorySegment configDesc) {
        var configuration = DescriptorParser.parseConfigurationDescriptor(configDesc, vendorId(), productId());
        setInterfaces(configuration.interfaces);
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

    private CompositeFunction findFunction(int interfaceNumber) {
        return functions.stream()
                .filter((func) -> func.firstInterfaceNumber() == interfaceNumber).findFirst().orElse(null);
    }

    private CompositeFunction findAnyOpenFunction() {
        return functions.stream()
                .filter((func) -> func.firstInterfaceHandle() != null).findFirst().orElse(null);
    }

    private CompositeFunction findControlTransferFunction(USBControlTransfer setup) {

        int interfaceNumber = -1;
        int endpointNumber = -1;

        if (setup.recipient() == USBRecipient.INTERFACE) {

            interfaceNumber = setup.index() & 0xff;

        } else if (setup.recipient() == USBRecipient.ENDPOINT) {

            endpointNumber = setup.index() & 0xff;
            if (endpointNumber != 0) {
                interfaceNumber = getInterfaceNumber(endpointNumber);
                if (interfaceNumber == -1)
                    interfaceNumber = -2;
            }
        }

        CompositeFunction function = null;
        if (interfaceNumber >= 0) {
            function = findFunction(interfaceNumber);
        } else if (interfaceNumber == -1) {
            function = findAnyOpenFunction();
        }

        if (function == null || function.firstInterfaceHandle() == null)
            throw new USBException("Interface not claimed for control transfer");

        return function;
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (intf.isClaimed())
            throw new USBException(String.format("Interface %d has already been claimed", interfaceNumber));
        var function = findFunction(interfaceNumber);
        if (function == null)
            throw new USBException(String.format("Interface number %d cannot be claimed (no DeviceInterfaceGUID?)", interfaceNumber));

        try (var session = MemorySession.openConfined()) {

            // open Windows device
            var pathSegment = Win.createSegmentFromString(function.devicePath(), session);
            var deviceHandle = Kernel32.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                    Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                    Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL);

            if (Win.IsInvalidHandle(deviceHandle))
                throw new WindowsUSBException(
                        String.format("Cannot open USB device %s", function.devicePath()), Kernel32.GetLastError());

            try {
                // open interface
                var interfaceHandleHolder = session.allocate(ADDRESS);
                if (WinUSB.WinUsb_Initialize(deviceHandle, interfaceHandleHolder) == 0)
                    throw new WindowsUSBException("Cannot open WinUSB device", Kernel32.GetLastError());
                var interfaceHandle = interfaceHandleHolder.get(ADDRESS, 0);

                function.setDeviceHandle(deviceHandle);
                function.setFirstInterfaceHandle(interfaceHandle);

            } catch (Throwable e) {
                Kernel32.CloseHandle(deviceHandle);
                throw e;
            }
        }

        setClaimed(interfaceNumber, true);
    }

    public void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (!intf.isClaimed())
            throw new USBException(String.format("Interface %d has not been claimed", interfaceNumber));

        var function = findFunction(interfaceNumber);
        assert function != null;

        if (function.deviceHandle() != null) {
            WinUSB.WinUsb_Free(function.firstInterfaceHandle());
            function.setFirstInterfaceHandle(null);
            Kernel32.CloseHandle(function.deviceHandle());
            function.setDeviceHandle(null);
        }

        setClaimed(interfaceNumber, false);
    }

    private MemorySegment createSetupPacket(MemorySession session, USBDirection direction, USBControlTransfer setup,
                                            MemorySegment data) {
        var setupPacket = session.allocate(USBStructs.SetupPacket$Struct);
        var bmRequest =
                (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        USBStructs.SetupPacket_bmRequest.set(setupPacket, (byte) bmRequest);
        USBStructs.SetupPacket_bRequest.set(setupPacket, setup.request());
        USBStructs.SetupPacket_wValue.set(setupPacket, setup.value());
        USBStructs.SetupPacket_wIndex.set(setupPacket, setup.index());
        USBStructs.SetupPacket_wLength.set(setupPacket, (short) (data != null ? data.byteSize() : 0));
        return setupPacket;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        checkIsOpen();
        var function = findControlTransferFunction(setup);

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(length);
            var setupPacket = createSetupPacket(session, USBDirection.IN, setup, buffer);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ControlTransfer(function.firstInterfaceHandle(), setupPacket, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throw new WindowsUSBException("Control transfer IN failed", Kernel32.GetLastError());

            int rxLength = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, rxLength).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        checkIsOpen();
        var function = findControlTransferFunction(setup);

        try (var session = MemorySession.openConfined()) {

            // copy data to native memory
            int dataLength = data != null ? data.length : 0;
            MemorySegment buffer = session.allocate(dataLength);
            if (dataLength != 0)
                buffer.copyFrom(MemorySegment.ofArray(data));

            // create setup packet
            var setupPacket = createSetupPacket(session, USBDirection.OUT, setup, buffer);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ControlTransfer(function.firstInterfaceHandle(), setupPacket, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throw new WindowsUSBException("Control transfer OUT failed", Kernel32.GetLastError());
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data) {
        checkIsOpen();

        var endpoint = getEndpoint(endpointNumber, USBDirection.OUT, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var function = findFunction(endpoint.interfaceNumber());
        assert function != null;

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_WritePipe(function.firstInterfaceHandle(), endpoint.endpointAddress(), buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throw new WindowsUSBException("Bulk/interrupt transfer OUT failed", Kernel32.GetLastError());
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength) {
        var endpoint = getEndpoint(endpointNumber, USBDirection.IN, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var function = findFunction(endpoint.interfaceNumber());
        assert function != null;

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(maxLength);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ReadPipe(function.firstInterfaceHandle(), endpoint.endpointAddress(), buffer, (int) buffer.byteSize(), lengthHolder
                    , NULL) == 0)
                throw new WindowsUSBException("Bulk/interrupt transfer IN failed", Kernel32.GetLastError());

            int len = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, len).toArray(JAVA_BYTE);
        }
    }
}
