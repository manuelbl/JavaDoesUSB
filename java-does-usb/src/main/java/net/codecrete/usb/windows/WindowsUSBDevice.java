//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.*;
import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.usbstandard.SetupPacket;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.winusb.WinUSB;
import net.codecrete.usb.windows.winsdk.Kernel32B;
import net.codecrete.usb.windows.winsdk.WinUSB2;

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
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.common.ForeignMemory.dereference;
import static net.codecrete.usb.windows.DevicePropertyKey.Children;
import static net.codecrete.usb.windows.DevicePropertyKey.HardwareIds;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUSBException.throwException;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

/**
 * Windows implementation for USB device.
 */
@SuppressWarnings("java:S2160")
public class WindowsUSBDevice extends USBDeviceImpl {

    private static final System.Logger LOG = System.getLogger(WindowsUSBDevice.class.getName());

    private static final int RETRY_LATER = 0;
    private static final int TRY_NEXT_CHILD = 1;
    private static final int SUCCESS = 2;

    private final WindowsAsyncTask asyncTask;
    private List<InterfaceHandle> interfaceHandles;
    // device paths by interface number (first interface of function)
    private Map<Integer, String> devicePaths;

    /**
     * Indicates if {@link #open()} has been called. Since separate interfaces can have separate underlying
     * Windows device, {@link #claimInterface(int)} instead of {@link #open()} will open the Windows device.
     */
    private boolean showAsOpen;

    WindowsUSBDevice(String devicePath, int vendorId, int productId, MemorySegment configDesc) {
        super(devicePath, vendorId, productId);
        asyncTask = WindowsAsyncTask.INSTANCE;
        readDescription(configDesc);
    }

    private void readDescription(MemorySegment configDesc) {
        var configuration = setConfigurationDescriptor(configDesc);

        // build list of interface handles
        interfaceHandles = configuration.interfaces().stream()
                .map(intf -> {
                    var interfaceNumber = intf.number();
                    var function = configuration.findFunction(interfaceNumber);
                    return new InterfaceHandle(interfaceNumber, function.firstInterfaceNumber());
                }).
                toList();
    }

    @Override
    public boolean isOpen() {
        return showAsOpen;
    }

    @Override
    public synchronized void open() {
        if (isOpen())
            throwException("device is already open");

        showAsOpen = true;
    }

    @Override
    public synchronized void close() {
        if (!isOpen())
            return;

        for (var intf : interfaceList) {
            if (intf.isClaimed())
                releaseInterface(intf.number());
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
            if (claimInteraceSynchronized(interfaceNumber))
                return; // success

            numRetries -= 1;
            if (numRetries == 0)
                throw new USBException("claiming interface failed (function has no device path, might be missing WinUSB driver)");

            // sleep and retry
            try {
                LOG.log(DEBUG, "Sleeping for 100ms...");
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized boolean claimInteraceSynchronized(int interfaceNumber) {
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
                var pathSegment = Win.createSegmentFromString(devicePath, arena);
                var deviceHandle = Kernel32B.CreateFileW(pathSegment, Kernel32.GENERIC_WRITE() | Kernel32.GENERIC_READ(),
                        Kernel32.FILE_SHARE_WRITE() | Kernel32.FILE_SHARE_READ(), NULL, Kernel32.OPEN_EXISTING(),
                        Kernel32.FILE_ATTRIBUTE_NORMAL() | Kernel32.FILE_FLAG_OVERLAPPED(), NULL, errorState);

                if (Win.isInvalidHandle(deviceHandle))
                    throwLastError(errorState, "claiming interface failed (opening USB device %s failed)", devicePath);

                try {
                    // open first interface
                    var interfaceHandleHolder = arena.allocate(ADDRESS);
                    if (WinUSB2.WinUsb_Initialize(deviceHandle, interfaceHandleHolder, errorState) == 0)
                        throwLastError(errorState, "claiming interface failed");
                    var interfaceHandle = dereference(interfaceHandleHolder);

                    firstIntfHandle.deviceHandle = deviceHandle;
                    firstIntfHandle.winusbHandle = interfaceHandle;
                    asyncTask.addDevice(deviceHandle);

                } catch (Exception e) {
                    Kernel32.CloseHandle(deviceHandle);
                    throw e;
                }
            }

            if (intfHandle != firstIntfHandle) {
                // open associated interface
                var interfaceHandleHolder = arena.allocate(ADDRESS);
                if (WinUSB2.WinUsb_GetAssociatedInterface(firstIntfHandle.winusbHandle,
                        (byte)(intfHandle.interfaceNumber - firstIntfHandle.interfaceNumber - 1),
                        interfaceHandleHolder, errorState) == 0)
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
        if (altSetting == null)
            throwException("interface %d does not have an alternate interface setting %d", interfaceNumber,
                    alternateNumber);

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (WinUSB2.WinUsb_SetCurrentAlternateSetting(intfHandle.winusbHandle, (byte) alternateNumber,
                    errorState) == 0)
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
            WinUSB.WinUsb_Free(intfHandle.winusbHandle);
            intfHandle.winusbHandle = null;
        }

        // close device if needed
        firstIntfHandle.deviceOpenCount -= 1;
        if (firstIntfHandle.deviceOpenCount == 0) {
            WinUSB.WinUsb_Free(firstIntfHandle.winusbHandle);
            firstIntfHandle.winusbHandle = null;

            var path = (String)getUniqueId();
            if (firstIntfHandle.interfaceNumber != 0) {
                if (devicePaths != null)
                    path = devicePaths.get(firstIntfHandle.interfaceNumber);
                else
                    path = null;
            }
            LOG.log(DEBUG, "closing device {0}", path);

            Kernel32.CloseHandle(firstIntfHandle.deviceHandle);
            firstIntfHandle.deviceHandle = null;
        }
    }

    @Override
    public void controlTransferOut(USBControlTransfer setup, byte[] data) {
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
                submitControlTransfer(USBDirection.OUT, setup, transfer);
                waitForTransfer(transfer, 0, USBDirection.OUT, 0);
            }
        }
    }

