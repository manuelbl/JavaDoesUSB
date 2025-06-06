//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.UsbControlTransfer;
import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbRecipient;
import net.codecrete.usb.UsbTransferType;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.UsbDeviceImpl;
import net.codecrete.usb.usbstandard.SetupPacket;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static net.codecrete.usb.common.ForeignMemory.dereference;
import static net.codecrete.usb.windows.CustomApis.CloseHandle;
import static net.codecrete.usb.windows.CustomApis.WinUsb_ControlTransfer;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUsbException.throwException;
import static net.codecrete.usb.windows.WindowsUsbException.throwLastError;
import static windows.win32.devices.properties.Constants.DEVPKEY_Device_Children;
import static windows.win32.devices.properties.Constants.DEVPKEY_Device_HardwareIds;
import static windows.win32.devices.usb.Apis.WinUsb_AbortPipe;
import static windows.win32.devices.usb.Apis.WinUsb_Free;
import static windows.win32.devices.usb.Apis.WinUsb_GetAssociatedInterface;
import static windows.win32.devices.usb.Apis.WinUsb_Initialize;
import static windows.win32.devices.usb.Apis.WinUsb_ReadPipe;
import static windows.win32.devices.usb.Apis.WinUsb_ResetPipe;
import static windows.win32.devices.usb.Apis.WinUsb_SetCurrentAlternateSetting;
import static windows.win32.devices.usb.Apis.WinUsb_SetPipePolicy;
import static windows.win32.devices.usb.Apis.WinUsb_WritePipe;
import static windows.win32.devices.usb.WINUSB_PIPE_POLICY.PIPE_TRANSFER_TIMEOUT;
import static windows.win32.devices.usb.WINUSB_PIPE_POLICY.RAW_IO;
import static windows.win32.foundation.GENERIC_ACCESS_RIGHTS.GENERIC_READ;
import static windows.win32.foundation.GENERIC_ACCESS_RIGHTS.GENERIC_WRITE;
import static windows.win32.foundation.WIN32_ERROR.ERROR_INVALID_PARAMETER;
import static windows.win32.foundation.WIN32_ERROR.ERROR_IO_PENDING;
import static windows.win32.storage.filesystem.Apis.CreateFileW;
import static windows.win32.storage.filesystem.FILE_CREATION_DISPOSITION.OPEN_EXISTING;
import static windows.win32.storage.filesystem.FILE_FLAGS_AND_ATTRIBUTES.FILE_ATTRIBUTE_NORMAL;
import static windows.win32.storage.filesystem.FILE_FLAGS_AND_ATTRIBUTES.FILE_FLAG_OVERLAPPED;
import static windows.win32.storage.filesystem.FILE_SHARE_MODE.FILE_SHARE_READ;
import static windows.win32.storage.filesystem.FILE_SHARE_MODE.FILE_SHARE_WRITE;

/**
 * Windows implementation for USB device.
 */
@SuppressWarnings("java:S2160")
public class WindowsUsbDevice extends UsbDeviceImpl {

    private static final System.Logger LOG = System.getLogger(WindowsUsbDevice.class.getName());

    private final WindowsAsyncTask asyncTask;
    /**
     *  Indicates if the device is a composite device
     */
    private final boolean isComposite;

    private List<InterfaceHandle> interfaceHandles;

    // device paths by interface number (first interface of function)
    private Map<Integer, String> devicePaths;

    /**
     * Indicates if {@link #open()} has been called. Since separate interfaces can have separate underlying
     * Windows device, {@link #claimInterface(int)} instead of {@link #open()} will open the Windows device.
     */
    private boolean showAsOpen;

    WindowsUsbDevice(String devicePath, int vendorId, int productId, MemorySegment configDesc, boolean isComposite) {
        super(devicePath, vendorId, productId);
        asyncTask = WindowsAsyncTask.INSTANCE;
        this.isComposite = isComposite;
        if (isComposite)
            devicePaths = new HashMap<>();
        readDescription(configDesc);
    }

    private void readDescription(MemorySegment configDesc) {
        var configuration = setConfigurationDescriptor(configDesc);

        // build list of interface handles
        interfaceHandles = configuration.interfaces().stream()
                .map(intf -> {
                    var interfaceNumber = intf.getNumber();
                    var function = configuration.findFunction(interfaceNumber);
                    return new InterfaceHandle(interfaceNumber, function.firstInterfaceNumber());
                }).
                toList();
    }

