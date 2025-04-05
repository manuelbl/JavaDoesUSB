//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.UsbDevice;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.UsbDeviceImpl;
import net.codecrete.usb.common.UsbDeviceRegistry;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;
import net.codecrete.usb.usbstandard.DeviceDescriptor;
import net.codecrete.usb.usbstandard.SetupPacket;
import net.codecrete.usb.usbstandard.StringDescriptor;
import windows.win32.devices.usb.USB_DESCRIPTOR_REQUEST;
import windows.win32.devices.usb.USB_NODE_CONNECTION_INFORMATION_EX;
import windows.win32.ui.windowsandmessaging.DEV_BROADCAST_DEVICEINTERFACE_W;
import windows.win32.ui.windowsandmessaging.DEV_BROADCAST_HDR;
import windows.win32.ui.windowsandmessaging.MSG;
import windows.win32.ui.windowsandmessaging.WNDCLASSEXW;
import windows.win32.ui.windowsandmessaging.WNDPROC;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.lang.foreign.ValueLayout.PathElement;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static net.codecrete.usb.usbstandard.Constants.CONFIGURATION_DESCRIPTOR_TYPE;
import static net.codecrete.usb.usbstandard.Constants.DEFAULT_LANGUAGE;
import static net.codecrete.usb.usbstandard.Constants.STRING_DESCRIPTOR_TYPE;
import static net.codecrete.usb.windows.CustomApis.CloseHandle;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUsbException.throwException;
import static net.codecrete.usb.windows.WindowsUsbException.throwLastError;
import static windows.win32.devices.properties.Constants.DEVPKEY_Device_Address;
import static windows.win32.devices.properties.Constants.DEVPKEY_Device_InstanceId;
import static windows.win32.devices.properties.Constants.DEVPKEY_Device_Parent;
import static windows.win32.devices.usb.Constants.GUID_DEVINTERFACE_USB_DEVICE;
import static windows.win32.devices.usb.Constants.GUID_DEVINTERFACE_USB_HUB;
import static windows.win32.devices.usb.Constants.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION;
import static windows.win32.devices.usb.Constants.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX;
import static windows.win32.devices.usb.Constants.USB_REQUEST_GET_DESCRIPTOR;
import static windows.win32.foundation.GENERIC_ACCESS_RIGHTS.GENERIC_WRITE;
import static windows.win32.storage.filesystem.Apis.CreateFileW;
import static windows.win32.storage.filesystem.FILE_CREATION_DISPOSITION.OPEN_EXISTING;
import static windows.win32.storage.filesystem.FILE_SHARE_MODE.FILE_SHARE_WRITE;
import static windows.win32.system.io.Apis.DeviceIoControl;
import static windows.win32.system.libraryloader.Apis.GetModuleHandleW;
import static windows.win32.ui.windowsandmessaging.Apis.CreateWindowExW;
import static windows.win32.ui.windowsandmessaging.Apis.DefWindowProcW;
import static windows.win32.ui.windowsandmessaging.Apis.GetMessageW;
import static windows.win32.ui.windowsandmessaging.Apis.RegisterClassExW;
import static windows.win32.ui.windowsandmessaging.Apis.RegisterDeviceNotificationW;
import static windows.win32.ui.windowsandmessaging.Constants.DBT_DEVICEARRIVAL;
import static windows.win32.ui.windowsandmessaging.Constants.DBT_DEVICEREMOVECOMPLETE;
import static windows.win32.ui.windowsandmessaging.Constants.HWND_MESSAGE;
import static windows.win32.ui.windowsandmessaging.Constants.WM_DEVICECHANGE;
import static windows.win32.ui.windowsandmessaging.DEV_BROADCAST_HDR_DEVICE_TYPE.DBT_DEVTYP_DEVICEINTERFACE;
import static windows.win32.ui.windowsandmessaging.REGISTER_NOTIFICATION_FLAGS.DEVICE_NOTIFY_WINDOW_HANDLE;

/**
 * Windows implementation of USB device registry.
 * <p>
 * To retrieve details of a USB device, this class accesses it indirectly
 * via the parent. To address it the parent's handle (<i>hub handle</i>) and
 * the device's port number is needed.
 * </p>
 */
public class WindowsUsbDeviceRegistry extends UsbDeviceRegistry {

    private static final System.Logger LOG = System.getLogger(WindowsUsbDeviceRegistry.class.getName());

    private static final long REQUEST_DATA_OFFSET
            = USB_DESCRIPTOR_REQUEST.layout().byteOffset(PathElement.groupElement("Data"));

