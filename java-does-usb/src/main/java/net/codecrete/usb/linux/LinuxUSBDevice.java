//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.*;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBInterfaceImpl;
import net.codecrete.usb.linux.gen.errno.errno;
import net.codecrete.usb.linux.gen.fcntl.fcntl;
import net.codecrete.usb.linux.gen.unistd.unistd;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_bulktransfer;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_ctrltransfer;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_setinterface;
import net.codecrete.usb.usbstandard.DeviceDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.linux.LinuxUSBException.throwException;
import static net.codecrete.usb.linux.LinuxUSBException.throwLastError;

public class LinuxUSBDevice extends USBDeviceImpl {

    private int fd = -1;

    private final LinuxAsyncTask asyncTask;

    LinuxUSBDevice(Object id, int vendorId, int productId) {
        super(id, vendorId, productId);
        asyncTask = LinuxAsyncTask.instance();
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
        var descriptorsSegment = MemorySegment.ofArray(descriptors);
        setFromDeviceDescriptor(descriptorsSegment);
        setConfigurationDescriptor(descriptorsSegment.asSlice(DeviceDescriptor.LAYOUT.byteSize()));
    }

    @Override
    public boolean isOpen() {
        return fd != -1;
    }

    @Override
    public synchronized void open() {
        if (isOpen())
            throwException("the device is already open");

        try (var arena = Arena.openConfined()) {
            var pathUtf8 = arena.allocateUtf8String(id_.toString());
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            fd = IO.open(pathUtf8, fcntl.O_RDWR() | fcntl.O_CLOEXEC(), errnoState);
            if (fd == -1)
                throwLastError(errnoState, "Cannot open USB device");
            asyncTask.addForAsyncIOCompletion(this);
        }
    }

    @Override
    public synchronized void close() {
        if (!isOpen())
            return;

        asyncTask.removeFromAsyncIOCompletion(this);

        for (var intf : interfaces_)
            ((USBInterfaceImpl) intf).setClaimed(false);

        unistd.close(fd);
        fd = -1;
    }

    int fileDescriptor() {
        return fd;
    }

    public synchronized void claimInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throwException("Invalid interface number: %d", interfaceNumber);
        if (intf.isClaimed())
            throwException("Interface %d has already been claimed", interfaceNumber);

