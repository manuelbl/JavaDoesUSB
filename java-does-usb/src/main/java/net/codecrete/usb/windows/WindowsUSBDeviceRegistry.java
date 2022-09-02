//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.USBDescriptors;
import net.codecrete.usb.common.USBDeviceImpl;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.common.USBStructs;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DATA;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVINFO_DATA;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;
import net.codecrete.usb.windows.gen.usbioctl.USBIoctl;
import net.codecrete.usb.windows.gen.user32.*;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

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
        try (var session = MemorySession.openConfined()) {

            final var className = Win.createSegmentFromString("USB_MONITOR", session);
            final var windowName = Win.createSegmentFromString("USB device monitor", session);
            final var instance = Kernel32.GetModuleHandleW(NULL);

            // create upcall for handling window messages
            var handleWindowMessageMH = MethodHandles.lookup().findVirtual(WindowsUSBDeviceRegistry.class,
                    "handleWindowMessage", MethodType.methodType(long.class, MemoryAddress.class, int.class,
                            long.class, long.class)).bindTo(this);
            var handleWindowMessageStub = Linker.nativeLinker().upcallStub(handleWindowMessageMH,
                    FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG), session);

            // register window class
            var wx = session.allocate(WNDCLASSEXW.$LAYOUT());
            WNDCLASSEXW.cbSize$set(wx, (int) wx.byteSize());
            WNDCLASSEXW.lpfnWndProc$set(wx, handleWindowMessageStub.address());
            WNDCLASSEXW.hInstance$set(wx, instance);
            WNDCLASSEXW.lpszClassName$set(wx, className.address());
            User32.RegisterClassExW(wx);

            // create message-only window
            Addressable hwnd = User32.CreateWindowExW(0, className, windowName, 0, 0, 0, 0, 0, User32.HWND_MESSAGE(),
                    NULL, instance, NULL);
            if (hwnd == NULL)
                throw new USBException("internal error (CreateWindowExW)", Kernel32.GetLastError());

            // configure notifications
            var notificationFilter = session.allocate(DEV_BROADCAST_DEVICEINTERFACE_W.$LAYOUT());
            DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_size$set(notificationFilter, (int) notificationFilter.byteSize());
            DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_devicetype$set(notificationFilter,
                    User32.DBT_DEVTYP_DEVICEINTERFACE());
            DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_classguid$slice(notificationFilter).copyFrom(USBHelper.GUID_DEVINTERFACE_USB_DEVICE);

            var notifyHandle = User32.RegisterDeviceNotificationW(hwnd, notificationFilter,
                    User32.DEVICE_NOTIFY_WINDOW_HANDLE());
            if (notifyHandle == NULL)
                throw new USBException("internal error (RegisterDeviceNotificationW)", Kernel32.GetLastError());

            // initial device enumeration
            enumeratePresentDevices();

            // process messages
            var msg = session.allocate(MSG.$LAYOUT());
            while (User32.GetMessageW(msg, hwnd, 0, 0) > 0)
                ; // do nothing

        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void enumeratePresentDevices() {

        List<USBDevice> deviceList = new ArrayList<>();

        try (var outerSession = MemorySession.openConfined()) {
            // get device information set of all USB devices present
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(USBHelper.GUID_DEVINTERFACE_USB_DEVICE, NULL,
                    NULL, SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throw new USBException("internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            outerSession.addCloseAction(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            var devInfo = MemorySegment.allocateNative(SP_DEVINFO_DATA.$LAYOUT(), outerSession);
            SP_DEVINFO_DATA.cbSize$set(devInfo, (int) SP_DEVINFO_DATA.$LAYOUT().byteSize());

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemoryAddress>();
            outerSession.addCloseAction(() -> hubHandles.forEach((path, handle) -> Kernel32.CloseHandle(handle)));

            // iterate all devices
            for (int i = 0; true; i++) {
                if (SetupAPI.SetupDiEnumDeviceInfo(devInfoSetHandle, i, devInfo) == 0) {
                    int err = Kernel32.GetLastError();
                    // TODO: Remove check for ERROR_SUCCESS if proper GetLastError() handling is available
                    if (err == Kernel32.ERROR_NO_MORE_ITEMS() || err == Kernel32.ERROR_SUCCESS())
                        break;
                    throw new USBException("Internal error (SetupDiEnumDeviceInfo) ");
                }

                var instanceID = DeviceProperty.getDeviceStringProperty(devInfoSetHandle, devInfo,
                        DeviceProperty.DEVPKEY_Device_InstanceId);
                var devicePath = DeviceProperty.getDevicePath(instanceID, USBHelper.GUID_DEVINTERFACE_USB_DEVICE);

                deviceList.add(createDeviceFromDeviceInfo(devInfoSetHandle, devInfo, devicePath, hubHandles));
            }

            setInitialDeviceList(deviceList);
        }
    }

    private USBDevice createDeviceFromDeviceInfo(MemoryAddress devInfoSetHandle, MemorySegment devInfo, String devicePath,
                                       HashMap<String, MemoryAddress> hubHandles) {
        try (var session = MemorySession.openConfined()) {

            var usbPortNum = DeviceProperty.getDeviceIntProperty(devInfoSetHandle, devInfo,
                    DeviceProperty.DEVPKEY_Device_Address);
            var parentInstanceID = DeviceProperty.getDeviceStringProperty(devInfoSetHandle, devInfo,
                    DeviceProperty.DEVPKEY_Device_Parent);
            var hubPath = DeviceProperty.getDevicePath(parentInstanceID, USBHelper.GUID_DEVINTERFACE_USB_HUB);

            // open hub if not open yet
            var hubHandle = hubHandles.get(hubPath);
            if (hubHandle == null) {
                var hubPathSeg = Win.createSegmentFromString(hubPath, session);
                hubHandle = Kernel32.CreateFileW(hubPathSeg, Kernel32.GENERIC_WRITE(), Kernel32.FILE_SHARE_WRITE(),
                        NULL, Kernel32.OPEN_EXISTING(), 0, NULL);
                if (Win.IsInvalidHandle(hubHandle))
                    throw new USBException("Cannot open USB hub", Kernel32.GetLastError());
                hubHandles.put(hubPath, hubHandle);
            }

            return createDevice(devicePath, hubHandle, usbPortNum);
        }
    }

    /**
     * Retrieve device descriptor and create {@code USBDevice} instance
     * @param devicePath the device path
     * @param hubHandle the hub handle (parent)
     * @param usbPortNum the USB port number
     * @return the {@code USBDevice} instance
     */
    private USBDevice createDevice(String devicePath, MemoryAddress hubHandle, int usbPortNum) {

        try (var session = MemorySession.openConfined()) {

            // get device descriptor
            var connInfo = session.allocate(USBHelper.USB_NODE_CONNECTION_INFORMATION_EX$Struct);
            USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_ConnectionIndex.set(connInfo, usbPortNum);
            var sizeHolder = session.allocate(JAVA_INT);
            if (Kernel32.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX(), connInfo
                    , (int) connInfo.byteSize(), connInfo, (int) connInfo.byteSize(), sizeHolder, NULL) == 0)
                throw new USBException("Internal error (cannot get device descriptor)", Kernel32.GetLastError());

            byte currentConfigurationValue =
                    (byte) USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_CurrentConfigurationValue.get(connInfo);

            var deviceDesc = connInfo.asSlice(USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$Offset,
                    USBDescriptors.Device$Struct.byteSize());

            // extract info from device descriptor
            int manufacturerIndex = 255 & (byte) USBDescriptors.Device_iManufacturer.get(deviceDesc);
            String manufacturer = getStringDescriptor(hubHandle, usbPortNum, manufacturerIndex);
            int productIndex = 255 & (byte) USBDescriptors.Device_iProduct.get(deviceDesc);
            String product = getStringDescriptor(hubHandle, usbPortNum, productIndex);
            int serialNumberIndex = 255 & (byte) USBDescriptors.Device_iSerialNumber.get(deviceDesc);
            String serialNumber = getStringDescriptor(hubHandle, usbPortNum, serialNumberIndex);

            int vendorId = 0xffff & (short) USBDescriptors.Device_idVendor.get(deviceDesc);
            int productId = 0xffff & (short) USBDescriptors.Device_idProduct.get(deviceDesc);
            int classCode = 255 & (byte) USBDescriptors.Device_bDeviceClass.get(deviceDesc);
            int subClassCode = 255 & (byte) USBDescriptors.Device_bDeviceSubClass.get(deviceDesc);
            int protocolCode = 255 & (byte) USBDescriptors.Device_bDeviceProtocol.get(deviceDesc);

            return new WindowsUSBDevice(devicePath, vendorId, productId, manufacturer, product, serialNumber,
                    classCode, subClassCode, protocolCode, currentConfigurationValue);
        }
    }

    private String getStringDescriptor(Addressable hubHandle, int usbPortNumber, int index) {
        if (index == 0)
            return null;

        try (var session = MemorySession.openConfined()) {

            final int dataLen = 255;
            var descriptorRequest = session.allocate(USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset + dataLen);
            USBHelper.USB_DESCRIPTOR_REQUEST_ConnectionIndex.set(descriptorRequest, usbPortNumber);
            var setupPacket = descriptorRequest.asSlice(USBHelper.USB_DESCRIPTOR_REQUEST_SetupPacket$Offset,
                    USBStructs.SetupPacket$Struct.byteSize());
            USBStructs.SetupPacket_bmRequest.set(setupPacket, (byte) 0x80);
            USBStructs.SetupPacket_bRequest.set(setupPacket, USBHelper.USB_REQUEST_GET_DESCRIPTOR);
            USBStructs.SetupPacket_wValue.set(setupPacket,
                    (short) ((USBHelper.USB_STRING_DESCRIPTOR_TYPE << 8) | index));
            USBStructs.SetupPacket_wIndex.set(setupPacket, (short) 0x0409);
            USBStructs.SetupPacket_wLength.set(setupPacket, (short) dataLen);

            var sizeHolder = session.allocate(JAVA_INT);
            if (Kernel32.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION(),
                    descriptorRequest, (int) descriptorRequest.byteSize(), descriptorRequest,
                    (int) descriptorRequest.byteSize(), sizeHolder, NULL) == 0)
                throw new USBException(String.format("Cannot retrieve string descriptor %d", index),
                        Kernel32.GetLastError());

            // The string descriptor is not null terminated
            var stringDesc = descriptorRequest.asSlice(USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset, dataLen);
            int stringLen = 255 & (byte) USBHelper.USB_STRING_DESCRIPTOR_bLength.get(stringDesc);
            var chars =
                    stringDesc.asSlice(USBHelper.USB_STRING_DESCRIPTOR_bString$Offset, stringLen - 2).toArray(JAVA_CHAR);
            return new String(chars);
        }
    }

    private long handleWindowMessage(MemoryAddress hWnd, int uMsg, long wParam, long lParam) {

        // check for message related to connecting/disconnecting devices
        if (uMsg == User32.WM_DEVICECHANGE() && (wParam == User32.DBT_DEVICEARRIVAL() || wParam == User32.DBT_DEVICEREMOVECOMPLETE())) {
            try (var session = MemorySession.openConfined()) {
                var data = MemorySegment.ofAddress(MemoryAddress.ofLong(lParam),
                        DEV_BROADCAST_DEVICEINTERFACE_W.sizeof(), session);
                if (DEV_BROADCAST_HDR.dbch_devicetype$get(data) == User32.DBT_DEVTYP_DEVICEINTERFACE()) {

                    // get device path
                    var nameSlice =
                            MemorySegment.ofAddress(DEV_BROADCAST_DEVICEINTERFACE_W.dbcc_name$slice(data).address(),
                                    500, session);
                    var devicePath = Win.createStringFromSegment(nameSlice);
                    if (wParam == User32.DBT_DEVICEARRIVAL())
                        onDeviceConnected(devicePath);
                    else
                        onDeviceDisconnected(devicePath);
                    return 0;
                }
            }
        }

        // default message handling
        return User32.DefWindowProcW(hWnd, uMsg, wParam, lParam);
    }

    private void onDeviceConnected(String devicePath) {
        try (var session = MemorySession.openConfined()) {

            // get device information set of all USB devices present
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(USBHelper.GUID_DEVINTERFACE_USB_DEVICE, NULL,
                    NULL, SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throw new USBException("internal error (SetupDiGetClassDevsW)", Kernel32.GetLastError());

            // ensure the result is destroyed when the scope is left
            session.addCloseAction(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            var devIntfData = session.allocate(SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            var devicePathSegment = Win.createSegmentFromString(devicePath, session);
            if (SetupAPI.SetupDiOpenDeviceInterfaceW(devInfoSetHandle, devicePathSegment, 0, devIntfData) == 0)
                throw new USBException("internal error (SetupDiOpenDeviceInterfaceW)", Kernel32.GetLastError());

            var devInfo = session.allocate(SP_DEVINFO_DATA.$LAYOUT());
            SP_DEVINFO_DATA.cbSize$set(devInfo, (int) devInfo.byteSize());
            if (SetupAPI.SetupDiGetDeviceInterfaceDetailW(devInfoSetHandle, devIntfData, NULL, 0, NULL, devInfo) == 0) {
                int err = Kernel32.GetLastError();
                if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
                    throw new USBException("internal error (SetupDiGetDeviceInterfaceDetailW)", err);
            }

            // ensure all hubs are closed later
            final var hubHandles = new HashMap<String, MemoryAddress>();
            session.addCloseAction(() -> hubHandles.forEach((path, handle) -> Kernel32.CloseHandle(handle)));

            // create device instance
            var device = createDeviceFromDeviceInfo(devInfoSetHandle, devInfo, devicePath, hubHandles);

            // add it to device list
            addDevice(device);
        }
    }

    private void onDeviceDisconnected(String devicePath) {
        // remove from device list
        removeDevice(devicePath);
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
}
