//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;
import net.codecrete.usb.usbstandard.DeviceDescriptor;
import net.codecrete.usb.usbstandard.SetupPacket;
import net.codecrete.usb.usbstandard.StringDescriptor;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.*;
import net.codecrete.usb.windows.gen.usbioctl.USBIoctl;
import net.codecrete.usb.windows.gen.usbioctl._USB_DESCRIPTOR_REQUEST;
import net.codecrete.usb.windows.gen.usbioctl._USB_NODE_CONNECTION_INFORMATION_EX;
import net.codecrete.usb.windows.gen.user32.*;
import net.codecrete.usb.windows.winsdk.Kernel32B;
import net.codecrete.usb.windows.winsdk.SetupAPI2;
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
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.usbstandard.Constants.*;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUSBException.throwException;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

/**
 * Windows implementation of USB device registry.
 * <p>
 * To retrieve details of a USB device, this class accesses it indirectly
 * via the parent. To address it the parent's handle (<i>hub handle</i>) and
 * the device's port number is needed.
 * </p>
 */
public class WindowsUSBDeviceRegistry extends USBDeviceRegistry {

    private static final long REQUEST_DATA_OFFSET
            = _USB_DESCRIPTOR_REQUEST.$LAYOUT().byteOffset(PathElement.groupElement("Data"));

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
                var handleWindowMessageMH = MethodHandles.lookup().findVirtual(WindowsUSBDeviceRegistry.class,
                        "handleWindowMessage", MethodType.methodType(long.class, MemorySegment.class, int.class,
                                long.class, long.class)).bindTo(this);
                var handleWindowMessageStub = Linker.nativeLinker().upcallStub(handleWindowMessageMH,
                        FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG), arena);

                // register window class
                var wx = arena.allocate(tagWNDCLASSEXW.$LAYOUT());
                tagWNDCLASSEXW.cbSize$set(wx, (int) wx.byteSize());
                tagWNDCLASSEXW.lpfnWndProc$set(wx, handleWindowMessageStub);
                tagWNDCLASSEXW.hInstance$set(wx, instance);
                tagWNDCLASSEXW.lpszClassName$set(wx, className);
                var atom = User32B.RegisterClassExW(wx, errorState);
                if (atom == 0)
                    throwLastError(errorState, "Internal error (RegisterClassExW)");

                // create message-only window
                hwnd = User32B.CreateWindowExW(0, className, windowName, 0, 0, 0, 0, 0, User32.HWND_MESSAGE(), NULL,
                        instance, NULL, errorState);
                if (hwnd.address() == 0)
                    throwLastError(errorState, "Internal error (CreateWindowExW)");

                // configure notifications
                var notificationFilter = arena.allocate(_DEV_BROADCAST_DEVICEINTERFACE_W.$LAYOUT());
                _DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_size$set(notificationFilter, (int) notificationFilter.byteSize());
                _DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_devicetype$set(notificationFilter,
                        User32.DBT_DEVTYP_DEVICEINTERFACE());
                _DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_classguid$slice(notificationFilter).copyFrom(USBConstants.GUID_DEVINTERFACE_USB_DEVICE);

                var notifyHandle = User32B.RegisterDeviceNotificationW(hwnd, notificationFilter,
                        User32.DEVICE_NOTIFY_WINDOW_HANDLE(), errorState);
                if (notifyHandle.address() == 0)
                    throwLastError(errorState, "Internal error (RegisterDeviceNotificationW)");

                // initial device enumeration
                enumeratePresentDevices();

            } catch (Exception e) {
                enumerationFailed(e);
                return;
            }

            // process messages
            var msg = arena.allocate(tagMSG.$LAYOUT());
            int err;
            //noinspection StatementWithEmptyBody
            while ((err = User32B.GetMessageW(msg, hwnd, 0, 0, errorState)) > 0)
                ; // do nothing

            if (err == -1)
                throwLastError(errorState, "Internal error (GetMessageW)");
        }
    }

    @SuppressWarnings("java:S106")
    private void enumeratePresentDevices() {

        List<USBDevice> deviceList = new ArrayList<>();
        try (var outerArena = Arena.ofConfined(); var outerCleanup = new ScopeCleanup()) {

            var errorState = allocateErrorState(outerArena);

            // get device information set of all USB devices present
            var devInfoSet = SetupAPI2.SetupDiGetClassDevsW(USBConstants.GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE(), errorState);
            if (Win.isInvalidHandle(devInfoSet))
                throwLastError(errorState, "Internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            outerCleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

            // allocate SP_DEVINFO_DATA (will receive details for each device)
            var devInfoData = outerArena.allocate(_SP_DEVINFO_DATA.$LAYOUT());
            _SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) _SP_DEVINFO_DATA.$LAYOUT().byteSize());

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            outerCleanup.add(() -> hubHandles.forEach((path, handle) -> Kernel32.CloseHandle(handle)));

            // iterate all devices
            for (var i = 0; true; i++) {

                if (SetupAPI2.SetupDiEnumDeviceInfo(devInfoSet, i, devInfoData, errorState) == 0) {
                    var err = Win.getLastError(errorState);
                    if (err == Kernel32.ERROR_NO_MORE_ITEMS())
                        break;
                    throwLastError(errorState, "Internal error (SetupDiEnumDeviceInfo)");
                }

                var instanceId = DeviceProperty.getDeviceStringProperty(devInfoSet, devInfoData,
                        DeviceProperty.DEVPKEY_Device_InstanceId);
                var devicePath = DeviceProperty.getDevicePath(instanceId, USBConstants.GUID_DEVINTERFACE_USB_DEVICE);

                try {
                    deviceList.add(createDeviceFromDeviceInfo(devInfoSet, devInfoData, devicePath, hubHandles));

                } catch (Exception e) {
                    System.err.printf("Info: [JavaDoesUSB] failed to retrieve information about device %s - ignoring "
                            + "device%n", devicePath);
                    e.printStackTrace(System.err);
                }
            }

            setInitialDeviceList(deviceList);
        }
    }

    /**
     * Enumerate the children of a composite device.
     *
     * @param childrenIds the children IDs
     * @return map of containing children device paths, index by the first interface number
     */
    private Map<Integer, String> enumerateChildren(List<String> childrenIds) {

        var children = new HashMap<Integer, String>();

        // iterate all children
        for (var instanceId : childrenIds) {
            try (var arena = Arena.ofConfined(); var cleanup = new ScopeCleanup()) {

                //noinspection DuplicatedCode
                var errorState = allocateErrorState(arena);

                // create empty device info set
                var devInfoSet = SetupAPI2.SetupDiCreateDeviceInfoList(NULL, NULL, errorState);
                if (Win.isInvalidHandle(devInfoSet))
                    throwLastError(errorState, "Internal error (SetupDiCreateDeviceInfoList)");
                cleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

                // get device info for child
                var devInfoData = arena.allocate(_SP_DEVINFO_DATA.$LAYOUT());
                _SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) devInfoData.byteSize());
                var instanceIdSegment = Win.createSegmentFromString(instanceId, arena);
                if (SetupAPI2.SetupDiOpenDeviceInfoW(devInfoSet, instanceIdSegment, NULL, 0, devInfoData,
                        errorState) == 0)
                    throwLastError(errorState, "Internal error (SetupDiOpenDeviceInfoW)");

                // get hardware IDs (to extract interface number)
                var hardwareIds = DeviceProperty.getDeviceStringListProperty(devInfoSet, devInfoData,
                        DeviceProperty.DEVPKEY_Device_HardwareIds);
                if (hardwareIds == null)
                    throwException("DEVPKEY_Device_HardwareIds not found");
                var interfaceNumber = extractInterfaceNumber(hardwareIds);
                if (interfaceNumber == -1)
                    continue;

                var devicePath = DeviceProperty.getDevicePathByGUID(devInfoSet, devInfoData, instanceId, arena);
                if (devicePath != null)
                    children.put(interfaceNumber, devicePath);

            }
        }

        return children;
    }


    private USBDevice createDeviceFromDeviceInfo(MemorySegment devInfoSet, MemorySegment devInfoData,
                                                 String devicePath, HashMap<String, MemorySegment> hubHandles) {
        try (var arena = Arena.ofConfined()) {

            var usbPortNum = DeviceProperty.getDeviceIntProperty(devInfoSet, devInfoData,
                    DeviceProperty.DEVPKEY_Device_Address);
            var parentInstanceId = DeviceProperty.getDeviceStringProperty(devInfoSet, devInfoData,
                    DeviceProperty.DEVPKEY_Device_Parent);
            var hubPath = DeviceProperty.getDevicePath(parentInstanceId, USBConstants.GUID_DEVINTERFACE_USB_HUB);

            // open hub if not open yet
            var hubHandle = hubHandles.get(hubPath);
            if (hubHandle == null) {
                var hubPathSeg = Win.createSegmentFromString(hubPath, arena);
                var errorState = allocateErrorState(arena);
                hubHandle = Kernel32B.CreateFileW(hubPathSeg, Kernel32.GENERIC_WRITE(), Kernel32.FILE_SHARE_WRITE(),
                        NULL, Kernel32.OPEN_EXISTING(), 0, NULL, errorState);
                if (Win.isInvalidHandle(hubHandle))
                    throwLastError(errorState, "Cannot open USB hub");
                hubHandles.put(hubPath, hubHandle);
            }

            // check for composite device
            var children = getChildDevices(devInfoSet, devInfoData, devicePath);

            return createDevice(devicePath, children, hubHandle, usbPortNum);
        }
    }

    @SuppressWarnings({"java:S106", "java:S1168"})
    private Map<Integer, String> getChildDevices(MemorySegment devInfoSet, MemorySegment devInfoData, String devicePath) {
        if (!DeviceProperty.isCompositeDevice(devInfoSet, devInfoData))
            return null;

        // For certain devices, it seems to take some time until the "Device_Children"
        // entry is present. So we retry a few times if needed and pause in between.
        List<String> childrenInstanceIDs;
        var numTries = 5;
        while (true) {
            numTries -= 1;
            childrenInstanceIDs = DeviceProperty.getDeviceStringListProperty(devInfoSet, devInfoData,
                    DeviceProperty.DEVPKEY_Device_Children);
            if (childrenInstanceIDs != null || numTries == 0)
                break;

            // sleep and retry
            try {
                //noinspection BusyWait
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (childrenInstanceIDs == null) {
            System.err.printf("Info: [JavaDoesUSB] cannot get device children of device %s%n", devicePath);
            return null;
        }

        return enumerateChildren(childrenInstanceIDs);
    }

    /**
     * Retrieve device descriptor and create {@code USBDevice} instance
     *
     * @param devicePath the device path
     * @param children   map of child device paths, indexed by the first interface number
     * @param hubHandle  the hub handle (parent)
     * @param usbPortNum the USB port number
     * @return the {@code USBDevice} instance
     */
    private USBDevice createDevice(String devicePath, Map<Integer, String> children, MemorySegment hubHandle,
                                   int usbPortNum) {

        try (var arena = Arena.ofConfined()) {

            // get device descriptor
            var connInfo = _USB_NODE_CONNECTION_INFORMATION_EX.allocate(arena);
            _USB_NODE_CONNECTION_INFORMATION_EX.ConnectionIndex$set(connInfo, usbPortNum);
            var sizeHolder = arena.allocate(JAVA_INT);
            var errorState = allocateErrorState(arena);
            if (Kernel32B.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX(),
                    connInfo, (int) connInfo.byteSize(), connInfo, (int) connInfo.byteSize(), sizeHolder, NULL,
                    errorState) == 0)
                throwLastError(errorState, "Internal error (cannot get device descriptor)");

            var descriptorSegment = _USB_NODE_CONNECTION_INFORMATION_EX.DeviceDescriptor$slice(connInfo);
            var deviceDescriptor = new DeviceDescriptor(descriptorSegment);

            var vendorId = deviceDescriptor.vendorID();
            var productId = deviceDescriptor.productID();

            var configDesc = getDescriptor(hubHandle, usbPortNum, CONFIGURATION_DESCRIPTOR_TYPE, 0, (short) 0, arena);

            var device = new WindowsUSBDevice(devicePath, children, vendorId, productId, configDesc);
            device.setFromDeviceDescriptor(descriptorSegment);
            device.setProductString(descriptorSegment, index -> getStringDescriptor(hubHandle, usbPortNum, index));

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
        _USB_DESCRIPTOR_REQUEST.ConnectionIndex$set(descriptorRequest, usbPortNumber);
        var setupPacket = new SetupPacket(_USB_DESCRIPTOR_REQUEST.SetupPacket$slice(descriptorRequest));
        setupPacket.setRequestType(0x80); // device-to-host / type standard / recipient device
        setupPacket.setRequest(USBConstants.USB_REQUEST_GET_DESCRIPTOR);
        setupPacket.setValue((descriptorType << 8) | index);
        setupPacket.setIndex(languageID);
        setupPacket.setLength(size - (int) REQUEST_DATA_OFFSET);

        // execute request
        var effectiveSizeHolder = arena.allocate(JAVA_INT);
        var errorState = allocateErrorState(arena);
        if (Kernel32B.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION(),
                descriptorRequest, size, descriptorRequest, size, effectiveSizeHolder, NULL, errorState) == 0)
            throwLastError(errorState, "Cannot retrieve descriptor %d", index);

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
                throwException("Unexpected descriptor size");

            // repeat with correct size
            return getDescriptor(hubHandle, usbPortNumber, descriptorType, index, languageID, expectedSize, arena);
        }

        return descriptorRequest.asSlice(REQUEST_DATA_OFFSET, effectiveSize);
    }

    @SuppressWarnings("java:S106")
    private String getStringDescriptor(MemorySegment hubHandle, int usbPortNumber, int index) {
        if (index == 0)
            return null;

        try (var arena = Arena.ofConfined()) {
            var stringDesc = new StringDescriptor(getDescriptor(hubHandle, usbPortNumber, STRING_DESCRIPTOR_TYPE,
                    index, DEFAULT_LANGUAGE, arena));
            return stringDesc.string();

        } catch (USBException e) {
            System.err.printf("Info: [JavaDoesUSB] failed to retrieve string descriptor %d (%s) - ignoring%n", index,
                    e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("java:S1144")
    private long handleWindowMessage(MemorySegment hWnd, int uMsg, long wParam, long lParam) {

        // check for message related to connecting/disconnecting devices
        if (uMsg == User32.WM_DEVICECHANGE() && (wParam == User32.DBT_DEVICEARRIVAL() || wParam == User32.DBT_DEVICEREMOVECOMPLETE())) {
            var data = MemorySegment.ofAddress(lParam).reinterpret(_DEV_BROADCAST_DEVICEINTERFACE_W.sizeof());
            if (_DEV_BROADCAST_HDR.dbch_devicetype$get(data) == User32.DBT_DEVTYP_DEVICEINTERFACE()) {

                // get device path
                var nameSlice =
                        MemorySegment.ofAddress(_DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_name$slice(data).address()).reinterpret(500);
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
        try (var arena = Arena.ofConfined(); var cleanup = new ScopeCleanup()) {

            var errorState = allocateErrorState(arena);

            // create empty device info set
            var devInfoSet = SetupAPI2.SetupDiCreateDeviceInfoList(NULL, NULL, errorState);
            if (Win.isInvalidHandle(devInfoSet))
                throwLastError(errorState, "Internal error (SetupDiCreateDeviceInfoList)");

            cleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

            // load device information into dev info set
            var devIntfData = arena.allocate(_SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            _SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            var devicePathSegment = Win.createSegmentFromString(devicePath, arena);
            if (SetupAPI2.SetupDiOpenDeviceInterfaceW(devInfoSet, devicePathSegment, 0, devIntfData, errorState) == 0)
                throwLastError(errorState, "Internal error (SetupDiOpenDeviceInterfaceW)");

            cleanup.add(() -> SetupAPI.SetupDiDeleteDeviceInterfaceData(devInfoSet, devIntfData));

            var devInfoData = arena.allocate(_SP_DEVINFO_DATA.$LAYOUT());
            _SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) devInfoData.byteSize());
            if (SetupAPI2.SetupDiGetDeviceInterfaceDetailW(devInfoSet, devIntfData, NULL, 0, NULL, devInfoData,
                    errorState) == 0) {
                var err = Win.getLastError(errorState);
                if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
                    throwException(err, "Internal error (SetupDiGetDeviceInterfaceDetailW)");
            }

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            cleanup.add(() -> hubHandles.forEach((path, handle) -> Kernel32.CloseHandle(handle)));

            try {
                // create device instance
                var device = createDeviceFromDeviceInfo(devInfoSet, devInfoData, devicePath, hubHandles);

                // add it to device list
                addDevice(device);

            } catch (Exception e) {
                System.err.printf("Info: [JavaDoesUSB] failed to retrieve information about device %s - ignoring " +
                        "device%n", devicePath);
                e.printStackTrace(System.err);
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
    protected int findDeviceIndex(List<USBDevice> deviceList, Object deviceId) {
        var id = deviceId.toString();
        for (var i = 0; i < deviceList.size(); i++) {
            var dev = (USBDeviceImpl) deviceList.get(i);
            if (id.equalsIgnoreCase(dev.getUniqueId().toString()))
                return i;
        }
        return -1;
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
