//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.*;
import net.codecrete.usb.common.*;
import net.codecrete.usb.linux.gen.errno.errno;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_setinterface;
import net.codecrete.usb.macos.MacosUSBException;
import net.codecrete.usb.usbstandard.DeviceDescriptor;
import net.codecrete.usb.linux.gen.fcntl.fcntl;
import net.codecrete.usb.linux.gen.ioctl.ioctl;
import net.codecrete.usb.linux.gen.unistd.unistd;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_bulktransfer;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_ctrltransfer;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class LinuxUSBDevice extends USBDeviceImpl {

    private int fd = -1;

    LinuxUSBDevice(Object id, int vendorId, int productId) {
        super(id, vendorId, productId);
        loadDescription((String) id);
    }

    private void loadDescription(String path) {
        byte[] descriptors;
        try {
            descriptors = Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            throw new USBException("Cannot read configuration descriptor", e);
        }

        // `descriptors` contains the device descriptor followed by the configuration descriptor
        // (including the interface descriptors, endpoint descriptors etc.)

        try (var session = MemorySession.openConfined()) {
            var descriptorsSegment = MemorySegment.ofArray(descriptors);

            // split off device descriptor (and copy it to fix alignment issues)
            var deviceDesc = session.allocate(DeviceDescriptor.LAYOUT);
            deviceDesc.copyFrom(descriptorsSegment.asSlice(0, DeviceDescriptor.LAYOUT.byteSize()));

            setFromDeviceDescriptor(deviceDesc);

            // skip to configuration descriptor
            var configDesc = session.allocateArray(JAVA_BYTE, descriptors.length - 18);
            configDesc.copyFrom(descriptorsSegment.asSlice(DeviceDescriptor.LAYOUT.byteSize()));
            setConfigurationDescriptor(configDesc);
        }
    }

    @Override
    public boolean isOpen() {
        return fd != -1;
    }

    @Override
    public void open() {
        if (isOpen())
            throw new USBException("the device is already open");

        try (var session = MemorySession.openConfined()) {
            var pathUtf8 = session.allocateUtf8String(id_.toString());
            fd = fcntl.open(pathUtf8, fcntl.O_RDWR() | fcntl.O_CLOEXEC());
            if (fd == -1)
                throw new USBException("Cannot open USB device", IO.getErrno());
        }
    }

    @Override
    public void close() {
        if (!isOpen())
            return;

        for (var intf : interfaces_)
            ((USBInterfaceImpl) intf).setClaimed(false);

        unistd.close(fd);
        fd = -1;
    }

    public void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (intf.isClaimed())
            throw new USBException(String.format("Interface %d has already been claimed", interfaceNumber));

        try (var session = MemorySession.openConfined()) {
            var intfNumSegment = session.allocate(JAVA_INT, interfaceNumber);
            int ret = ioctl.ioctl(fd, USBDevFS.CLAIMINTERFACE, intfNumSegment.address());
            if (ret != 0)
                throw new USBException("Cannot claim USB interface", IO.getErrno());
            setClaimed(interfaceNumber, true);
        }
    }

    @Override
    public void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (!intf.isClaimed())
            throw new USBException(String.format("Interface %d has not been claimed", interfaceNumber));

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        if (altSetting == null)
            throw new MacosUSBException(String.format("Interface %d does not have an alternate interface setting %d",
                    interfaceNumber, alternateNumber));

        try (var session = MemorySession.openConfined()) {
            var setIntfSegment = session.allocate(usbdevfs_setinterface.$LAYOUT());
            usbdevfs_setinterface.interface_$set(setIntfSegment, interfaceNumber);
            usbdevfs_setinterface.altsetting$set(setIntfSegment, alternateNumber);
            int ret = ioctl.ioctl(fd, USBDevFS.SETINTERFACE, setIntfSegment.address());
            if (ret != 0)
                throw new USBException("Failed to set alternate interface", IO.getErrno());
        }

        intf.setAlternate(altSetting);
    }

    public void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throw new USBException(String.format("Invalid interface number: %d", interfaceNumber));
        if (!intf.isClaimed())
            throw new USBException(String.format("Interface %d has not been claimed", interfaceNumber));

        try (var session = MemorySession.openConfined()) {
            var intfNumSegment = session.allocate(JAVA_INT, interfaceNumber);
            int ret = ioctl.ioctl(fd, USBDevFS.RELEASEINTERFACE, intfNumSegment.address());
            if (ret != 0)
                throw new USBException("Cannot release USB interface", IO.getErrno());
            setClaimed(interfaceNumber, false);
        }
    }

    private MemorySegment createCtrlTransfer(MemorySession session, USBDirection direction, USBControlTransfer setup,
                                             MemorySegment data) {
        var ctrlTransfer = session.allocate(usbdevfs_ctrltransfer.$LAYOUT());
        var bmRequest =
                (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        usbdevfs_ctrltransfer.bRequestType$set(ctrlTransfer, (byte) bmRequest);
        usbdevfs_ctrltransfer.bRequest$set(ctrlTransfer, (byte) setup.request());
        usbdevfs_ctrltransfer.wValue$set(ctrlTransfer, (short) setup.value());
        usbdevfs_ctrltransfer.wIndex$set(ctrlTransfer, (short) setup.index());
        usbdevfs_ctrltransfer.wLength$set(ctrlTransfer, (short) data.byteSize());
        usbdevfs_ctrltransfer.data$set(ctrlTransfer, data.address());
        return ctrlTransfer;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var session = MemorySession.openConfined()) {
            var data = session.allocate(length);
            var ctrlTransfer = createCtrlTransfer(session, USBDirection.IN, setup, data);

            int res = ioctl.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer.address());
            if (res < 0)
                throw new USBException("Control IN transfer failed", IO.getErrno());

            return data.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var session = MemorySession.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var buffer = session.allocate(dataLength);
            if (dataLength != 0)
                buffer.copyFrom(MemorySegment.ofArray(data));
            var ctrlTransfer = createCtrlTransfer(session, USBDirection.OUT, setup, buffer);

            int res = ioctl.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer.address());
            if (res < 0)
                throw new USBException("Control OUT transfer failed", IO.getErrno());
        }
    }

    private MemorySegment createBulkTransfer(MemorySession session, byte endpointAddress, MemorySegment data, int timeout) {
        var transfer = session.allocate(usbdevfs_bulktransfer.$LAYOUT());
        usbdevfs_bulktransfer.ep$set(transfer, 255 & endpointAddress);
        usbdevfs_bulktransfer.len$set(transfer, (int) data.byteSize());
        usbdevfs_bulktransfer.data$set(transfer, data.address());
        usbdevfs_bulktransfer.timeout$set(transfer, timeout);
        return transfer;
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {
        var endpointAddress = getEndpointAddress(endpointNumber, USBDirection.OUT,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var transfer = createBulkTransfer(session, endpointAddress, buffer, timeout);

            int res = ioctl.ioctl(fd, USBDevFS.BULK, transfer.address());
            if (res < 0) {
                int err = IO.getErrno();
                if (err == errno.ETIMEDOUT())
                    throw new TimeoutException("Transfer out aborted due to timeout");
                throw new USBException(String.format("USB OUT transfer on endpoint %d failed", endpointNumber), err);
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength, int timeout) {
        var endpointAddress = getEndpointAddress(endpointNumber, USBDirection.IN,
                USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(maxLength);

            var transfer = createBulkTransfer(session, endpointAddress, buffer, timeout);

            int res = ioctl.ioctl(fd, USBDevFS.BULK, transfer.address());
            if (res < 0) {
                int err = IO.getErrno();
                if (err == errno.ETIMEDOUT())
                    throw new TimeoutException("Transfer in aborted due to timeout");
                throw new USBException(String.format("USB IN transfer on endpoint %d failed", endpointNumber), err);
            }

            return buffer.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }
}
