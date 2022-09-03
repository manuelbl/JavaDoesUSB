//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBControlTransfer;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.DescriptorParser;
import net.codecrete.usb.common.USBDeviceImpl;
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

    LinuxUSBDevice(Object id, int vendorId, int productId, String manufacturer, String product, String serial) {
        super(id, vendorId, productId, manufacturer, product, serial);
        loadDescription((String) id);
    }

    private void loadDescription(String path) {
        byte[] descriptors;
        try {
            descriptors = Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            throw new USBException("Cannot read configuration descriptor", e);
        }

        try (var session = MemorySession.openConfined()) {
            var descriptorsSegment = MemorySegment.ofArray(descriptors);
            // skip device descriptor
            var configDesc = session.allocateArray(JAVA_BYTE, descriptors.length - 18);
            configDesc.copyFrom(descriptorsSegment.asSlice(18));
            var configuration = DescriptorParser.parseConfigurationDescriptor(configDesc, getVendorId(), getProductId());
            setInterfaces(configuration.interfaces);
        }
    }

    @Override
    public boolean isOpen() {
        return fd != -1;
    }

    @Override
    public void open() {
        if (fd != -1) return;

        try (var session = MemorySession.openConfined()) {
            var pathUtf8 = session.allocateUtf8String(id.toString());
            fd = fcntl.open(pathUtf8, fcntl.O_RDWR() | fcntl.O_CLOEXEC());
            if (fd == -1) throw new USBException("Cannot open USB device", IO.getErrno());
        }
    }

    @Override
    public void close() {
        if (fd == -1) return;

        unistd.close(fd);
        fd = -1;
    }

    public void claimInterface(int interfaceNumber) {
        try (var session = MemorySession.openConfined()) {
            var intfNumSegment = session.allocate(JAVA_INT, interfaceNumber);
            int ret = ioctl.ioctl(fd, USBDevFS.CLAIMINTERFACE, intfNumSegment.address());
            if (ret != 0) throw new USBException("Cannot claim USB interface", IO.getErrno());
        }
    }

    public void releaseInterface(int interfaceNumber) {
        try (var session = MemorySession.openConfined()) {
            var intfNumSegment = session.allocate(JAVA_INT, interfaceNumber);
            int ret = ioctl.ioctl(fd, USBDevFS.RELEASEINTERFACE, intfNumSegment.address());
            if (ret != 0) throw new USBException("Cannot release USB interface", IO.getErrno());
        }
    }

    private MemorySegment createCtrlTransfer(MemorySession session, USBDirection direction, USBControlTransfer setup,
                                             MemorySegment data) {
        var ctrlTransfer = session.allocate(usbdevfs_ctrltransfer.$LAYOUT());
        var bmRequest =
                (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        usbdevfs_ctrltransfer.bRequestType$set(ctrlTransfer, (byte) bmRequest);
        usbdevfs_ctrltransfer.bRequest$set(ctrlTransfer, setup.request());
        usbdevfs_ctrltransfer.wValue$set(ctrlTransfer, setup.value());
        usbdevfs_ctrltransfer.wIndex$set(ctrlTransfer, setup.index());
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
            if (res < 0) throw new USBException("Control IN transfer failed", res);

            return data.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var session = MemorySession.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var buffer = session.allocate(dataLength);
            if (dataLength != 0) buffer.copyFrom(MemorySegment.ofArray(data));
            var ctrlTransfer = createCtrlTransfer(session, USBDirection.OUT, setup, buffer);

            int res = ioctl.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer.address());
            if (res < 0) throw new USBException("Control OUT transfer failed", res);
        }
    }

    private MemorySegment createBulkTransfer(MemorySession session, int endpointAddress, MemorySegment data) {
        var transfer = session.allocate(usbdevfs_bulktransfer.$LAYOUT());
        usbdevfs_bulktransfer.ep$set(transfer, endpointAddress);
        usbdevfs_bulktransfer.len$set(transfer, (int) data.byteSize());
        usbdevfs_bulktransfer.data$set(transfer, data.address());
        return transfer;
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data) {
        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var transfer = createBulkTransfer(session, endpointNumber, buffer);

            int res = ioctl.ioctl(fd, USBDevFS.BULK, transfer.address());
            if (res < 0)
                throw new USBException(String.format("USB OUT transfer on endpoint %d failed", endpointNumber), res);
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength) {
        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(maxLength);

            var transfer = createBulkTransfer(session, 0x80 | endpointNumber, buffer);

            int res = ioctl.ioctl(fd, USBDevFS.BULK, transfer.address());
            if (res < 0)
                throw new USBException(String.format("USB IN transfer on endpoint %d failed", endpointNumber), res);

            return buffer.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }
}
