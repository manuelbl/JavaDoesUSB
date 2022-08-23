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
import net.codecrete.usb.windows.gen.cfgmgr32.CfgMgr32;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;
import net.codecrete.usb.windows.gen.setupapi._SP_DEVINFO_DATA;
import net.codecrete.usb.windows.gen.stdlib.StdLib;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class WindowsUSBDeviceRegistry implements USBDeviceRegistry {

    public List<USBDeviceInfo> getAllDevices() {

        List<USBDeviceInfo> result = new ArrayList<>();

        try (var outerSession = MemorySession.openConfined()) {
            // get as set of all USB devices present
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(SetupApi.GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL, SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throw new USBException("internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            outerSession.addCloseAction(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            var devInfo = MemorySegment.allocateNative(_SP_DEVINFO_DATA.$LAYOUT(), outerSession);
            _SP_DEVINFO_DATA.cbSize$set(devInfo, (int) _SP_DEVINFO_DATA.$LAYOUT().byteSize());

            var devInfoData = MemorySegment.allocateNative(_SP_DEVINFO_DATA.$LAYOUT(), outerSession);
            _SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) _SP_DEVINFO_DATA.$LAYOUT().byteSize());

            for (int i = 0; true; i++) {
                if (SetupAPI.SetupDiEnumDeviceInfo(devInfoSetHandle, i, devInfo) == 0) {
                    int err = Kernel32.GetLastError();
                    if (err != Kernel32.ERROR_NO_MORE_ITEMS()) {
                        throw new USBException("Internal error (SetupDiEnumDeviceInfo) ");
                    }
                    break;
                }

                try (var session = MemorySession.openConfined()) {

                    // get the parent device (a USB hub)
                    var parentDevInstHolder = session.allocate(JAVA_INT);
                    int devInst = (int) _SP_DEVINFO_DATA.DevInst$get(devInfo);
                    int cmRet = CfgMgr32.CM_Get_Parent(parentDevInstHolder, devInst, 0);
                    if (cmRet != 0)
                        throw new USBException("Internal error (CM_Get_Parent)");
                    var parentDevInst = parentDevInstHolder.get(JAVA_INT, 0);

                    // get parent device path
                    var pathBuffer = session.allocateArray(JAVA_CHAR, 260);
                    cmRet = CfgMgr32.CM_Get_Device_IDW(parentDevInst, pathBuffer, 260, 0);
                    if (cmRet != 0)
                        throw new USBException("Internal error (CM_Get_Device_ID)");

                    // create hub path
                    var pathChars = pathBuffer.toArray(JAVA_CHAR);
                    var index = 0;
                    for (; pathChars[index] != 0; index++) {
                        if (pathChars[index] == '\\' || pathChars[index] == '$')
                            pathChars[index] = '#';
                    }
                    var hubPath = "\\\\?\\" + new String(pathChars, 0, index) + "#{f18a0e88-c30c-11d0-8815-00a0c906bed8}\0";

                    var hubPathSeg = session.allocateArray(JAVA_CHAR, hubPath.length());
                    hubPathSeg.copyFrom(MemorySegment.ofArray(hubPath.toCharArray()));

                    final var hubHandle = Kernel32.CreateFileW(hubPathSeg, Kernel32.GENERIC_WRITE(), Kernel32.FILE_SHARE_WRITE(),
                            NULL, Kernel32.OPEN_EXISTING(), 0, NULL);
                    if (Win.IsInvalidHandle(hubHandle))
                        throw new USBException("Cannot open USB hub");

                    session.addCloseAction(() -> Kernel32.CloseHandle(hubHandle));

                    // get the interface data
                    if (SetupAPI.SetupDiEnumDeviceInterfaces(devInfoSetHandle, devInfo,
                            SetupApi.GUID_DEVINTERFACE_USB_DEVICE, 0, devInfoData) == 0) {
                        int lastError = Kernel32.GetLastError();
                        if (lastError == Kernel32.ERROR_NO_MORE_ITEMS())
                            throw new USBException("Internal error (SetupDiEnumDeviceInterfaces)");
                        continue;
                    }

                    // get path of first interface
                    var intfDetailData = MemorySegment.allocateNative(SetupApi.SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct, session);
                    SetupApi.SP_DEVICE_INTERFACE_DETAIL_DATA_W_cbSize.set(intfDetailData, 8);
                    int intfDatailDataSize = (int) SetupApi.SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct.byteSize();
                    if (SetupAPI.SetupDiGetDeviceInterfaceDetailW(devInfoSetHandle, devInfoData,
                            intfDetailData, intfDatailDataSize, NULL, NULL) == 0)
                        throw new USBException("Internal error (SetupDiGetDeviceInterfaceDetailA - 2)", Kernel32.GetLastError());

                    long pathLen = StdLib.wcslen(intfDetailData.address().addOffset(SetupApi.SP_DEVICE_INTERFACE_DETAIL_DATA_W_DevicePath$Offset));
                    char[] devicePathChars = intfDetailData.asSlice(SetupApi.SP_DEVICE_INTERFACE_DETAIL_DATA_W_DevicePath$Offset, pathLen * 2).toArray(JAVA_CHAR);
                    String devicePath = new String(devicePathChars);

                    // get device's hub port number
                    var usbPortNumHolder = session.allocate(JAVA_INT);
                    var sizeHolder = session.allocate(JAVA_INT);
                    if (SetupAPI.SetupDiGetDeviceRegistryPropertyW(devInfoSetHandle, devInfo,
                            SetupAPI.SPDRP_ADDRESS(), NULL, usbPortNumHolder,
                            (int) usbPortNumHolder.byteSize(), sizeHolder) == 0)
                        throw new USBException("Internal error (cannot get device hub port)", Kernel32.GetLastError());
                    int usbPortNum = usbPortNumHolder.get(JAVA_INT, 0);

                    result.add(createDeviceInfo(devicePath, hubHandle, usbPortNum));
                }
            }

            return result;
        }
    }

    private USBDeviceInfo createDeviceInfo(String devicePath, MemoryAddress hubHandle, int usbPortNum) {

        try (var session = MemorySession.openConfined()) {

            // get device descriptor
            var connInfo = session.allocate(USBIOCtl.USB_NODE_CONNECTION_INFORMATION_EX$Struct);
            USBIOCtl.USB_NODE_CONNECTION_INFORMATION_EX_ConnectionIndex.set(connInfo, usbPortNum);
            var sizeHolder = session.allocate(JAVA_INT);
            if (Kernel32.DeviceIoControl(hubHandle, USBIOCtl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX,
                    connInfo, (int) connInfo.byteSize(),
                    connInfo, (int) connInfo.byteSize(),
                    sizeHolder, NULL) == 0)
                throw new USBException("Internal error (cannot get device descriptor)", Kernel32.GetLastError());

            byte currentConfigurationValue =
                    (byte) USBIOCtl.USB_NODE_CONNECTION_INFORMATION_EX_CurrentConfigurationValue.get(connInfo);

            var deviceDesc = connInfo.asSlice(
                    USBIOCtl.USB_NODE_CONNECTION_INFORMATION_EX_DeviceDescriptor$Offset,
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
            var descriptorRequest = session.allocate(USBIOCtl.USB_DESCRIPTOR_REQUEST_Data$Offset + dataLen);
            USBIOCtl.USB_DESCRIPTOR_REQUEST_ConnectionIndex.set(descriptorRequest, usbPortNumber);
            var setupPacket = descriptorRequest.asSlice(
                    USBIOCtl.USB_DESCRIPTOR_REQUEST_SetupPacket$Offset,
                    USBStructs.SetupPacket$Struct.byteSize());
            USBStructs.SetupPacket_bmRequest.set(setupPacket, (byte) 0x80);
            USBStructs.SetupPacket_bRequest.set(setupPacket, USBIOCtl.USB_REQUEST_GET_DESCRIPTOR);
            USBStructs.SetupPacket_wValue.set(setupPacket, (short) ((USBIOCtl.USB_STRING_DESCRIPTOR_TYPE << 8) | index));
            USBStructs.SetupPacket_wIndex.set(setupPacket, (short) 0x0409);
            USBStructs.SetupPacket_wLength.set(setupPacket, (short) dataLen);

            var sizeHolder = session.allocate(JAVA_INT);
            if (Kernel32.DeviceIoControl(hubHandle, USBIOCtl.IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION,
                    descriptorRequest, (int) descriptorRequest.byteSize(),
                    descriptorRequest, (int) descriptorRequest.byteSize(),
                    sizeHolder, NULL) == 0)
                throw new USBException(String.format("Cannot retrieve string descriptor %d", index), Kernel32.GetLastError());

            var stringDesc = descriptorRequest.asSlice(USBIOCtl.USB_DESCRIPTOR_REQUEST_Data$Offset, dataLen);
            int stringLen = 255 & (byte) USBIOCtl.USB_STRING_DESCRIPTOR_bLength.get(stringDesc);
            var chars = stringDesc.asSlice(USBIOCtl.USB_STRING_DESCRIPTOR_bString$Offset, stringLen - 2)
                    .toArray(JAVA_CHAR);
            return new String(chars);
        }
    }
}
