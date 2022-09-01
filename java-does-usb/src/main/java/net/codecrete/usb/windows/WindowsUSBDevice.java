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
import net.codecrete.usb.common.DescriptorParser.Interface;
import net.codecrete.usb.common.USBDescriptors;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBStructs;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.winusb.WinUSB;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

/**
 * Windows implementation for USB device.
 */
public class WindowsUSBDevice extends USBDeviceImpl {

    private MemoryAddress device;
    private MemoryAddress firstInterface;
    private final byte currentConfigurationValue;
    private Configuration configuration;
    private List<Interface> claimedInterfaces;


    WindowsUSBDevice(Object id, int vendorId, int productId, String manufacturer, String product, String serial,
                     int classCode, int subclassCode, int protocolCode, byte currentConfigurationValue) {
        super(id, vendorId, productId, manufacturer, product, serial, classCode, subclassCode, protocolCode);
        this.currentConfigurationValue = currentConfigurationValue;
    }

    @Override
    public boolean isOpen() {
        return device != null;
    }

    @Override
    public void open() {

        if (device != null)
            return;

        try (var session = MemorySession.openConfined()) {

            // open Windows device
            var pathSegment = Win.createSegmentFromString(id.toString(), session);
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

                try {
                    // read configuration
                    byte[] desc = getDescriptor(USBDescriptors.CONFIGURATION_DESCRIPTOR_TYPE, 0, 0);
                    configuration = DescriptorParser.parseConfigurationDescriptor(desc);

                } catch (Throwable e) {
                    WinUSB.WinUsb_Free(firstInterface);
                    throw e;
                }

            } catch (Throwable e) {
                Kernel32.CloseHandle(device);
                configuration = null;
                firstInterface = null;
                device = null;
                throw e;
            }
        }
    }


    @Override
    public void close() {
        if (device == null)
            return;

        WinUSB.WinUsb_Free(firstInterface);
        firstInterface = null;
        Kernel32.CloseHandle(device);
        device = null;
        configuration = null;
    }

    public void claimInterface(int interfaceNumber) {
        var intfOptional = configuration.interfaces.stream().filter(intf -> intf.number == interfaceNumber).findFirst();
        if (intfOptional.isEmpty())
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));

        if (claimedInterfaces == null)
            claimedInterfaces = new ArrayList<>();

        claimedInterfaces.add(intfOptional.get());
    }

    public void releaseInterface(int interfaceNumber) {
        var intfOptional = claimedInterfaces.stream().filter(intf -> intf.number == interfaceNumber).findFirst();
        if (intfOptional.isEmpty())
            throw new USBException(String.format("Interface has not been claimed or is invalid: number %d",
                    interfaceNumber));

        claimedInterfaces.remove(intfOptional.get());
    }

    private byte checkEndpointNumber(int endpointNumber, USBDirection direction) {
        byte endpointAddress = (byte) ((direction.ordinal() << 7) | endpointNumber);
        if (endpointNumber >= 1 && endpointNumber <= 127 && claimedInterfaces != null) {
            for (var intf : claimedInterfaces) {
                for (var ep : intf.endpoints) {
                    if (ep.address == endpointAddress)
                        return endpointAddress;
                }
            }
        }

        throw new USBException(String.format("Endpoint number %d is not part of a claimed interface, the endpoint " +
                "does not operate " + "in the %s direction or is otherwise invalid", endpointNumber, direction.name()));
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
        byte endpointAddress = checkEndpointNumber(endpointNumber, USBDirection.OUT);

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
        byte endpointAddress = checkEndpointNumber(endpointNumber, USBDirection.IN);

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