    @Override
    protected void monitorDevices() {
        try (var arena = Arena.ofConfined()) {

            MemorySegment hwnd;
            var errorState = allocateErrorState(arena);

            try {
                final var className = arena.allocateFrom("USB_MONITOR", UTF_16LE);
                final var windowName = arena.allocateFrom("USB device monitor", UTF_16LE);
                final var instance = GetModuleHandleW(errorState, NULL);

                // register window class
                var wx = WNDCLASSEXW.allocate(arena);
                WNDCLASSEXW.lpfnWndProc(wx, WNDPROC.allocate(arena, this::handleWindowMessage));
                WNDCLASSEXW.hInstance(wx, instance);
                WNDCLASSEXW.lpszClassName(wx, className);
                var atom = RegisterClassExW(errorState, wx);
                if (atom == 0)
                    throwLastError(errorState, "internal error (RegisterClassExW)");

                // create message-only window
                hwnd = CreateWindowExW(errorState, 0, className, windowName, 0, 0, 0, 0, 0, HWND_MESSAGE, NULL,
                        instance, NULL);
                if (hwnd.address() == 0)
                    throwLastError(errorState, "internal error (CreateWindowExW)");

                // configure notifications
                var notificationFilter = DEV_BROADCAST_DEVICEINTERFACE_W.allocate(arena, 260);
                DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_size(notificationFilter, (int) notificationFilter.byteSize());
                DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_devicetype(notificationFilter, DBT_DEVTYP_DEVICEINTERFACE);
                DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_classguid(notificationFilter).copyFrom(GUID_DEVINTERFACE_USB_DEVICE());

                var notifyHandle = RegisterDeviceNotificationW(errorState, hwnd, notificationFilter,
                        DEVICE_NOTIFY_WINDOW_HANDLE);
                if (notifyHandle.address() == 0)
                    throwLastError(errorState, "internal error (RegisterDeviceNotificationW)");

                // initial device enumeration
                enumeratePresentDevices();

            } catch (Exception e) {
                enumerationFailed(e);
                return;
            }

            // process messages
            var msg = MSG.allocate(arena);
            int err;
            //noinspection StatementWithEmptyBody
            while ((err = GetMessageW(errorState, msg, hwnd, 0, 0)) > 0)
                ; // do nothing

            if (err == -1)
                throwLastError(errorState, "internal error (GetMessageW)");
        }
    }

    @SuppressWarnings("java:S106")
    private void enumeratePresentDevices() {

        List<UsbDevice> deviceList = new ArrayList<>();
        try (var cleanup = new ScopeCleanup();
             var deviceInfoSet = DeviceInfoSet.ofPresentDevices(GUID_DEVINTERFACE_USB_DEVICE(), null)) {

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            cleanup.add(() -> hubHandles.forEach((_, handle) -> CloseHandle(handle)));

            // iterate all devices
            while (deviceInfoSet.next()) {

                var instanceId = deviceInfoSet.getStringProperty(DEVPKEY_Device_InstanceId());
                var devicePath = DeviceInfoSet.getDevicePath(instanceId, GUID_DEVINTERFACE_USB_DEVICE());

                try {
                    deviceList.add(createDeviceFromDeviceInfo(deviceInfoSet, devicePath, hubHandles));

                } catch (Exception e) {
                    LOG.log(INFO, String.format("failed to retrieve information about device %s - ignoring device", devicePath), e);
                }
            }

            setInitialDeviceList(deviceList);
        }
    }

    private UsbDevice createDeviceFromDeviceInfo(DeviceInfoSet deviceInfoSet, String devicePath,
                                                 Map<String, MemorySegment> hubHandles) {
        try (var arena = Arena.ofConfined()) {

            var usbPortNum = deviceInfoSet.getIntProperty(DEVPKEY_Device_Address());
            var parentInstanceId = deviceInfoSet.getStringProperty(DEVPKEY_Device_Parent());
            var hubPath = DeviceInfoSet.getDevicePath(parentInstanceId, GUID_DEVINTERFACE_USB_HUB());

            // open hub if not open yet
            var hubHandle = hubHandles.get(hubPath);
            if (hubHandle == null) {
                var hubPathSeg = arena.allocateFrom(hubPath, UTF_16LE);
                var errorState = allocateErrorState(arena);
                hubHandle = CreateFileW(errorState, hubPathSeg, GENERIC_WRITE, FILE_SHARE_WRITE,
                        NULL, OPEN_EXISTING, 0, NULL);
                if (Win.isInvalidHandle(hubHandle))
                    throwLastError(errorState, "internal error (opening hub device)");
                hubHandles.put(hubPath, hubHandle);
            }

            return createDevice(devicePath, deviceInfoSet.isCompositeDevice(), hubHandle, usbPortNum);
        }
    }