    @Override
    public byte[] controlTransferIn(USBControlTransfer setup, int length) {
        try (var arena = Arena.ofConfined()) {
            var transfer = createSyncControlTransfer();
            transfer.setData(arena.allocate(length));
            transfer.setDataSize(length);

            synchronized (transfer) {
                submitControlTransfer(USBDirection.IN, setup, transfer);
                waitForTransfer(transfer, 0, USBDirection.IN, 0);
            }

            return transfer.data().asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    @Override
    public void transferOut(int endpointNumber, byte[] data, int offset, int length, int timeout) {
        try (var arena = Arena.ofConfined()) {
            var buffer = arena.allocate(data.length);
            buffer.copyFrom(MemorySegment.ofArray(data).asSlice(offset, length));
            var transfer = createSyncTransfer(buffer);

            synchronized (transfer) {
                submitTransferOut(endpointNumber, transfer);
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
                submitTransferIn(endpointNumber, transfer);
                waitForTransfer(transfer, timeout, USBDirection.IN, endpointNumber);
            }

            return buffer.asSlice(0, transfer.resultSize()).toArray(JAVA_BYTE);
        }
    }

    private WindowsTransfer createSyncControlTransfer() {
        var transfer = new WindowsTransfer();
        transfer.setCompletion(USBDeviceImpl::onSyncTransferCompleted);
        return transfer;
    }

    private WindowsTransfer createSyncTransfer(MemorySegment data) {
        var transfer = new WindowsTransfer();
        transfer.setData(data);
        transfer.setDataSize((int) data.byteSize());
        transfer.setCompletion(USBDeviceImpl::onSyncTransferCompleted);
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

    synchronized void submitControlTransfer(USBDirection direction, USBControlTransfer setup, WindowsTransfer transfer) {
        checkIsOpen();
        var intfHandle = findControlTransferInterface(setup);

        try (var arena = Arena.ofConfined()) {
            var setupPacket = new SetupPacket(arena);
            var bmRequest =
                    (direction == USBDirection.IN ? 0x80 : 0) | (setup.requestType().ordinal() << 5) | setup.recipient().ordinal();
            setupPacket.setRequestType(bmRequest);
            setupPacket.setRequest(setup.request());
            setupPacket.setValue(setup.value());
            setupPacket.setIndex(setup.index());
            setupPacket.setLength(transfer.dataSize());

            var errorState = allocateErrorState(arena);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUSB2.WinUsb_ControlTransfer(intfHandle.winusbHandle, setupPacket.segment(), transfer.data(),
                    transfer.dataSize(), NULL, transfer.overlapped(), errorState) == 0) {
                var err = Win.getLastError(errorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "submitting control transfer failed");
            }
        }
    }

    synchronized void submitTransferOut(int endpointNumber, WindowsTransfer transfer) {
        var endpoint = getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUSB2.WinUsb_WritePipe(intfHandle.winusbHandle, endpoint.endpointAddress(), transfer.data(),
                    transfer.dataSize(), NULL, transfer.overlapped(), errorState) == 0) {
                var err = Win.getLastError(errorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "submitting transfer OUT failed");
            }
        }
    }

