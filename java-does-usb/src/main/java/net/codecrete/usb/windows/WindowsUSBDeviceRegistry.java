//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.USBDescriptors;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.common.USBStructs;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;
import net.codecrete.usb.windows.gen.setupapi._SP_DEVINFO_DATA;
import net.codecrete.usb.windows.gen.usbioctl.USBIoctl;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class WindowsUSBDeviceRegistry implements USBDeviceRegistry {

    public List<USBDeviceInfo> getAllDevices() {

        List<USBDeviceInfo> result = new ArrayList<>();

        try (var outerSession = MemorySession.openConfined()) {
            // get device information set of all USB devices present
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(
                    USBHelper.GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throw new USBException("internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            outerSession.addCloseAction(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            var devInfo = MemorySegment.allocateNative(_SP_DEVINFO_DATA.$LAYOUT(), outerSession);
            _SP_DEVINFO_DATA.cbSize$set(devInfo, (int) _SP_DEVINFO_DATA.$LAYOUT().byteSize());

            // iterate all devices
            for (int i = 0; true; i++) {
                if (SetupAPI.SetupDiEnumDeviceInfo(devInfoSetHandle, i, devInfo) == 0) {
                    int err = Kernel32.GetLastError();
                    if (err == Kernel32.ERROR_NO_MORE_ITEMS() || err == Kernel32.ERROR_SUCCESS())
                        break;
                    throw new USBException("Internal error (SetupDiEnumDeviceInfo) ");
                }

                try (var session = MemorySession.openConfined()) {

                    var usbPortNum = DeviceProperty.getDeviceIntProperty(devInfoSetHandle, devInfo, DeviceProperty.DEVPKEY_Device_Address);
                    var instanceID = DeviceProperty.getDeviceStringProperty(devInfoSetHandle, devInfo, DeviceProperty.DEVPKEY_Device_InstanceId);
                    var devicePath = DeviceProperty.getDevicePath(instanceID, USBHelper.GUID_DEVINTERFACE_USB_DEVICE);
                    var parentInstanceID = DeviceProperty.getDeviceStringProperty(devInfoSetHandle, devInfo, DeviceProperty.DEVPKEY_Device_Parent);
                    var hubPath = DeviceProperty.getDevicePath(parentInstanceID, USBHelper.GUID_DEVINTERFACE_USB_HUB);

                    // open hub
                    var hubPathSeg = session.allocateArray(JAVA_CHAR, hubPath.length() + 2);
                    hubPathSeg.copyFrom(MemorySegment.ofArray(hubPath.toCharArray()));
                    final var hubHandle = Kernel32.CreateFileW(hubPathSeg, Kernel32.GENERIC_WRITE(), Kernel32.FILE_SHARE_WRITE(),
                            NULL, Kernel32.OPEN_EXISTING(), 0, NULL);
                    if (Win.IsInvalidHandle(hubHandle))
                        throw new USBException("Cannot open USB hub", Kernel32.GetLastError());

                    // ensure the hub is closed later
                    session.addCloseAction(() -> Kernel32.CloseHandle(hubHandle));

                    result.add(createDeviceInfo(devicePath, hubHandle, usbPortNum));
                }
            }

            return result;
        }
    }

    private USBDeviceInfo createDeviceInfo(String devicePath, MemoryAddress hubHandle, int usbPortNum) {

        try (var session = MemorySession.openConfined()) {

            // get device descriptor
            var connInfo = session.allocate(USBHelper.USB_NODE_CONNECTION_INFORMATION_EX$Struct);
            USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_ConnectionIndex.set(connInfo, usbPortNum);
            var sizeHolder = session.allocate(JAVA_INT);
            if (Kernel32.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX(),
                    connInfo, (int) connInfo.byteSize(),
                    connInfo, (int) connInfo.byteSize(),
                    sizeHolder, NULL) == 0)
                throw new USBException("Internal error (cannot get device descriptor)", Kernel32.GetLastError());

            byte currentConfigurationValue =
                    (byte) USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_CurrentConfigurationValue.get(connInfo);

            var deviceDesc = connInfo.asSlice(
                    USBHelper.USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$Offset,
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

            return new WindowsUSBDeviceInfo(
                    devicePath, vendorId, productId,
                    manufacturer, product, serialNumber,
                    classCode, subClassCode, protocolCode,
                    currentConfigurationValue);
        }
    }

    private String getStringDescriptor(Addressable hubHandle, int usbPortNumber, int index) {
        if (index == 0)
            return null;

        try (var session = MemorySession.openConfined()) {
            final int dataLen = 32;
            var descriptorRequest = session.allocate(USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset + dataLen);
            USBHelper.USB_DESCRIPTOR_REQUEST_ConnectionIndex.set(descriptorRequest, usbPortNumber);
            var setupPacket = descriptorRequest.asSlice(
                    USBHelper.USB_DESCRIPTOR_REQUEST_SetupPacket$Offset,
                    USBStructs.SetupPacket$Struct.byteSize());
            USBStructs.SetupPacket_bmRequest.set(setupPacket, (byte) 0x80);
            USBStructs.SetupPacket_bRequest.set(setupPacket, USBHelper.USB_REQUEST_GET_DESCRIPTOR);
            USBStructs.SetupPacket_wValue.set(setupPacket, (short) ((USBHelper.USB_STRING_DESCRIPTOR_TYPE << 8) | index));
            USBStructs.SetupPacket_wIndex.set(setupPacket, (short) 0x0409);
            USBStructs.SetupPacket_wLength.set(setupPacket, (short) dataLen);

            var sizeHolder = session.allocate(JAVA_INT);
            if (Kernel32.DeviceIoControl(hubHandle, USBIoctl.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION(),
                    descriptorRequest, (int) descriptorRequest.byteSize(),
                    descriptorRequest, (int) descriptorRequest.byteSize(),
                    sizeHolder, NULL) == 0)
                throw new USBException(String.format("Cannot retrieve string descriptor %d", index), Kernel32.GetLastError());

            var stringDesc = descriptorRequest.asSlice(USBHelper.USB_DESCRIPTOR_REQUEST_Data$Offset, dataLen);
            int stringLen = 255 & (byte) USBHelper.USB_STRING_DESCRIPTOR_bLength.get(stringDesc);
            var chars = stringDesc.asSlice(USBHelper.USB_STRING_DESCRIPTOR_bString$Offset, stringLen - 2)
                    .toArray(JAVA_CHAR);
            return new String(chars);
        }
    }

    @Override
    public void setOnDeviceConnected(Consumer<USBDeviceInfo> handler) {
    }

    @Override
    public void setOnDeviceDisconnected(Consumer<USBDeviceInfo> handler) {
    }
}
