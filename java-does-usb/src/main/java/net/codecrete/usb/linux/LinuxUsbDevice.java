//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.UsbControlTransfer;
import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbTransferType;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.UsbDeviceImpl;
import net.codecrete.usb.common.UsbInterfaceImpl;
import net.codecrete.usb.linux.gen.fcntl.fcntl;
import net.codecrete.usb.linux.gen.unistd.unistd;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_disconnect_claim;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_ioctl;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_setinterface;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs;
import net.codecrete.usb.usbstandard.DeviceDescriptor;
import net.codecrete.usb.usbstandard.SetupPacket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.linux.Linux.allocateErrorState;
import static net.codecrete.usb.linux.LinuxUsbException.throwException;
import static net.codecrete.usb.linux.LinuxUsbException.throwLastError;

@SuppressWarnings("java:S2160")
public class LinuxUsbDevice extends UsbDeviceImpl {

    private static final MemorySegment DRIVER_NAME_USBFS = Arena.global().allocateFrom("usbfs");

    private int fd = -1;

    private final LinuxAsyncTask asyncTask;

    private boolean detachDrivers = false;

    LinuxUsbDevice(Object id, int vendorId, int productId) {
        super(id, vendorId, productId);
        asyncTask = LinuxAsyncTask.INSTANCE;
        loadDescription((String) id);
    }

    private void loadDescription(String path) {
        byte[] descriptors;
        try {
            descriptors = Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            throw new UsbException("reading configuration descriptor failed", e);
        }

        // `descriptors` contains the device descriptor followed by the configuration descriptor
        // (including the interface descriptors, endpoint descriptors etc.)
        var descriptorsSegment = MemorySegment.ofArray(descriptors);
        setFromDeviceDescriptor(descriptorsSegment.asSlice(0, DeviceDescriptor.LAYOUT));
        setConfigurationDescriptor(descriptorsSegment.asSlice(DeviceDescriptor.LAYOUT.byteSize()));
    }

    @Override
    public void detachStandardDrivers() {
        if (isOpened())
            throwException("detachStandardDrivers() must not be called while the device is open");
        detachDrivers = true;
    }

    @Override
    public void attachStandardDrivers() {
        if (isOpened())
            throwException("attachStandardDrivers() must not be called while the device is open");
        detachDrivers = false;
    }

    @Override
    public boolean isOpened() {
        return fd != -1;
    }

    @Override
    public synchronized void open() {
        if (isOpened())
            throwException("device is already open");

        try (var arena = Arena.ofConfined()) {
            var pathUtf8 = arena.allocateFrom(uniqueDeviceId.toString());
            var errorState = allocateErrorState(arena);
            fd = IO.open(pathUtf8, fcntl.O_RDWR() | fcntl.O_CLOEXEC(), errorState);
            if (fd == -1)
                throwLastError(errorState, "opening USB device failed");
            asyncTask.addForAsyncIOCompletion(this);
        }
    }

    @Override
    public synchronized void close() {
        if (!isOpened())
            return;

        asyncTask.removeFromAsyncIOCompletion(this);

        for (var intf : interfaceList)
            ((UsbInterfaceImpl) intf).setClaimed(false);

        unistd.close(fd);
        fd = -1;
    }

    int fileDescriptor() {
        return fd;
    }

