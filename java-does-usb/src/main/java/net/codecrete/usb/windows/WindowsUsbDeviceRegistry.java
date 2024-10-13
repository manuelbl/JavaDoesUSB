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
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.usbioctl.USBIoctl;
import net.codecrete.usb.windows.gen.usbioctl._USB_DESCRIPTOR_REQUEST;
import net.codecrete.usb.windows.gen.usbioctl._USB_NODE_CONNECTION_INFORMATION_EX;
import net.codecrete.usb.windows.gen.user32.User32;
import net.codecrete.usb.windows.gen.user32._DEV_BROADCAST_DEVICEINTERFACE_W;
import net.codecrete.usb.windows.gen.user32._DEV_BROADCAST_HDR;
import net.codecrete.usb.windows.gen.user32.tagMSG;
import net.codecrete.usb.windows.gen.user32.tagWNDCLASSEXW;
import net.codecrete.usb.windows.winsdk.Kernel32B;
import net.codecrete.usb.windows.winsdk.User32B;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.lang.foreign.ValueLayout.PathElement;
import static net.codecrete.usb.usbstandard.Constants.CONFIGURATION_DESCRIPTOR_TYPE;
import static net.codecrete.usb.usbstandard.Constants.DEFAULT_LANGUAGE;
import static net.codecrete.usb.usbstandard.Constants.STRING_DESCRIPTOR_TYPE;
import static net.codecrete.usb.windows.DevicePropertyKey.Address;
import static net.codecrete.usb.windows.DevicePropertyKey.InstanceId;
import static net.codecrete.usb.windows.DevicePropertyKey.Parent;
import static net.codecrete.usb.windows.UsbConstants.GUID_DEVINTERFACE_USB_DEVICE;
import static net.codecrete.usb.windows.UsbConstants.GUID_DEVINTERFACE_USB_HUB;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUsbException.throwException;
import static net.codecrete.usb.windows.WindowsUsbException.throwLastError;

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
            = _USB_DESCRIPTOR_REQUEST.layout().byteOffset(PathElement.groupElement("Data"));

    @Override
    protected void monitorDevices() {
        try (var arena = Arena.ofConfined()) {

            MemorySegment hwnd;
            var errorState = allocateErrorState(arena);

            try {
                final var className = Win.createSegmentFromString("USB_MONITOR", arena);
                final var windowName = Win.createSegmentFromString("USB device monitor", arena);
                final var instance = Kernel32.GetModuleHandleW(NULL);

                // create upcall for handling window messages
                var handleWindowMessageMH = MethodHandles.lookup().findVirtual(WindowsUsbDeviceRegistry.class,
                        "handleWindowMessage", MethodType.methodType(long.class, MemorySegment.class, int.class,
                                long.class, long.class)).bindTo(this);
                var handleWindowMessageStub = Linker.nativeLinker().upcallStub(handleWindowMessageMH,
                        FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG), arena);

                // register window class
                var wx = tagWNDCLASSEXW.allocate(arena);
                tagWNDCLASSEXW.cbSize(wx, (int) wx.byteSize());
                tagWNDCLASSEXW.lpfnWndProc(wx, handleWindowMessageStub);
                tagWNDCLASSEXW.hInstance(wx, instance);
                tagWNDCLASSEXW.lpszClassName(wx, className);
                var atom = User32B.RegisterClassExW(wx, errorState);
                if (atom == 0)
                    throwLastError(errorState, "internal error (RegisterClassExW)");

                // create message-only window
                hwnd = User32B.CreateWindowExW(0, className, windowName, 0, 0, 0, 0, 0, User32.HWND_MESSAGE(), NULL,
                        instance, NULL, errorState);
                if (hwnd.address() == 0)
                    throwLastError(errorState, "internal error (CreateWindowExW)");

                // configure notifications
                var notificationFilter = _DEV_BROADCAST_DEVICEINTERFACE_W.allocate(arena);
                _DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_size(notificationFilter, (int) notificationFilter.byteSize());
                _DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_devicetype(notificationFilter,
                        User32.DBT_DEVTYP_DEVICEINTERFACE());
                _DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_classguid(notificationFilter).copyFrom(GUID_DEVINTERFACE_USB_DEVICE);

                var notifyHandle = User32B.RegisterDeviceNotificationW(hwnd, notificationFilter,
                        User32.DEVICE_NOTIFY_WINDOW_HANDLE(), errorState);
                if (notifyHandle.address() == 0)
                    throwLastError(errorState, "internal error (RegisterDeviceNotificationW)");

                // initial device enumeration
                enumeratePresentDevices();

            } catch (Exception e) {
                enumerationFailed(e);
                return;
            }

            // process messages
            var msg = tagMSG.allocate(arena);
            int err;
            //noinspection StatementWithEmptyBody
            while ((err = User32B.GetMessageW(msg, hwnd, 0, 0, errorState)) > 0)
                ; // do nothing

            if (err == -1)
                throwLastError(errorState, "internal error (GetMessageW)");
        }
    }

    @SuppressWarnings("java:S106")
    private void enumeratePresentDevices() {

        List<UsbDevice> deviceList = new ArrayList<>();
        try (var cleanup = new ScopeCleanup();
             var deviceInfoSet = DeviceInfoSet.ofPresentDevices(GUID_DEVINTERFACE_USB_DEVICE, null)) {

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            cleanup.add(() -> hubHandles.forEach((_, handle) -> Kernel32.CloseHandle(handle)));

            // iterate all devices
            while (deviceInfoSet.next()) {

                var instanceId = deviceInfoSet.getStringProperty(InstanceId);
                var devicePath = DeviceInfoSet.getDevicePath(instanceId, GUID_DEVINTERFACE_USB_DEVICE);

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
                                                 HashMap<String, MemorySegment> hubHandles) {
        try (var arena = Arena.ofConfined()) {

            var usbPortNum = deviceInfoSet.getIntProperty(Address);
            var parentInstanceId = deviceInfoSet.getStringProperty(Parent);
            var hubPath = DeviceInfoSet.getDevicePath(parentInstanceId, GUID_DEVINTERFACE_USB_HUB);

            // open hub if not open yet
            var hubHandle = hubHandles.get(hubPath);
            if (hubHandle == null) {
                var hubPathSeg = Win.createSegmentFromString(hubPath, arena);
                var errorState = allocateErrorState(arena);
                hubHandle = Kernel32B.CreateFileW(hubPathSeg, Kernel32.GENERIC_WRITE(), Kernel32.FILE_SHARE_WRITE(),
                        NULL, Kernel32.OPEN_EXISTING(), 0, NULL, errorState);
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
            var connInfo = _USB_NODE_CONNECTION_INFORMATION_EX.allocate(arena);
            _USB_NODE_CONNECTION_INFORMATION_EX.ConnectionIndex(connInfo, usbPortNum);
            var sizeHolder = arena.allocate(JAVA_INT);
            var errorState = allocateErrorState(arena);
            if (Kernel32B.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX(),
                    connInfo, (int) connInfo.byteSize(), connInfo, (int) connInfo.byteSize(), sizeHolder, NULL,
                    errorState) == 0)
                throwLastError(errorState, "internal error (getting device descriptor failed)");

            var descriptorSegment = _USB_NODE_CONNECTION_INFORMATION_EX.DeviceDescriptor(connInfo);
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
        _USB_DESCRIPTOR_REQUEST.ConnectionIndex(descriptorRequest, usbPortNumber);
        var setupPacket = new SetupPacket(_USB_DESCRIPTOR_REQUEST.SetupPacket(descriptorRequest));
        setupPacket.setRequestType(0x80); // device-to-host / type standard / recipient device
        setupPacket.setRequest(UsbConstants.USB_REQUEST_GET_DESCRIPTOR);
        setupPacket.setValue((descriptorType << 8) | index);
        setupPacket.setIndex(languageID);
        setupPacket.setLength(size - (int) REQUEST_DATA_OFFSET);

        // execute request
        var effectiveSizeHolder = arena.allocate(JAVA_INT);
        var errorState = allocateErrorState(arena);
        if (Kernel32B.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION(),
                descriptorRequest, size, descriptorRequest, size, effectiveSizeHolder, NULL, errorState) == 0)
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

                } catch (UsbException e) {
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
                return new short[] { DEFAULT_LANGUAGE };
            return languages.asSlice(2, n * 2).toArray(JAVA_SHORT);
        } catch (UsbException e) {
            return new short[] { DEFAULT_LANGUAGE };
        }
    }

    @SuppressWarnings("java:S1144")
    private long handleWindowMessage(MemorySegment hWnd, int uMsg, long wParam, long lParam) {

        // check for message related to connecting/disconnecting devices
        if (uMsg == User32.WM_DEVICECHANGE() && (wParam == User32.DBT_DEVICEARRIVAL() || wParam == User32.DBT_DEVICEREMOVECOMPLETE())) {
            var data = MemorySegment.ofAddress(lParam).reinterpret(_DEV_BROADCAST_DEVICEINTERFACE_W.sizeof());
            if (_DEV_BROADCAST_HDR.dbch_devicetype(data) == User32.DBT_DEVTYP_DEVICEINTERFACE()) {

                // get device path
                var nameSlice =
                        MemorySegment.ofAddress(_DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_name(data).address()).reinterpret(500);
                var devicePath = Win.createStringFromSegment(nameSlice);
                if (wParam == User32.DBT_DEVICEARRIVAL())
                    onDeviceConnected(devicePath);
                else
                    onDeviceDisconnected(devicePath);
                return 0;
            }
        }

        // default message handling
        return User32.DefWindowProcW(hWnd, uMsg, wParam, lParam);
    }

    @SuppressWarnings("java:S106")
    private void onDeviceConnected(String devicePath) {
        try (var cleanup = new ScopeCleanup();
             var deviceInfoSet = DeviceInfoSet.ofPath(devicePath)) {

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            cleanup.add(() -> hubHandles.forEach((_, handle) -> Kernel32.CloseHandle(handle)));

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