        try (var arena = Arena.openConfined()) {
            var intfNumSegment = arena.allocate(JAVA_INT, interfaceNumber);
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int ret = IO.ioctl(fd, USBDevFS.CLAIMINTERFACE, intfNumSegment, errnoState);
            if (ret != 0)
                throwLastError(errnoState, "Cannot claim USB interface");
            setClaimed(interfaceNumber, true);
        }
    }

    @Override
    public synchronized void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throwException("Invalid interface number: %d", interfaceNumber);
        if (!intf.isClaimed())
            throwException("Interface %d has not been claimed", interfaceNumber);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);
        if (altSetting == null)
            throwException("Interface %d does not have an alternate interface setting %d", interfaceNumber,
                    alternateNumber);

        try (var arena = Arena.openConfined()) {
            var setIntfSegment = arena.allocate(usbdevfs_setinterface.$LAYOUT());
            usbdevfs_setinterface.interface_$set(setIntfSegment, interfaceNumber);
            usbdevfs_setinterface.altsetting$set(setIntfSegment, alternateNumber);
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int ret = IO.ioctl(fd, USBDevFS.SETINTERFACE, setIntfSegment, errnoState);
            if (ret != 0)
                throwLastError(errnoState, "Failed to set alternate interface");
        }

        intf.setAlternate(altSetting);
    }

    public synchronized void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        var intf = getInterface(interfaceNumber);
        if (intf == null)
            throwException("Invalid interface number: %d", interfaceNumber);
        if (!intf.isClaimed())
            throwException("Interface %d has not been claimed", interfaceNumber);

        try (var arena = Arena.openConfined()) {
            var intfNumSegment = arena.allocate(JAVA_INT, interfaceNumber);
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int ret = IO.ioctl(fd, USBDevFS.RELEASEINTERFACE, intfNumSegment, errnoState);
            if (ret != 0)
                throwLastError(errnoState, "Cannot release USB interface");
            setClaimed(interfaceNumber, false);
        }
    }

    private MemorySegment createCtrlTransfer(Arena arena, USBDirection direction, USBControlTransfer setup,
                                             MemorySegment data) {
        var ctrlTransfer = arena.allocate(usbdevfs_ctrltransfer.$LAYOUT());
        var bmRequest =
                (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        usbdevfs_ctrltransfer.bRequestType$set(ctrlTransfer, (byte) bmRequest);
        usbdevfs_ctrltransfer.bRequest$set(ctrlTransfer, (byte) setup.request());
        usbdevfs_ctrltransfer.wValue$set(ctrlTransfer, (short) setup.value());
        usbdevfs_ctrltransfer.wIndex$set(ctrlTransfer, (short) setup.index());
        usbdevfs_ctrltransfer.wLength$set(ctrlTransfer, (short) data.byteSize());
        usbdevfs_ctrltransfer.data$set(ctrlTransfer, data);
        return ctrlTransfer;
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var arena = Arena.openConfined()) {
            var data = arena.allocate(length);
            var ctrlTransfer = createCtrlTransfer(arena, USBDirection.IN, setup, data);

            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int res = IO.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer, errnoState);
            if (res < 0)
                throwLastError(errnoState, "Control IN transfer failed");

            return data.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var arena = Arena.openConfined()) {
            int dataLength = data != null ? data.length : 0;
            var buffer = arena.allocate(dataLength);
            if (dataLength != 0)
                buffer.copyFrom(MemorySegment.ofArray(data));
            var ctrlTransfer = createCtrlTransfer(arena, USBDirection.OUT, setup, buffer);

            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int res = IO.ioctl(fd, USBDevFS.CONTROL, ctrlTransfer, errnoState);
            if (res < 0)
                throwLastError(errnoState, "Control OUT transfer failed");
        }
    }

    private MemorySegment createBulkTransfer(Arena arena, byte endpointAddress, MemorySegment data, int timeout) {
        var transfer = arena.allocate(usbdevfs_bulktransfer.$LAYOUT());
        usbdevfs_bulktransfer.ep$set(transfer, 255 & endpointAddress);
        usbdevfs_bulktransfer.len$set(transfer, (int) data.byteSize());
        usbdevfs_bulktransfer.data$set(transfer, data);
        usbdevfs_bulktransfer.timeout$set(transfer, timeout);
        return transfer;
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int timeout) {
        var endpoint = getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var buffer = arena.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data));
            var transfer = createBulkTransfer(arena, endpoint.endpointAddress(), buffer, timeout);

            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int res = IO.ioctl(fd, USBDevFS.BULK, transfer, errnoState);
            if (res < 0) {
                int err = Linux.getErrno(errnoState);
                if (err == errno.ETIMEDOUT())
                    throw new USBTimeoutException("Transfer out aborted due to timeout");
                throwLastError(errnoState, "USB OUT transfer on endpoint %d failed", endpointNumber);
            }
        }
    }

    @Override
    public byte[] transferIn(int endpointNumber, int timeout) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var buffer = arena.allocate(endpoint.packetSize());

            var transfer = createBulkTransfer(arena, endpoint.endpointAddress(), buffer, timeout);

            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int res = IO.ioctl(fd, USBDevFS.BULK, transfer, errnoState);
            if (res < 0) {
                int err = Linux.getErrno(errnoState);
                if (err == errno.ETIMEDOUT())
                    throw new USBTimeoutException("Transfer in aborted due to timeout");
                throwLastError(errnoState, "USB IN transfer on endpoint %d failed", endpointNumber);
            }

            return buffer.asSlice(0, res).toArray(JAVA_BYTE);
        }
    }

    @Override
    protected Transfer createTransfer() {
        return new LinuxTransfer();
    }

    @Override
    protected void throwOSException(int errorCode, String message, Object... args) {
        throwException(errorCode, message, args);
    }

    @Override
    public void clearHalt(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);

        try (var arena = Arena.openConfined()) {
            var endpointAddrSegment = arena.allocate(JAVA_INT, endpoint.endpointAddress() & 0xff);
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            int res = IO.ioctl(fd, USBDevFS.CLEAR_HALT, endpointAddrSegment, errnoState);
            if (res < 0)
                throwLastError(errnoState, "Clearing halt failed");
        }
    }

    @Override
    public synchronized void abortTransfers(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);

        asyncTask.abortTransfers(this, endpoint.endpointAddress());
    }

    @Override
    public InputStream openInputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, null);

        return new LinuxEndpointInputStream(this, endpointNumber);
    }

    @Override
    public OutputStream openOutputStream(int endpointNumber) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, null);

        return new LinuxEndpointOutputStream(this, endpointNumber);
    }

    synchronized void submitTransferIn(int endpointNumber, LinuxTransfer transfer) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        asyncTask.submitBulkTransfer(this, endpoint.endpointAddress(), transfer);
    }

    synchronized void submitTransferOut(int endpointNumber, LinuxTransfer transfer) {
        var endpoint = getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        asyncTask.submitBulkTransfer(this, endpoint.endpointAddress(), transfer);
    }
}