    public synchronized void claimInterface(int interfaceNumber) {
        checkIsOpen();

        getInterfaceWithCheck(interfaceNumber, false);

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            int ret;

            if (detachDrivers) {
                // claim interface (detaching kernel driver)
                var disconnectClaim = usbdevfs_disconnect_claim.allocate(arena);
                usbdevfs_disconnect_claim.interface_(disconnectClaim, interfaceNumber);
                usbdevfs_disconnect_claim.flags(disconnectClaim, usbdevice_fs.USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER());
                usbdevfs_disconnect_claim.driver(disconnectClaim).copyFrom(DRIVER_NAME_USBFS);
                ret = IO.ioctl(fd, UsbDevFS.DISCONNECT_CLAIM, disconnectClaim, errorState);

            } else {
                // claim interface (without detaching kernel driver)
                var intfNumSegment = arena.allocate(JAVA_INT);
                intfNumSegment.setAtIndex(JAVA_INT, 0, interfaceNumber);
                ret = IO.ioctl(fd, UsbDevFS.CLAIMINTERFACE, intfNumSegment, errorState);
            }

            if (ret != 0)
                throwLastError(errorState, "claiming USB interface failed");

            setClaimed(interfaceNumber, true);
        }
    }

    @Override
    public synchronized void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        checkIsOpen();

        var intf = getInterfaceWithCheck(interfaceNumber, true);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);

        try (var arena = Arena.ofConfined()) {
            var setIntfSegment = usbdevfs_setinterface.allocate(arena);
            usbdevfs_setinterface.interface_(setIntfSegment, interfaceNumber);
            usbdevfs_setinterface.altsetting(setIntfSegment, alternateNumber);
            var errorState = allocateErrorState(arena);
            var ret = IO.ioctl(fd, UsbDevFS.SETINTERFACE, setIntfSegment, errorState);
            if (ret != 0)
                throwLastError(errorState, "setting alternate interface failed");
        }

        intf.setAlternate(altSetting);
    }

    public synchronized void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        getInterfaceWithCheck(interfaceNumber, true);

        try (var arena = Arena.ofConfined()) {
            var intfNumSegment = arena.allocate(JAVA_INT);
            intfNumSegment.setAtIndex(JAVA_INT, 0, interfaceNumber);
            var errorState = allocateErrorState(arena);
            var ret = IO.ioctl(fd, UsbDevFS.RELEASEINTERFACE, intfNumSegment, errorState);
            if (ret != 0)
                throwLastError(errorState, "releasing USB interface failed");

            setClaimed(interfaceNumber, false);

            if (detachDrivers) {
                // reattach kernel driver
                var request = usbdevfs_ioctl.allocate(arena);
                usbdevfs_ioctl.ifno(request, interfaceNumber);
                usbdevfs_ioctl.ioctl_code(request, UsbDevFS.CONNECT);
                usbdevfs_ioctl.data(request, MemorySegment.NULL);
                IO.ioctl(fd, UsbDevFS.IOCTL, request, errorState);
            }
        }
    }

    @Override
    public void controlTransferOut(@NotNull UsbControlTransfer setup, byte[] data) {
        try (var arena = Arena.ofConfined()) {
            var dataLength = data != null ? data.length : 0;
            var transfer = createSyncCtrlTransfer(arena, UsbDirection.OUT, setup, dataLength);
            if (dataLength != 0)
                transfer.data().asSlice(8).copyFrom(MemorySegment.ofArray(data));

            synchronized (transfer) {
                submitTransfer(UsbDirection.OUT, 0, transfer);
                waitForTransfer(transfer, 0, UsbDirection.OUT, 0);
            }
        }
    }

    @Override
    public byte @NotNull [] controlTransferIn(@NotNull UsbControlTransfer setup, int length) {
        try (var arena = Arena.ofConfined()) {
            var transfer = createSyncCtrlTransfer(arena, UsbDirection.IN, setup, length);

            synchronized (transfer) {
                submitTransfer(UsbDirection.IN, 0, transfer);
                waitForTransfer(transfer, 0, UsbDirection.IN, 0);
            }

            return transfer.data().asSlice(8, transfer.resultSize()).toArray(JAVA_BYTE);
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
    private LinuxTransfer createSyncCtrlTransfer(Arena arena, UsbDirection direction, UsbControlTransfer setup,
                                                 int dataLength) {
        var bmRequest =
                (direction == UsbDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
        var buffer = arena.allocate(8L + dataLength, 8);
        var setupPacket = new SetupPacket(buffer);
        setupPacket.setRequestType(bmRequest);
        setupPacket.setRequest(setup.request());
        setupPacket.setValue(setup.value());
        setupPacket.setIndex(setup.index());
        setupPacket.setLength(dataLength);

        var transfer = new LinuxTransfer();
        transfer.setData(buffer);
        transfer.setDataSize((int) buffer.byteSize());
        transfer.setResultSize(-1);
        transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);

        return transfer;
    }

    @Override
    public void transferOut(int endpointNumber, byte @NotNull [] data, int offset, int length, int timeout) {
        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(length);
            buffer.copyFrom(MemorySegment.ofArray(data).asSlice(offset, length));
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransfer(UsbDirection.OUT, endpointNumber, transfer);
                waitForTransfer(transfer, timeout, UsbDirection.OUT, endpointNumber);
            }
        }
    }

    @Override
    public byte @NotNull [] transferIn(int endpointNumber, int timeout) {
        var endpoint = getEndpoint(UsbDirection.IN, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);

        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(endpoint.packetSize());
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransfer(UsbDirection.IN, endpointNumber, transfer);
                waitForTransfer(transfer, timeout, UsbDirection.IN, endpointNumber);
            }

            return buffer.asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    private LinuxTransfer createSyncTransfer(MemorySegment data) {
        var transfer = new LinuxTransfer();
        transfer.setData(data);
        transfer.setDataSize((int) data.byteSize());
        transfer.setResultSize(-1);
        transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);
        return transfer;
    }

    synchronized void submitTransfer(UsbDirection direction, int endpointNumber, LinuxTransfer transfer) {
        if (endpointNumber != 0) {
            var endpoint = getEndpoint(direction, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);
            asyncTask.submitTransfer(this, endpoint.endpointAddress(), endpoint.transferType(), transfer);
        } else {
            asyncTask.submitTransfer(this, 0, UsbTransferType.CONTROL, transfer);
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
    public void clearHalt(UsbDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);

        try (var arena = Arena.ofConfined()) {
            var endpointAddrSegment = arena.allocate(JAVA_INT);
            endpointAddrSegment.setAtIndex(JAVA_INT, 0, endpoint.endpointAddress() & 0xff);
            var errorState = allocateErrorState(arena);
            var res = IO.ioctl(fd, UsbDevFS.CLEAR_HALT, endpointAddrSegment, errorState);
            if (res < 0)
                throwLastError(errorState, "clearing halt failed");
        }
    }

    @Override
    public synchronized void abortTransfers(UsbDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);

        asyncTask.abortTransfers(this, endpoint.endpointAddress());
    }

    @Override
    public synchronized @NotNull InputStream openInputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(UsbDirection.IN, endpointNumber, UsbTransferType.BULK, null);

        return new LinuxEndpointInputStream(this, endpointNumber, bufferSize);
    }

    @Override
    public synchronized @NotNull OutputStream openOutputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(UsbDirection.OUT, endpointNumber, UsbTransferType.BULK, null);

        return new LinuxEndpointOutputStream(this, endpointNumber, bufferSize);
    }
}