    /**
     * Retrieve device descriptor and create {@code UsbDevice} instance
     *
     * @param devicePath the device path
     * @param hubHandle  the hub handle (parent)
     * @param usbPortNum the USB port number
     * @return the {@code UsbDevice} instance
     */
    private UsbDevice createDevice(String devicePath, boolean isComposite, MemorySegment hubHandle, int usbPortNum) {

        try (var arena = Arena.ofConfined()) {

            // get device descriptor
            var connInfo = USB_NODE_CONNECTION_INFORMATION_EX.allocate(arena, 0);
            USB_NODE_CONNECTION_INFORMATION_EX.ConnectionIndex(connInfo, usbPortNum);
            var sizeHolder = arena.allocate(JAVA_INT);
            var errorState = allocateErrorState(arena);
            if (DeviceIoControl(errorState, hubHandle, IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX,
                    connInfo, (int) connInfo.byteSize(), connInfo, (int) connInfo.byteSize(), sizeHolder, NULL) == 0)
                throwLastError(errorState, "internal error (getting device descriptor failed)");

            var descriptorSegment = USB_NODE_CONNECTION_INFORMATION_EX.DeviceDescriptor(connInfo);
            var deviceDescriptor = new DeviceDescriptor(descriptorSegment);

            var vendorId = deviceDescriptor.vendorID();
            var productId = deviceDescriptor.productID();

            var configDesc = getDescriptor(hubHandle, usbPortNum, CONFIGURATION_DESCRIPTOR_TYPE, 0, (short) 0, arena);

            // create new device
            var device = new WindowsUsbDevice(devicePath, vendorId, productId, configDesc, isComposite);
            device.setFromDeviceDescriptor(descriptorSegment);

            var languages = getLanguages(hubHandle, usbPortNum, arena);
            device.setProductString(descriptorSegment, index -> getStringDescriptor(hubHandle, usbPortNum, index, languages));
            return device;
        }
    }

    private MemorySegment getDescriptor(MemorySegment hubHandle, int usbPortNumber, int descriptorType, int index,
                                        short languageID, Arena arena) {
        return getDescriptor(hubHandle, usbPortNumber, descriptorType, index, languageID, 0, arena);

    }

    private MemorySegment getDescriptor(MemorySegment hubHandle, int usbPortNumber, int descriptorType, int index,
                                        short languageID, int requestSize, Arena arena) {
        var size = requestSize != 0 ? requestSize + (int) REQUEST_DATA_OFFSET : 256;

        // create descriptor requests
        var descriptorRequest = arena.allocate(size);
        USB_DESCRIPTOR_REQUEST.ConnectionIndex(descriptorRequest, usbPortNumber);
        var setupPacket = new SetupPacket(descriptorRequest.asSlice(
                USB_DESCRIPTOR_REQUEST.SetupPacket_bmRequest$offset(), SetupPacket.LAYOUT.byteSize()));
        setupPacket.setRequestType(0x80); // device-to-host / type standard / recipient device
        setupPacket.setRequest(USB_REQUEST_GET_DESCRIPTOR);
        setupPacket.setValue((descriptorType << 8) | index);
        setupPacket.setIndex(languageID);
        setupPacket.setLength(size - (int) REQUEST_DATA_OFFSET);

        // execute request
        var effectiveSizeHolder = arena.allocate(JAVA_INT);
        var errorState = allocateErrorState(arena);
        if (DeviceIoControl(errorState, hubHandle, IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION,
                descriptorRequest, size, descriptorRequest, size, effectiveSizeHolder, NULL) == 0)
            throwLastError(errorState, "internal error (retrieving descriptor %d failed)", index);

        // determine size of descriptor
        int expectedSize;
        if (descriptorType != CONFIGURATION_DESCRIPTOR_TYPE) {
            expectedSize = 255 & descriptorRequest.get(JAVA_BYTE, REQUEST_DATA_OFFSET);
        } else {
            var configDesc =
                    new ConfigurationDescriptor(descriptorRequest.asSlice(REQUEST_DATA_OFFSET, ConfigurationDescriptor.LAYOUT.byteSize()));
            expectedSize = configDesc.totalLength();
        }

        // check against effective size
        var effectiveSize = effectiveSizeHolder.get(JAVA_INT, 0) - REQUEST_DATA_OFFSET;
        if (effectiveSize != expectedSize) {
            if (requestSize != 0)
                throwException("internal error (unexpected descriptor size)");

            // repeat with correct size
            return getDescriptor(hubHandle, usbPortNumber, descriptorType, index, languageID, expectedSize, arena);
        }

        return descriptorRequest.asSlice(REQUEST_DATA_OFFSET, effectiveSize);
    }