    synchronized void submitTransferIn(int endpointNumber, WindowsTransfer transfer) {
        var endpoint = getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            asyncTask.prepareForSubmission(transfer);

            // submit transfer
            if (WinUSB2.WinUsb_ReadPipe(intfHandle.winusbHandle, endpoint.endpointAddress(), transfer.data(),
                    transfer.dataSize(), NULL, transfer.overlapped(), errorState) == 0) {
                var err = Win.getLastError(errorState);
                if (err != Kernel32.ERROR_IO_PENDING())
                    throwException(err, "submitting transfer IN failed");
            }
        }
    }

    synchronized void configureForAsyncIo(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);

            var timeoutHolder = arena.allocate(JAVA_INT, 0);
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.winusbHandle, endpoint.endpointAddress(),
                    WinUSB.PIPE_TRANSFER_TIMEOUT(), (int) timeoutHolder.byteSize(), timeoutHolder, errorState) == 0)
                throwLastError(errorState, "setting timeout failed");

            var rawIoHolder = arena.allocate(JAVA_BYTE, (byte) 1);
            if (WinUSB2.WinUsb_SetPipePolicy(intfHandle.winusbHandle, endpoint.endpointAddress(), WinUSB.RAW_IO(),
                    (int) rawIoHolder.byteSize(), rawIoHolder, errorState) == 0)
                throwLastError(errorState, "setting raw IO failed");
        }
    }

    @Override
    public synchronized void clearHalt(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (WinUSB2.WinUsb_ResetPipe(intfHandle.winusbHandle, endpoint.endpointAddress(), errorState) == 0)
                throwLastError(errorState, "clearing halt failed");
        }
    }

    @Override
    public synchronized void abortTransfers(USBDirection direction, int endpointNumber) {
        var endpoint = getEndpoint(direction, endpointNumber, USBTransferType.BULK, USBTransferType.INTERRUPT);
        var intfHandle = getInterfaceHandle(endpoint.interfaceNumber());

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (WinUSB2.WinUsb_AbortPipe(intfHandle.winusbHandle, endpoint.endpointAddress(), errorState) == 0)
                throwLastError(errorState, "aborting transfers on endpoint failed");
        }
    }

    @Override
    public synchronized InputStream openInputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.IN, endpointNumber, USBTransferType.BULK, null);

        return new WindowsEndpointInputStream(this, endpointNumber, bufferSize);
    }

    @Override
    public synchronized OutputStream openOutputStream(int endpointNumber, int bufferSize) {
        // check that endpoint number is valid
        getEndpoint(USBDirection.OUT, endpointNumber, USBTransferType.BULK, null);

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

    private InterfaceHandle findControlTransferInterface(USBControlTransfer setup) {

        var interfaceNumber = -1;
        int endpointNumber;

        if (setup.recipient() == USBRecipient.INTERFACE) {

            interfaceNumber = setup.index() & 0xff;

        } else if (setup.recipient() == USBRecipient.ENDPOINT) {

            endpointNumber = setup.index() & 0x7f;
            var direction = (setup.index() & 0x80) != 0 ? USBDirection.IN : USBDirection.OUT;
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
        var parentDevicePath = (String) getUniqueId(); // device path is id
        if (interfaceNumber == 0)
            return parentDevicePath;

        if (devicePaths != null) {
            var devicePath = devicePaths.get(interfaceNumber);
            if (devicePath != null)
                return devicePath;
        }

        try (var deviceInfoSet = DeviceInfoSet.ofPath(parentDevicePath)) {
            if (!deviceInfoSet.isCompositeDevice())
                throwException("internal error: interface belongs to a composite function but device is not composite");

            var childrenInstanceIDs = deviceInfoSet.getStringListProperty(Children);
            if (childrenInstanceIDs == null) {
                LOG.log(DEBUG, "missing children instance IDs for device {0}", parentDevicePath);
                return null;

            } else {
                LOG.log(DEBUG, "children instance IDs: {0}", childrenInstanceIDs);

                for (var instanceId : childrenInstanceIDs) {
                    var res = fetchChildDevicePath(instanceId, interfaceNumber);
                    if (res == SUCCESS)
                        return devicePaths.get(interfaceNumber);
                    if (res == RETRY_LATER)
                        return null; // retry later
                }
            }
        }

        return null; // retry later
    }

    private int fetchChildDevicePath(String instanceId, int interfaceNumber) {
        try (var deviceInfoSet = DeviceInfoSet.ofInstance(instanceId)) {

            // get hardware IDs (to extract interface number)
            var hardwareIds = deviceInfoSet.getStringListProperty(HardwareIds);
            if (hardwareIds == null) {
                LOG.log(DEBUG, "child device {0} has no hardware IDs", instanceId);
                return TRY_NEXT_CHILD;
            }

            var extractedNumber = extractInterfaceNumber(hardwareIds);
            if (extractedNumber == -1) {
                LOG.log(DEBUG, "child device {0} has no interface number", instanceId);
                return TRY_NEXT_CHILD;
            }

            if (extractedNumber != interfaceNumber)
                return TRY_NEXT_CHILD;

            var devicePath = deviceInfoSet.getDevicePathByGUID(instanceId);
            if (devicePath == null) {
                LOG.log(INFO, "Child device {0} has no device path", instanceId);
                return RETRY_LATER;
            }

            if (devicePaths == null)
                devicePaths = new HashMap<>();
            devicePaths.put(interfaceNumber, devicePath);
            return SUCCESS;
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
                } catch (NumberFormatException e) {
                    // ignore and try next one
                }
            }
        }

        return -1;
    }
}
