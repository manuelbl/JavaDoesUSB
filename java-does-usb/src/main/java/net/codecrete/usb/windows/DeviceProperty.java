//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBException;
import net.codecrete.usb.windows.gen.advapi32.Advapi32;
import net.codecrete.usb.windows.gen.kernel32.GUID;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DATA;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DETAIL_DATA_W;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Device property GUIDs and functions
 */
public class DeviceProperty {

    public static final MemorySegment DEVPKEY_Device_Address = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 30);

    public static final MemorySegment DEVPKEY_Device_InstanceId = createDEVPROPKEY(0x78c34fc8, (short) 0x104a,
            (short) 0x4aca, (byte) 0x9e, (byte) 0xa4, (byte) 0x52, (byte) 0x4d, (byte) 0x52, (byte) 0x99, (byte) 0x6e
            , (byte) 0x57, 256);

    public static final MemorySegment DEVPKEY_Device_Parent = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa,
            (short) 0x4706, (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64, (byte) 0x80, (byte) 0x08, (byte) 0xa5
            , (byte) 0xa7, 8);

    public static final MemorySegment DEVPKEY_Device_Service = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50,
            (byte) 0xe0, 6);

    public static final MemorySegment DEVPKEY_Device_Children = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa,
            (short) 0x4706, (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64, (byte) 0x80, (byte) 0x08, (byte) 0xa5,
            (byte) 0xa7, 9);

    public static final MemorySegment DEVPKEY_Device_HardwareIds = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50,
            (byte) 0xe0, 3);

    public static int getDeviceIntProperty(Addressable devInfo, Addressable devInfoData, Addressable propertyKey) {
        try (var session = MemorySession.openConfined()) {
            var propertyTypeHolder = session.allocate(JAVA_INT);
            var propertyValueHolder = session.allocate(JAVA_INT);
            if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                    propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
                throw new WindowsUSBException("Internal error (SetupDiGetDevicePropertyW)", Kernel32.GetLastError());

            if (propertyTypeHolder.get(JAVA_INT, 0) != SetupAPI.DEVPROP_TYPE_UINT32())
                throw new USBException("Internal error (expected property type UINT32)");

            return propertyValueHolder.get(JAVA_INT, 0);
        }
    }

    public static String getDeviceStringProperty(Addressable devInfo, Addressable devInfoData,
                                                 Addressable propertyKey) {
        try (var session = MemorySession.openConfined()) {
            var propertyValue = getProperty(devInfo, devInfoData, propertyKey,
                    SetupAPI.DEVPROP_TYPE_STRING(), session);
            return Win.createStringFromSegment(propertyValue);
        }
    }

    public static List<String> getDeviceStringListProperty(Addressable devInfo, Addressable devInfoData,
                                                           Addressable propertyKey) {
        try (var session = MemorySession.openConfined()) {
            var propertyValue = getProperty(devInfo, devInfoData, propertyKey,
                    SetupAPI.DEVPROP_TYPE_STRING() | SetupAPI.DEVPROP_TYPEMOD_LIST(), session);

            return Win.createStringListFromSegment(propertyValue);
        }
    }

    private static MemorySegment getProperty(Addressable devInfo, Addressable devInfoData, Addressable propertyKey,
                                             int propertyType, MemorySession session) {
        var propertyTypeHolder = session.allocate(JAVA_INT);
        var requiredSizeHolder = session.allocate(JAVA_INT);
        if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder, NULL, 0,
                requiredSizeHolder, 0) == 0) {
            // TODO: Reactivate when proper GetLastError() handling is available
            //                int err = Kernel32.GetLastError();
            //                if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
            //                    throw new WindowsUSBException("Internal error (SetupDiGetDevicePropertyW)", Kernel32
            //                    .GetLastError());
        }

        if (propertyTypeHolder.get(JAVA_INT, 0) != propertyType)
            throw new USBException("Internal error (unexpected property type)");

        int stringLen = requiredSizeHolder.get(JAVA_INT, 0) / 2 - 1;

        var propertyValueHolder = session.allocateArray(JAVA_CHAR, stringLen + 1);
        if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
            throw new WindowsUSBException("Internal error (SetupDiGetDevicePropertyW)", Kernel32.GetLastError());

        return propertyValueHolder;
    }

    public static List<String> findDeviceInterfaceGUIDs(MemoryAddress devInfoSetHandle, MemorySegment devInfo, MemorySession session) {

        // open device registry key
        var regKey = SetupAPI.SetupDiOpenDevRegKey(devInfoSetHandle, devInfo, SetupAPI.DICS_FLAG_GLOBAL(),
                0, SetupAPI.DIREG_DEV(), Advapi32.KEY_READ());
        if (Win.IsInvalidHandle(regKey))
            throw new WindowsUSBException("Cannot open device registry key", Kernel32.GetLastError());
        session.addCloseAction(() -> Advapi32.RegCloseKey(regKey));

        // read registry value (without buffer, to query length)
        var keyNameSegment = Win.createSegmentFromString("DeviceInterfaceGUIDs", session);
        var valueTypeHolder = session.allocate(JAVA_INT);
        var valueSizeHolder = session.allocate(JAVA_INT);
        var res = Advapi32.RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, NULL, valueSizeHolder);
        if (res == Kernel32.ERROR_FILE_NOT_FOUND())
            return List.of(); // no device interface GUIDs
        if (res != 0 && res != Kernel32.ERROR_MORE_DATA())
            throw new WindowsUSBException("Internal error (RegQueryValueExW)", res);

        // read registry value (with buffer)
        var valueSize = valueSizeHolder.get(JAVA_INT, 0);
        var value = session.allocate(valueSize);
        res = Advapi32.RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, value, valueSizeHolder);
        if (res != 0)
            throw new WindowsUSBException("Internal error (RegQueryValueExW)", res);

        return Win.createStringListFromSegment(value);
    }

    public static String getDevicePath(String instanceID, Addressable interfaceGuid) {
        try (var session = MemorySession.openConfined()) {
            // get device info set for instance
            var instanceIDSegment = Win.createSegmentFromString(instanceID, session);
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(interfaceGuid, instanceIDSegment, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throw new USBException("internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            session.addCloseAction(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            // retrieve first element of enumeration
            var devIntfData = session.allocate(SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            if (SetupAPI.SetupDiEnumDeviceInterfaces(devInfoSetHandle, NULL, interfaceGuid, 0, devIntfData) == 0)
                throw new USBException("internal error (SetupDiEnumDeviceInterfaces)");

            // get device path
            // (SP_DEVICE_INTERFACE_DETAIL_DATA_W is of variable length and requires a bigger allocation so
            // the device path fits)
            final int devicePathOffset = 4;
            var intfDetailData = session.allocate(4 + 260 * 2);
            SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$set(intfDetailData,
                    (int) SP_DEVICE_INTERFACE_DETAIL_DATA_W.sizeof());
            int intfDetailDataSize = (int) intfDetailData.byteSize();
            if (SetupAPI.SetupDiGetDeviceInterfaceDetailW(devInfoSetHandle, devIntfData, intfDetailData,
                    intfDetailDataSize, NULL, NULL) == 0)
                throw new WindowsUSBException("Internal error (SetupDiGetDeviceInterfaceDetailW)", Kernel32.GetLastError());

            return Win.createStringFromSegment(intfDetailData.asSlice(devicePathOffset));
        }
    }

    public static MemorySegment createDEVPROPKEY(int data1, short data2, short data3, byte data4_0, byte data4_1,
                                                 byte data4_2, byte data4_3, byte data4_4, byte data4_5, byte data4_6
            , byte data4_7, int pid) {
        var propKey = MemorySession.global().allocate(GUID.sizeof() + JAVA_INT.byteSize());
        Win.setGUID(propKey, data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4, data4_5, data4_6,
                data4_7);
        propKey.set(JAVA_INT, 16, pid);
        return propKey;
    }

}