    @SuppressWarnings("java:S106")
    private String getStringDescriptor(MemorySegment hubHandle, int usbPortNumber, int index, short[] languages) {
        if (index == 0)
            return null;

        try (var arena = Arena.ofConfined()) {
            for (var language : languages) {
                try {
                    var stringDesc = new StringDescriptor(getDescriptor(hubHandle, usbPortNumber, STRING_DESCRIPTOR_TYPE,
                            index, language, arena));
                    return stringDesc.string();

                } catch (UsbException _) {
                    // ignore and try next language
                }
            }
        }

        // Even though this function is only called for string descriptors referenced in the
        // configuration descriptor, some device might not provide them; so ignore it.
        return null;
    }

    private short[] getLanguages(MemorySegment hubHandle, int usbPortNumber, Arena arena) {
        try {
            var languages = getDescriptor(hubHandle, usbPortNumber, STRING_DESCRIPTOR_TYPE, 0, (short) 0, arena);
            var n = (languages.byteSize() - 2) / 2;
            if (n == 0)
                return new short[]{DEFAULT_LANGUAGE};
            return languages.asSlice(2, n * 2).toArray(JAVA_SHORT);
        } catch (UsbException _) {
            return new short[]{DEFAULT_LANGUAGE};
        }
    }

    @SuppressWarnings("java:S1144")
    private long handleWindowMessage(MemorySegment hWnd, int uMsg, long wParam, long lParam) {

        // check for message related to connecting/disconnecting devices
        if (uMsg == WM_DEVICECHANGE && (wParam == DBT_DEVICEARRIVAL || wParam == DBT_DEVICEREMOVECOMPLETE)) {
            var data = MemorySegment.ofAddress(lParam).reinterpret(DEV_BROADCAST_DEVICEINTERFACE_W.sizeof());
            if (DEV_BROADCAST_HDR.dbch_devicetype(data) == DBT_DEVTYP_DEVICEINTERFACE) {

                // get device path
                var nameSlice =
                        MemorySegment.ofAddress(DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_name(data).address()).reinterpret(500);
                var devicePath = nameSlice.getString(0, UTF_16LE);
                if (wParam == DBT_DEVICEARRIVAL)
                    onDeviceConnected(devicePath);
                else
                    onDeviceDisconnected(devicePath);
                return 0;
            }
        }

        // default message handling
        return DefWindowProcW(hWnd, uMsg, wParam, lParam);
    }

    @SuppressWarnings("java:S106")
    private void onDeviceConnected(String devicePath) {
        try (var cleanup = new ScopeCleanup();
             var deviceInfoSet = DeviceInfoSet.ofPath(devicePath)) {

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            cleanup.add(() -> hubHandles.forEach((_, handle) -> CloseHandle(handle)));

            try {
                // create device instance
                var device = createDeviceFromDeviceInfo(deviceInfoSet, devicePath, hubHandles);

                // add it to device list
                addDevice(device);

            } catch (Exception e) {
                LOG.log(INFO, String.format("failed to retrieve information about device %s - ignoring device", devicePath), e);
            }
        }
    }

    private void onDeviceDisconnected(String devicePath) {
        closeAndRemoveDevice(devicePath);
    }

    /**
     * Finds the index of the device in the list.
     * <p>
     * This override uses a case-insensitive string comparison as Windows uses different casing
     * when initially enumerating devices and during later monitoring.
     * </p>
     *
     * @param deviceList the device list
     * @param deviceId   the unique device ID
     * @return index, or -1 if not found
     */
    @Override
    protected int findDeviceIndex(List<UsbDevice> deviceList, Object deviceId) {
        var id = deviceId.toString();
        for (var i = 0; i < deviceList.size(); i++) {
            var dev = (UsbDeviceImpl) deviceList.get(i);
            if (id.equalsIgnoreCase(dev.getUniqueId().toString()))
                return i;
        }
        return -1;
    }
}
