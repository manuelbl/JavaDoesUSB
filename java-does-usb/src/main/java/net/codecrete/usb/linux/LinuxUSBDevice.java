//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBControlTransfer;
import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.USBDeviceImpl;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class LinuxUSBDevice extends USBDeviceImpl {

    private final int fd;

    LinuxUSBDevice(String path, USBDeviceInfo info) {
        super(path, info);

        fd = IO.open(path, IO.O_RDWR | IO.O_CLOEXEC);
        if (fd == -1)
            throw new USBException("Cannot open USB device", IO.getErrno());
    }

    public void claimInterface(int interfaceNumber) {
        try (var session = MemorySession.openConfined()) {
            var intfNumSegment = session.allocate(JAVA_INT, interfaceNumber);
            int ret = IO.ioctl(fd, USBDevFS.CLAIMINTERFACE, intfNumSegment);
            if (ret != 0)
                throw new USBException("Cannot claim USB interface", IO.getErrno());
        }
    }

    public void releaseInterface(int interfaceNumber) {
        try (var session = MemorySession.openConfined()) {
            var intfNumSegment = session.allocate(JAVA_INT, interfaceNumber);
            int ret = IO.ioctl(fd, USBDevFS.RELEASEINTERFACE, intfNumSegment);
            if (ret != 0)
                throw new USBException("Cannot release USB interface", IO.getErrno());
        }
    }

    private MemorySegment createCtrlTransfer(MemorySession session, USBDirection direction, USBControlTransfer setup, MemorySegment data) {
        var ctrlTransfer = session.allocate(USBDevFS.ctrltransfer$Struct);
        var bmRequest = (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        USBDevFS.ctrltransfer_bRequestType.set(ctrlTransfer, (byte) bmRequest);
        USBDevFS.ctrltransfer_bRequest.set(ctrlTransfer, setup.request());
        USBDevFS.ctrltransfer_wValue.set(ctrlTransfer, setup.value());
        USBDevFS.ctrltransfer_wIndex.set(ctrlTransfer, setup.index());
        USBDevFS.ctrltransfer_wLength.set(ctrlTransfer, (short) data.byteSize());
        USBDevFS.ctrltransfer_data.set(ctrlTransfer, data.address());
        return ctrlTransfer;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var session = MemorySession.openConfined()) {
            var data = session.allocate(length);
            var ctrlTransfer = createCtrlTransfer(session, USBDirection.IN, setup, data);

            int res = IO.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer);
            if (res < 0)
                throw new USBException("Control IN transfer failed", res);

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

            int res = IO.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer);
            if (res < 0)
                throw new USBException("Control OUT transfer failed", res);
        }
    }

    private MemorySegment createBulkTransfer(MemorySession session, int endpointAddress, MemorySegment data) {
        var transfer = session.allocate(USBDevFS.bulktransfer$Struct);
        USBDevFS.bulktransfer_ep.set(transfer, endpointAddress);
        USBDevFS.bulktransfer_len.set(transfer, (int) data.byteSize());
        USBDevFS.bulktransfer_data.set(transfer, data.address());
        return transfer;
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data) {
        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var transfer = createBulkTransfer(session, endpointNumber, buffer);

            int res = IO.ioctl(fd, USBDevFS.BULK, transfer);
            if (res < 0)
                throw new USBException(String.format("USB OUT transfer on endpoint %d failed", endpointNumber), res);
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int maxLength) {
        try (var session = MemorySession.openConfined()) {
            var buffer = session.allocate(maxLength);

            var transfer = createBulkTransfer(session, 0x80 | endpointNumber, buffer);

            int res = IO.ioctl(fd, USBDevFS.BULK, transfer);
            if (res < 0)
                throw new USBException(String.format("USB IN transfer on endpoint %d failed", endpointNumber), res);

            return buffer.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void close() throws Exception {
        IO.close(fd);
    }
}
