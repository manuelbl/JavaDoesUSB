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
import net.codecrete.usb.USBTransferType;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBInterfaceImpl;
import net.codecrete.usb.linux.gen.fcntl.fcntl;
import net.codecrete.usb.linux.gen.unistd.unistd;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_disconnect_claim;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_ioctl;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_setinterface;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs;
import net.codecrete.usb.usbstandard.DeviceDescriptor;
import net.codecrete.usb.usbstandard.SetupPacket;

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

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class LinuxUSBDevice extends USBDeviceImpl {

    private int fd = -1;

    private final LinuxAsyncTask asyncTask;

    private boolean detachDrivers = false;

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
    public void detachStandardDrivers() {
        if (isOpen())
            throwException("detachStandardDrivers() must not be called while the device is open");
        detachDrivers = true;
    }

    @Override
    public void attachStandardDrivers() {
        if (isOpen())
            throwException("attachStandardDrivers() must not be called while the device is open");
        detachDrivers = false;
    }

    @Override
    public boolean isOpen() {
        return fd != -1;
    }

    @Override
    public synchronized void open() {
        if (isOpen())
            throwException("the device is already open");

        try (var arena = Arena.ofConfined()) {
            var pathUtf8 = arena.allocateUtf8String(uniqueDeviceId.toString());
            var errnoState = arena.allocate(Linux.ERRNO_STATE_LAYOUT);
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

        for (var intf : interfaceList)
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

        try (var arena = Arena.ofConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE_LAYOUT);
            int ret;

            if (detachDrivers) {
                // claim interface (detaching kernel driver)
                var disconnectClaim = usbdevfs_disconnect_claim.allocate(arena);
                usbdevfs_disconnect_claim.interface_$set(disconnectClaim, interfaceNumber);
                usbdevfs_disconnect_claim.flags$set(disconnectClaim, usbdevice_fs.USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER());
                byte[] driverName = {'u', 's', 'b', 'f', 's', 0};
                usbdevfs_disconnect_claim.driver$slice(disconnectClaim).copyFrom(MemorySegment.ofArray(driverName));
                ret = IO.ioctl(fd, USBDevFS.DISCONNECT_CLAIM, disconnectClaim, errnoState);

            } else {
                // claim interface (without detaching kernel driver)
                var intfNumSegment = arena.allocate(JAVA_INT, interfaceNumber);
                ret = IO.ioctl(fd, USBDevFS.CLAIMINTERFACE, intfNumSegment, errnoState);
            }

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

        try (var arena = Arena.ofConfined()) {
            var setIntfSegment = arena.allocate(usbdevfs_setinterface.$LAYOUT());
            usbdevfs_setinterface.interface_$set(setIntfSegment, interfaceNumber);
            usbdevfs_setinterface.altsetting$set(setIntfSegment, alternateNumber);
            var errnoState = arena.allocate(Linux.ERRNO_STATE_LAYOUT);
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

        try (var arena = Arena.ofConfined()) {
            var intfNumSegment = arena.allocate(JAVA_INT, interfaceNumber);
            var errnoState = arena.allocate(Linux.ERRNO_STATE_LAYOUT);
            int ret = IO.ioctl(fd, USBDevFS.RELEASEINTERFACE, intfNumSegment, errnoState);
            if (ret != 0)
                throwLastError(errnoState, "Cannot release USB interface");

            setClaimed(interfaceNumber, false);

            if (detachDrivers) {
                // reattach kernel driver
                var request = usbdevfs_ioctl.allocate(arena);
                usbdevfs_ioctl.ifno$set(request, interfaceNumber);
                usbdevfs_ioctl.ioctl_code$set(request, USBDevFS.CONNECT);
                usbdevfs_ioctl.data$set(request, MemorySegment.NULL);
                IO.ioctl(fd, USBDevFS.IOCTL, request, errnoState);
            }
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
        try (var arena = Arena.ofConfined()) {
            int dataLength = data != null ? data.length : 0;
            var transfer = createSyncCtrlTransfer(arena, USBDirection.OUT, setup, dataLength);
            if (dataLength != 0)
                transfer.data.asSlice(8).copyFrom(MemorySegment.ofArray(data));

            synchronized (transfer) {
                submitTransfer(USBDirection.OUT, 0, transfer);
                waitForTransfer(transfer, 0, USBDirection.OUT, 0);
            }
        }
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var arena = Arena.ofConfined()) {
            var transfer = createSyncCtrlTransfer(arena, USBDirection.IN, setup, length);

            synchronized (transfer) {
                submitTransfer(USBDirection.IN, 0, transfer);
                waitForTransfer(transfer, 0, USBDirection.IN, 0);
            }

            return transfer.data.asSlice(8, transfer.resultSize).toArray(JAVA_BYTE);
        }
    }

    /**
     * Create transfer object for synchronous control request.
     *
     * @param arena      arena for allocating memory
     * @param direction  direction
     * @param setup      setup data
     * @param dataLength data length (in addition to setup data)
     * @return transfer object
     */
    private LinuxTransfer createSyncCtrlTransfer(Arena arena, USBDirection direction, USBControlTransfer setup,
                                                 int dataLength) {
        var bmRequest =
                (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        var buffer = arena.allocate(8 + dataLength, 8);
        var setupPacket = new SetupPacket(buffer);
        setupPacket.setRequestType(bmRequest);
        setupPacket.setRequest(setup.request());
        setupPacket.setValue(setup.value());
        setupPacket.setIndex(setup.index());
        setupPacket.setLength(dataLength);

        var transfer = new LinuxTransfer();
        transfer.data = buffer;
        transfer.dataSize = (int) buffer.byteSize();
        transfer.resultSize = -1;
        transfer.completion = USBDeviceImpl::onSyncTransferCompleted;

        return transfer;
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int offset, int length, int timeout) {
        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(length);
            buffer.copyFrom(MemorySegment.ofArray(data).asSlice(offset, length));
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransfer(USBDirection.OUT, endpointNumber, transfer);
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
                submitTransfer(USBDirection.IN, endpointNumber, transfer);
                waitForTransfer(transfer, timeout, USBDirection.IN, endpointNumber);
            }

            return buffer.asSlice(0, transfer.resultSize).toArray(JAVA_BYTE);
        }
    }

    private LinuxTransfer createSyncTransfer(MemorySegment data) {
        var transfer = new LinuxTransfer();
        transfer.data = data;
        transfer.dataSize = (int) data.byteSize();
        transfer.resultSize = -1;
        transfer.completion = USBDeviceImpl::onSyncTransferCompleted;
        return transfer;
    }

    synchronized void submitTransfer(USBDirection direction, int endpointNumber, LinuxTransfer transfer) {
        if (endpointNumber != 0) {
            var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
            asyncTask.submitTransfer(this, endpoint.endpointAddress(), endpoint.transferType(), transfer);
        } else {
            asyncTask.submitTransfer(this, 0, USBTransferType.CONTROL, transfer);
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

        try (var arena = Arena.ofConfined()) {
            var endpointAddrSegment = arena.allocate(JAVA_INT, endpoint.endpointAddress() & 0xff);
            var errnoState = arena.allocate(Linux.ERRNO_STATE_LAYOUT);
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
    public synchronized InputStream openInputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, null);

        return new LinuxEndpointInputStream(this, endpointNumber, bufferSize);
    }

    @Override
    public synchronized OutputStream openOutputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, null);

        return new LinuxEndpointOutputStream(this, endpointNumber, bufferSize);
    }
}
