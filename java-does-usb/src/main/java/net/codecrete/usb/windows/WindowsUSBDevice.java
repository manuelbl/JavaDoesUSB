//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBControlTransfer;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.DescriptorParser;
import net.codecrete.usb.common.DescriptorParser.Configuration;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBInterfaceImpl;
import net.codecrete.usb.common.USBStructs;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.winusb.WinUSB;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

/**
 * Windows implementation for USB device.
 */
public class WindowsUSBDevice extends USBDeviceImpl {

    private MemoryAddress device;
    private MemoryAddress firstInterface;
    private Configuration configuration;

    WindowsUSBDevice(Object id, int vendorId, int productId, String manufacturer, String product, String serial,
                     MemorySegment configDesc) {
        super(id, vendorId, productId, manufacturer, product, serial);
        readDescription(configDesc);
    }

    @Override
    public boolean isOpen() {
        return device != null;
    }

    @Override
    public void open() {

        if (isOpen())
            throw new USBException("the device is already open");

        try (var session = MemorySession.openConfined()) {

            // open Windows device
            var pathSegment = Win.createSegmentFromString(id_.toString(), session);
            device = Kernel32.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                    Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                    Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL);

            if (Win.IsInvalidHandle(device))
                throw new USBException("Cannot open USB device", Kernel32.GetLastError());

            try {
                // open first USB interface
                var interfaceHandleHolder = session.allocate(ADDRESS);
                if (WinUSB.WinUsb_Initialize(device, interfaceHandleHolder) == 0)
                    throw new USBException("Cannot open USB device", Kernel32.GetLastError());

                firstInterface = interfaceHandleHolder.get(ADDRESS, 0);

            } catch (Throwable e) {
                Kernel32.CloseHandle(device);
                firstInterface = null;
                device = null;
                throw e;
            }
        }
    }

    private void readDescription(MemorySegment configDesc) {
        configuration = DescriptorParser.parseConfigurationDescriptor(configDesc, vendorId(), productId());
        setInterfaces(configuration.interfaces);
    }

    @Override
    public void close() {
        if (!isOpen())
            return;

        for (var intf : interfaces_)
            ((USBInterfaceImpl) intf).setClaimed(false);

        WinUSB.WinUsb_Free(firstInterface);
        firstInterface = null;
        Kernel32.CloseHandle(device);
        device = null;
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = configuration.findInterfaceByNumber(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (intf.isClaimed())
            throw new USBException(String.format("Interface %d has already been claimed", interfaceNumber));

        setClaimed(interfaceNumber, true);
    }

    public void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = configuration.findInterfaceByNumber(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (!intf.isClaimed())
            throw new USBException(String.format("Interface %d has not been claimed", interfaceNumber));

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

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(length);
            var setupPacket = createSetupPacket(session, USBDirection.IN, setup, buffer);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ControlTransfer(firstInterface, setupPacket, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throw new USBException("Control transfer IN failed", Kernel32.GetLastError());

            int rxLength = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, rxLength).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        checkIsOpen();

        try (var session = MemorySession.openConfined()) {

            // copy data to native memory
            int dataLength = data != null ? data.length : 0;
            MemorySegment buffer = session.allocate(dataLength);
            if (dataLength != 0)
                buffer.copyFrom(MemorySegment.ofArray(data));

            // create setup packet
            var setupPacket = createSetupPacket(session, USBDirection.OUT, setup, buffer);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ControlTransfer(firstInterface, setupPacket, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throw new USBException("Control transfer OUT failed", Kernel32.GetLastError());
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data) {
        byte endpointAddress = getEndpointAddress(endpointNumber, USBDirection.OUT);

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_WritePipe(firstInterface, endpointAddress, buffer, (int) buffer.byteSize(),
                    lengthHolder, NULL) == 0)
                throw new USBException("Control transfer IN failed", Kernel32.GetLastError());
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength) {
        byte endpointAddress = getEndpointAddress(endpointNumber, USBDirection.IN);

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(maxLength);
            var lengthHolder = session.allocate(JAVA_INT);

            if (WinUSB.WinUsb_ReadPipe(firstInterface, endpointAddress, buffer, (int) buffer.byteSize(), lengthHolder
                    , NULL) == 0)
                throw new USBException("Control transfer IN failed", Kernel32.GetLastError());

            int len = lengthHolder.get(JAVA_INT, 0);
            return buffer.asSlice(0, len).toArray(JAVA_BYTE);
        }
    }
}