    @Override
    public boolean isOpened() {
        return showAsOpen;
    }

    @Override
    public synchronized void open() {
        checkIsClosed("device is already open");
        showAsOpen = true;
    }

    @Override
    public synchronized void close() {
        if (!isOpened())
            return;

        for (var intf : interfaceList) {
            if (intf.isClaimed())
                releaseInterface(intf.getNumber());
        }

        showAsOpen = false;
    }

    public void claimInterface(int interfaceNumber) {
        // When a device is plugged in, a notification is sent. For composite devices, it is a notification
        // that the composite device is ready. Each composite function will be registered separately and
        // the related information will be available with a delay. So for composite functions, several
        // retries might be needed until the device path is available.
        var numRetries = 30; // 30 x 100ms
        while (true) {
            if (claimInterfaceSynchronized(interfaceNumber))
                return; // success

            numRetries -= 1;
            if (numRetries == 0)
                throw new UsbException("claiming interface failed (function has no device path / interface GUID, might be missing WinUSB driver)");

            // sleep and retry
            try {
                LOG.log(DEBUG, "Sleeping for 100ms...");
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressWarnings("java:S3776")
    private synchronized boolean claimInterfaceSynchronized(int interfaceNumber) {
        checkIsOpen();

        getInterfaceWithCheck(interfaceNumber, false);

        var intfHandle = getInterfaceHandle(interfaceNumber);
        var firstIntfHandle = intfHandle;
        if (intfHandle.firstInterfaceNumber != interfaceNumber)
            firstIntfHandle = getInterfaceHandle(intfHandle.firstInterfaceNumber);

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            // both the device and the first interface must be opened for any interface belonging to the same function
            if (firstIntfHandle.deviceHandle == null) {
                var devicePath = getInterfaceDevicePath(firstIntfHandle.interfaceNumber);
                if (devicePath == null)
                    return false; // retry later

                LOG.log(DEBUG, "opening device {0}", devicePath);

                // open Windows device if needed
                var pathSegment = arena.allocateFrom(devicePath, UTF_16LE);
                var deviceHandle = CreateFileW(errorState, pathSegment, GENERIC_WRITE | GENERIC_READ,
                        FILE_SHARE_WRITE | FILE_SHARE_READ, NULL, OPEN_EXISTING,
                        FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED, NULL);

                if (Win.isInvalidHandle(deviceHandle))
                    throwLastError(errorState, "claiming interface failed (opening USB device %s failed)", devicePath);

                try {
                    // open first interface
                    var interfaceHandleHolder = arena.allocate(ADDRESS);
                    if (WinUsb_Initialize(errorState, deviceHandle, interfaceHandleHolder) == 0) {
                        if (Win.getLastError(errorState) == ERROR_INVALID_PARAMETER)
                            throw new UsbException(
                                    "claiming interface failed (required WinUSB driver is probably not installed for the device)",
                                    ERROR_INVALID_PARAMETER
                            );
                        throwLastError(errorState, "claiming interface failed");
                    }
                    var interfaceHandle = dereference(interfaceHandleHolder);

                    firstIntfHandle.deviceHandle = deviceHandle;
                    firstIntfHandle.winusbHandle = interfaceHandle;
                    asyncTask.addDevice(deviceHandle);

                } catch (Exception e) {
                    CloseHandle(deviceHandle);
                    throw e;
                }
            }

            if (intfHandle != firstIntfHandle) {
                // open associated interface
                var interfaceHandleHolder = arena.allocate(ADDRESS);
                if (WinUsb_GetAssociatedInterface(errorState, firstIntfHandle.winusbHandle,
                        (byte) (intfHandle.interfaceNumber - firstIntfHandle.interfaceNumber - 1),
                        interfaceHandleHolder) == 0)
                    throwLastError(errorState, "claiming (associated) interface failed");
                intfHandle.winusbHandle = dereference(interfaceHandleHolder);
            }
        }

        firstIntfHandle.deviceOpenCount += 1;
        setClaimed(interfaceNumber, true);
        return true;
    }

    @Override
    public synchronized void selectAlternateSetting(int interfaceNumber, int alternateNumber) {
        checkIsOpen();

        var intf = getInterfaceWithCheck(interfaceNumber, true);
        var intfHandle = getInterfaceHandle(interfaceNumber);

        // check alternate setting
        var altSetting = intf.getAlternate(alternateNumber);

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (WinUsb_SetCurrentAlternateSetting(errorState, intfHandle.winusbHandle, (byte) alternateNumber) == 0)
                throwLastError(errorState, "setting alternate interface failed");
        }
        intf.setAlternate(altSetting);
    }

    public synchronized void releaseInterface(int interfaceNumber) {
        checkIsOpen();

        getInterfaceWithCheck(interfaceNumber, true);

        var intfHandle = getInterfaceHandle(interfaceNumber);
        var firstIntfHandle = intfHandle;
        if (intfHandle.firstInterfaceNumber != interfaceNumber)
            firstIntfHandle = getInterfaceHandle(intfHandle.firstInterfaceNumber);

        setClaimed(interfaceNumber, false);

        if (intfHandle != firstIntfHandle) {
            // close associated interface
            WinUsb_Free(intfHandle.winusbHandle);
            intfHandle.winusbHandle = null;
        }

        // close device if needed
        firstIntfHandle.deviceOpenCount -= 1;
        if (firstIntfHandle.deviceOpenCount == 0) {
            WinUsb_Free(firstIntfHandle.winusbHandle);
            firstIntfHandle.winusbHandle = null;

            LOG.log(DEBUG, "closing device {0}", getCachedInterfaceDevicePath(interfaceNumber));

            CloseHandle(firstIntfHandle.deviceHandle);
            firstIntfHandle.deviceHandle = null;
        }
    }

    @Override
    public void controlTransferOut(@NotNull UsbControlTransfer setup, byte[] data) {
        try (var arena = Arena.ofConfined()) {

            // copy data to native memory
            var transfer = createSyncControlTransfer();
            var dataLength = data != null ? data.length : 0;
            transfer.setDataSize(dataLength);
            if (dataLength != 0) {
                var buffer = arena.allocate(data.length);
                buffer.copyFrom(MemorySegment.ofArray(data));
                transfer.setData(buffer);
            } else {
                transfer.setData(NULL);
            }

            synchronized (transfer) {
                submitControlTransfer(UsbDirection.OUT, setup, transfer);
                waitForTransfer(transfer, 0, UsbDirection.OUT, 0);
            }
        }
    }

    @Override
    public byte @NotNull [] controlTransferIn(@NotNull UsbControlTransfer setup, int length) {
        try (var arena = Arena.ofConfined()) {
            var transfer = createSyncControlTransfer();
            transfer.setData(arena.allocate(length));
            transfer.setDataSize(length);

            synchronized (transfer) {
                submitControlTransfer(UsbDirection.IN, setup, transfer);
                waitForTransfer(transfer, 0, UsbDirection.IN, 0);
            }

            return transfer.data().asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte @NotNull [] data, int offset, int length, int timeout) {
        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data).asSlice(offset, length));
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransferOut(endpointNumber, transfer);
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
                submitTransferIn(endpointNumber, transfer);
                waitForTransfer(transfer, timeout, UsbDirection.IN, endpointNumber);
            }

