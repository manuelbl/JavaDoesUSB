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
import net.codecrete.usb.windows.gen.kernel32.GUID;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.ole32.Ole32;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DATA;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVINFO_DATA;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;
import net.codecrete.usb.windows.gen.usbioctl.USBIoctl;
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

    @Override
    protected void monitorDevices() {
        try (var arena = Arena.ofConfined()) {

            MemorySegment hwnd;
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

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
                var wx = arena.allocate(WNDCLASSEXW.$LAYOUT());
                WNDCLASSEXW.cbSize$set(wx, (int) wx.byteSize());
                WNDCLASSEXW.lpfnWndProc$set(wx, handleWindowMessageStub);
                WNDCLASSEXW.hInstance$set(wx, instance);
                WNDCLASSEXW.lpszClassName$set(wx, className);
                var atom = User32B.RegisterClassExW(wx, lastErrorState);
                if (atom == 0)
                    throwLastError(lastErrorState, "Internal error (RegisterClassExW)");

                // create message-only window
                hwnd = User32B.CreateWindowExW(0, className, windowName, 0, 0, 0, 0, 0, User32.HWND_MESSAGE(), NULL,
                        instance, NULL, lastErrorState);
                if (hwnd.address() == 0)
                    throwLastError(lastErrorState, "Internal error (CreateWindowExW)");

                // configure notifications
                var notificationFilter = arena.allocate(DEV_BROADCAST_DEVICEINTERFACE_W.$LAYOUT());
                DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_size$set(notificationFilter, (int) notificationFilter.byteSize());
                DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_devicetype$set(notificationFilter,
                        User32.DBT_DEVTYP_DEVICEINTERFACE());
                DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_classguid$slice(notificationFilter).copyFrom(USBHelper.GUID_DEVINTERFACE_USB_DEVICE);

                var notifyHandle = User32B.RegisterDeviceNotificationW(hwnd, notificationFilter,
                        User32.DEVICE_NOTIFY_WINDOW_HANDLE(), lastErrorState);
                if (notifyHandle.address() == 0)
                    throwLastError(lastErrorState, "Internal error (RegisterDeviceNotificationW)");

                // initial device enumeration
                enumeratePresentDevices();

            } catch (Throwable e) {
                enumerationFailed(e);
                return;
            }

            // process messages
            var msg = arena.allocate(MSG.$LAYOUT());
            int err;
            //noinspection StatementWithEmptyBody
            while ((err = User32B.GetMessageW(msg, hwnd, 0, 0, lastErrorState)) > 0)
                ; // do nothing

            if (err == -1)
                throwLastError(lastErrorState, "Internal error (GetMessageW)");
        }
    }

    private void enumeratePresentDevices() {

        List<USBDevice> deviceList = new ArrayList<>();
        try (var outerArena = Arena.ofConfined(); var outerCleanup = new ScopeCleanup()) {

            var lastErrorState = outerArena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            // get device information set of all USB devices present
            var devInfoSet = SetupAPI2.SetupDiGetClassDevsW(USBHelper.GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE(), lastErrorState);
            if (Win.IsInvalidHandle(devInfoSet))
                throwLastError(lastErrorState, "Internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            outerCleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

            // allocate SP_DEVINFO_DATA (will receive details for each device)
            var devInfoData = outerArena.allocate(SP_DEVINFO_DATA.$LAYOUT());
            SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) SP_DEVINFO_DATA.$LAYOUT().byteSize());

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemorySegment>();
            outerCleanup.add(() -> hubHandles.forEach((path, handle) -> Kernel32.CloseHandle(handle)));

            // iterate all devices
            for (int i = 0; true; i++) {

                if (SetupAPI2.SetupDiEnumDeviceInfo(devInfoSet, i, devInfoData, lastErrorState) == 0) {
                    int err = Win.getLastError(lastErrorState);
                    if (err == Kernel32.ERROR_NO_MORE_ITEMS())
                        break;
                    throwLastError(lastErrorState, "Internal error (SetupDiEnumDeviceInfo)");
                }

                var instanceId = DeviceProperty.getDeviceStringProperty(devInfoSet, devInfoData,
                        DeviceProperty.DEVPKEY_Device_InstanceId);
                var devicePath = DeviceProperty.getDevicePath(instanceId, USBHelper.GUID_DEVINTERFACE_USB_DEVICE);

                try {
                    deviceList.add(createDeviceFromDeviceInfo(devInfoSet, devInfoData, devicePath, hubHandles));

                } catch (Throwable e) {
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

                var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

                // create empty device info set
                var devInfoSet = SetupAPI2.SetupDiCreateDeviceInfoList(NULL, NULL, lastErrorState);
                if (Win.IsInvalidHandle(devInfoSet))
                    throwLastError(lastErrorState, "Internal error (SetupDiCreateDeviceInfoList)");
                cleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

                // get device info for child
                var devInfoData = arena.allocate(SP_DEVINFO_DATA.$LAYOUT());
                SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) devInfoData.byteSize());
                var instanceIdSegment = Win.createSegmentFromString(instanceId, arena);
                if (SetupAPI2.SetupDiOpenDeviceInfoW(devInfoSet, instanceIdSegment, NULL, 0, devInfoData,
                        lastErrorState) == 0)
                    throwLastError(lastErrorState, "Internal error (SetupDiOpenDeviceInfoW)");

                // get hardware IDs (to extract interface number)
                var hardwareIds = DeviceProperty.getDeviceStringListProperty(devInfoSet, devInfoData,
                        DeviceProperty.DEVPKEY_Device_HardwareIds);
                if (hardwareIds == null)
                    throwException("DEVPKEY_Device_HardwareIds not found");
                int interfaceNumber = extractInterfaceNumber(hardwareIds);
                if (interfaceNumber == -1)
                    continue;

                var guids = DeviceProperty.findDeviceInterfaceGUIDs(devInfoSet, devInfoData, arena);

                for (var guid : guids) {
                    // check for Class GUID
                    var guidSegment = Win.createSegmentFromString(guid, arena);
                    var clsid = arena.allocate(GUID.$LAYOUT());
                    if (Ole32.CLSIDFromString(guidSegment, clsid) != 0)
                        continue;

                    try {
                        var devicePath = DeviceProperty.getDevicePath(instanceId, clsid);
                        children.put(interfaceNumber, devicePath);
                        break;
                    } catch (Exception e) {
                        // ignore and try next one
                    }
                }
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
            var hubPath = DeviceProperty.getDevicePath(parentInstanceId, USBHelper.GUID_DEVINTERFACE_USB_HUB);

            // open hub if not open yet
            var hubHandle = hubHandles.get(hubPath);
            if (hubHandle == null) {
                var hubPathSeg = Win.createSegmentFromString(hubPath, arena);
                var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
                hubHandle = Kernel32B.CreateFileW(hubPathSeg, Kernel32.GENERIC_WRITE(), Kernel32.FILE_SHARE_WRITE(),
                        NULL, Kernel32.OPEN_EXISTING(), 0, NULL, lastErrorState);
                if (Win.IsInvalidHandle(hubHandle))
                    throwLastError(lastErrorState, "Cannot open USB hub");
                hubHandles.put(hubPath, hubHandle);
            }

            // check for composite device
            Map<Integer, String> children = null;
            if (DeviceProperty.isCompositeDevice(devInfoSet, devInfoData)) {

                // For certain devices, it seems to take some time until the "Device_Children"
                // entry is present. So we retry a few times if needed and pause in between.
                List<String> childrenInstanceIDs;
                int numTries = 5;
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
                        // ignore
                    }
                }

                if (childrenInstanceIDs == null) {
                    System.err.printf("Info: [JavaDoesUSB] cannot get device children of device %s%n", devicePath);
                } else {
                    children = enumerateChildren(childrenInstanceIDs);
                }
            }

            return createDevice(devicePath, children, hubHandle, usbPortNum);
        }
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
            var connInfo = arena.allocate(USBHelper.USB_NODE_CONNECTION_INFORMATION_EX$Struct);
            USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_ConnectionIndex.set(connInfo, usbPortNum);
            var sizeHolder = arena.allocate(JAVA_INT);
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            if (Kernel32B.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX(),
                    connInfo, (int) connInfo.byteSize(), connInfo, (int) connInfo.byteSize(), sizeHolder, NULL,
                    lastErrorState) == 0)
                throwLastError(lastErrorState, "Internal error (cannot get device descriptor)");

            var descriptorSegment = USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$slice(connInfo);
            var deviceDescriptor = new DeviceDescriptor(descriptorSegment);

            int vendorId = deviceDescriptor.vendorID();
            int productId = deviceDescriptor.productID();

            var configDesc = getDescriptor(hubHandle, usbPortNum, CONFIGURATION_DESCRIPTOR_TYPE, 0, (short) 0, arena);

            var device = new WindowsUSBDevice(devicePath, children, vendorId, productId, configDesc);
            device.setFromDeviceDescriptor(descriptorSegment);
            device.setProductString(descriptorSegment, (index) -> getStringDescriptor(hubHandle, usbPortNum, index));

            return device;
        }
    }

    private MemorySegment getDescriptor(MemorySegment hubHandle, int usbPortNumber, int descriptorType, int index,
                                        short languageID, Arena arena) {
        return getDescriptor(hubHandle, usbPortNumber, descriptorType, index, languageID, 0, arena);

    }

    private MemorySegment getDescriptor(MemorySegment hubHandle, int usbPortNumber, int descriptorType, int index,
                                        short languageID, int requestSize, Arena arena) {

        int size = requestSize != 0 ? requestSize + (int) USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset : 256;

        // create descriptor requests
        var descriptorRequest = arena.allocate(size);
        USBHelper.USB_DESCRIPTOR_REQUEST_ConnectionIndex.set(descriptorRequest, usbPortNumber);
        var setupPacket =
                new SetupPacket(descriptorRequest.asSlice(USBHelper.USB_DESCRIPTOR_REQUEST_SetupPacket$Offset,
                        SetupPacket.LAYOUT.byteSize()));
        setupPacket.setRequestType(0x80); // device-to-host / type standard / recipient device
        setupPacket.setRequest(USBHelper.USB_REQUEST_GET_DESCRIPTOR);
        setupPacket.setValue((descriptorType << 8) | index);
        setupPacket.setIndex(languageID);
        setupPacket.setLength(size - (int) USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset);

        // execute request
        var effectiveSizeHolder = arena.allocate(JAVA_INT);
        var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
        if (Kernel32B.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION(),
                descriptorRequest, size, descriptorRequest, size, effectiveSizeHolder, NULL, lastErrorState) == 0)
            throwLastError(lastErrorState, "Cannot retrieve descriptor %d", index);

        // determine size of descriptor
        int expectedSize;
        if (descriptorType != CONFIGURATION_DESCRIPTOR_TYPE) {
            expectedSize = 255 & descriptorRequest.get(JAVA_BYTE, USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset);
        } else {
            var configDesc =
                    new ConfigurationDescriptor(descriptorRequest.asSlice(USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset, ConfigurationDescriptor.LAYOUT.byteSize()));
            expectedSize = configDesc.totalLength();
        }

        // check against effective size
        var effectiveSize = effectiveSizeHolder.get(JAVA_INT, 0) - USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset;
        if (effectiveSize != expectedSize) {
            if (requestSize != 0)
                throwException("Unexpected descriptor size");

            // repeat with correct size
            return getDescriptor(hubHandle, usbPortNumber, descriptorType, index, languageID, expectedSize, arena);
        }

        return descriptorRequest.asSlice(USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset, effectiveSize);
    }

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

    private long handleWindowMessage(MemorySegment hWnd, int uMsg, long wParam, long lParam) {

        // check for message related to connecting/disconnecting devices
        if (uMsg == User32.WM_DEVICECHANGE() && (wParam == User32.DBT_DEVICEARRIVAL() || wParam == User32.DBT_DEVICEREMOVECOMPLETE())) {
            var data = MemorySegment.ofAddress(lParam).reinterpret(DEV_BROADCAST_DEVICEINTERFACE_W.sizeof());
            if (DEV_BROADCAST_HDR.dbch_devicetype$get(data) == User32.DBT_DEVTYP_DEVICEINTERFACE()) {

                // get device path
                var nameSlice =
                        MemorySegment.ofAddress(DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_name$slice(data).address()).reinterpret(500);
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

    private void onDeviceConnected(String devicePath) {
        try (var arena = Arena.ofConfined(); var cleanup = new ScopeCleanup()) {

            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            // create empty device info set
            var devInfoSet = SetupAPI2.SetupDiCreateDeviceInfoList(NULL, NULL, lastErrorState);
            if (Win.IsInvalidHandle(devInfoSet))
                throwLastError(lastErrorState, "Internal error (SetupDiCreateDeviceInfoList)");

            cleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

            // load device information into dev info set
            var devIntfData = arena.allocate(SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            var devicePathSegment = Win.createSegmentFromString(devicePath, arena);
            if (SetupAPI2.SetupDiOpenDeviceInterfaceW(devInfoSet, devicePathSegment, 0, devIntfData, lastErrorState) == 0)
                throwLastError(lastErrorState, "Internal error (SetupDiOpenDeviceInterfaceW)");

            cleanup.add(() -> SetupAPI.SetupDiDeleteDeviceInterfaceData(devInfoSet, devIntfData));

            var devInfoData = arena.allocate(SP_DEVINFO_DATA.$LAYOUT());
            SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) devInfoData.byteSize());
            if (SetupAPI2.SetupDiGetDeviceInterfaceDetailW(devInfoSet, devIntfData, NULL, 0, NULL, devInfoData,
                    lastErrorState) == 0) {
                int err = Win.getLastError(lastErrorState);
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

            } catch (Throwable e) {
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
        for (int i = 0; i < deviceList.size(); i++) {
            var dev = (USBDeviceImpl) deviceList.get(i);
            if (id.equalsIgnoreCase(dev.getUniqueId().toString()))
                return i;
        }
        return -1;
    }

    private static final Pattern MULTIPLE_INTERFACE_ID = Pattern.compile("USB\\\\VID_[0-9A-Fa-f]{4}&PID_[0-9A-Fa-f]{4" +
            "}&MI_([0-9A-Fa-f]{2})");

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