            return buffer.asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    private WindowsTransfer createSyncControlTransfer() {
        var transfer = new WindowsTransfer();
        transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);
        return transfer;
    }

    private WindowsTransfer createSyncTransfer(MemorySegment data) {
        var transfer = new WindowsTransfer();
        transfer.setData(data);
        transfer.setDataSize((int) data.byteSize());
        transfer.setCompletion(UsbDeviceImpl::onSyncTransferCompleted);
        return transfer;
    }

    @Override
    protected Transfer createTransfer() {
        return new WindowsTransfer();
    }

    @Override
    protected void throwOSException(int errorCode, String message, Object... args) {
        throwException(errorCode, message, args);
    }

    synchronized void submitControlTransfer(UsbDirection direction, UsbControlTransfer setup, WindowsTransfer transfer) {
        checkIsOpen();
        var intfHandle = findControlTransferInterface(setup);

        try (var arena = Arena.ofConfined()) {
            var setupPacket = new SetupPacket(arena);
            var bmRequest =
                    (direction == UsbDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
            setupPacket.setRequestType(bmRequest);
            setupPacket.setRequest(setup.request());
            setupPacket.setValue(setup.value());
            setupPacket.setIndex(setup.index());
            setupPacket.setLength(transfer.dataSize());

            var errorState = allocateErrorState(arena);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUsb_ControlTransfer(errorState, intfHandle.winusbHandle, setupPacket.segment(), transfer.data(),
                    transfer.dataSize(), NULL, transfer.overlapped()) == 0) {
                var err = Win.getLastError(errorState);
                if (err != ERROR_IO_PENDING)
                    throwException(err, "submitting control transfer failed");
            }
        }
    }

    synchronized void submitTransferOut(int endpointNumber, WindowsTransfer transfer) {
        var endpoint = getEndpoint(UsbDirection.OUT, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUsb_WritePipe(errorState, intfHandle.winusbHandle, endpoint.endpointAddress(), transfer.data(),
                    transfer.dataSize(), NULL, transfer.overlapped()) == 0) {
                var err = Win.getLastError(errorState);
                if (err != ERROR_IO_PENDING)
                    throwException(err, "submitting transfer OUT failed");
            }
        }
    }

    synchronized void submitTransferIn(int endpointNumber, WindowsTransfer transfer) {
        var endpoint = getEndpoint(UsbDirection.IN, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUsb_ReadPipe(errorState, intfHandle.winusbHandle, endpoint.endpointAddress(), transfer.data(),
                    transfer.dataSize(), NULL, transfer.overlapped()) == 0) {
                var err = Win.getLastError(errorState);
                if (err != ERROR_IO_PENDING)
                    throwException(err, "submitting transfer IN failed");
            }
        }
    }

    synchronized void configureForAsyncIo(UsbDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            var timeoutHolder = arena.allocate(JAVA_INT);
            if (WinUsb_SetPipePolicy(errorState, intfHandle.winusbHandle, endpoint.endpointAddress(),
                    PIPE_TRANSFER_TIMEOUT, (int) timeoutHolder.byteSize(), timeoutHolder) == 0)
                throwLastError(errorState, "setting timeout failed");

            var rawIoHolder = arena.allocate(JAVA_BYTE);
            rawIoHolder.setAtIndex(JAVA_BYTE, 0, (byte) 1);
            if (WinUsb_SetPipePolicy(errorState, intfHandle.winusbHandle, endpoint.endpointAddress(), RAW_IO,
                    (int) rawIoHolder.byteSize(), rawIoHolder) == 0)
                throwLastError(errorState, "setting raw IO failed");
        }
    }

    @Override
    public synchronized void clearHalt(UsbDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (WinUsb_ResetPipe(errorState, intfHandle.winusbHandle, endpoint.endpointAddress()) == 0)
                throwLastError(errorState, "clearing halt failed");
        }
    }

    @Override
    public synchronized void abortTransfers(UsbDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, UsbTransferType.BULK, UsbTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (WinUsb_AbortPipe(errorState, intfHandle.winusbHandle, endpoint.endpointAddress()) == 0)
                throwLastError(errorState, "aborting transfers on endpoint failed");
        }
    }

    @Override
    public synchronized @NotNull InputStream openInputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(UsbDirection.IN, endpointNumber, UsbTransferType.BULK, null);

        return new WindowsEndpointInputStream(this, endpointNumber, bufferSize);
    }

    @Override
    public synchronized @NotNull OutputStream openOutputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(UsbDirection.OUT, endpointNumber, UsbTransferType.BULK, null);

        return new WindowsEndpointOutputStream(this, endpointNumber, bufferSize);
    }

    private InterfaceHandle getInterfaceHandle(int interfaceNumber) {
        for (var intfHandle : interfaceHandles) {
            if (intfHandle.interfaceNumber == interfaceNumber)
                return intfHandle;
        }

        throwException("invalid interface number %s", interfaceNumber);
        throw new AssertionError("not reached");
    }

    private InterfaceHandle findControlTransferInterface(UsbControlTransfer setup) {

        var interfaceNumber = -1;
        int endpointNumber;

        if (setup.recipient() == UsbRecipient.INTERFACE) {

            interfaceNumber = setup.index() & 0xff;

        } else if (setup.recipient() == UsbRecipient.ENDPOINT) {

            endpointNumber = setup.index() & 0x7f;
            var direction = (setup.index() & 0x80) != 0 ? UsbDirection.IN : UsbDirection.OUT;
            if (endpointNumber != 0) {
                interfaceNumber = getInterfaceNumber(direction, endpointNumber);
                if (interfaceNumber == -1)
                    throwException("invalid endpoint number %d or interface not claimed", endpointNumber);
            }
        }

        if (interfaceNumber >= 0) {
            var intfHandle = getInterfaceHandle(interfaceNumber);
            if (intfHandle.winusbHandle == null)
                throwException("interface number %d has not been claimed", interfaceNumber);
            return intfHandle;
        }

        // for control transfer to device, use any claimed interface
        for (var intfHandle : interfaceHandles) {
            if (intfHandle.winusbHandle != null)
                return intfHandle;
        }

        throwException("control transfer cannot be executed as no interface has been claimed");
        throw new AssertionError("not reached");
    }

    private String getInterfaceDevicePath(int interfaceNumber) {
        var devicePath = getCachedInterfaceDevicePath(interfaceNumber);
        if (devicePath != null)
            return devicePath;

        var parentDevicePath = (String) getUniqueId();

        try (var deviceInfoSet = DeviceInfoSet.ofPath(parentDevicePath)) {
            var childrenInstanceIDs = deviceInfoSet.getStringListProperty(DEVPKEY_Device_Children());
            if (childrenInstanceIDs == null) {
                LOG.log(DEBUG, "missing children instance IDs for device {0}", parentDevicePath);
                return null;

            } else {
                LOG.log(DEBUG, "children instance IDs: {0}", childrenInstanceIDs);

                for (var instanceId : childrenInstanceIDs) {
                    devicePath = getChildDevicePath(instanceId, interfaceNumber);
                    if (devicePath != null)
                        return devicePath;
                }
            }
        }

        return null; // retry later
    }

    private String getCachedInterfaceDevicePath(int interfaceNumber) {
        if (!isComposite)
            return (String) getUniqueId();
        return devicePaths.get(interfaceNumber);
    }

    private String getChildDevicePath(String instanceId, int interfaceNumber) {
        try (var deviceInfoSet = DeviceInfoSet.ofInstance(instanceId)) {

            // get hardware IDs (to extract interface number)
            var hardwareIds = deviceInfoSet.getStringListProperty(DEVPKEY_Device_HardwareIds());
            if (hardwareIds == null) {
                LOG.log(DEBUG, "child device {0} has no hardware IDs", instanceId);
                return null;
            }

            var extractedNumber = extractInterfaceNumber(hardwareIds);
            if (extractedNumber == -1) {
                LOG.log(DEBUG, "child device {0} has no interface number", instanceId);
                return null;
            }

            if (extractedNumber != interfaceNumber)
                return null;

            var devicePath = deviceInfoSet.getDevicePathByGUID(instanceId);
            if (devicePath == null) {
                LOG.log(INFO, "Child device {0} has no device path / interface GUID", instanceId);
                throw new UsbException("claiming interface failed (composite function has no device path / interface GUID; the required WinUSB driver is probably not installed)");
            }

            if (devicePaths == null)
                devicePaths = new HashMap<>();
            devicePaths.put(interfaceNumber, devicePath);
            return devicePath;
        }
    }

    private static final Pattern MULTIPLE_INTERFACE_ID = Pattern.compile(
            "USB\\\\VID_[0-9A-Fa-f]{4}&PID_[0-9A-Fa-f]{4}&MI_([0-9A-Fa-f]{2})");

    private static int extractInterfaceNumber(List<String> hardwareIds) {
        // Also see https://docs.microsoft.com/en-us/windows-hardware/drivers/install/standard-usb-identifiers#multiple-interface-usb-devices

        for (var id : hardwareIds) {
            var matcher = MULTIPLE_INTERFACE_ID.matcher(id);
            if (matcher.find()) {
                var intfHexNumber = matcher.group(1);
                try {
                    return Integer.parseInt(intfHexNumber, 16);
                } catch (NumberFormatException _) {
                    // ignore and try next one
                }
            }
        }

        return -1;
    }
}
