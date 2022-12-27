//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.windows.gen.advapi32.Advapi32;
import net.codecrete.usb.windows.gen.kernel32.GUID;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DATA;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DETAIL_DATA_W;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.windows.WindowsUSBException.throwException;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

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

    public static int getDeviceIntProperty(MemorySegment devInfo, MemorySegment devInfoData, MemorySegment propertyKey) {
        try (var arena = Arena.openConfined()) {
            var propertyTypeHolder = arena.allocate(JAVA_INT);
            var propertyValueHolder = arena.allocate(JAVA_INT);
            if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                    propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
                throwLastError("Internal error (SetupDiGetDevicePropertyW)");

            if (propertyTypeHolder.get(JAVA_INT, 0) != SetupAPI.DEVPROP_TYPE_UINT32())
                throwException("Internal error (expected property type UINT32)");

            return propertyValueHolder.get(JAVA_INT, 0);
        }
    }

    public static String getDeviceStringProperty(MemorySegment devInfo, MemorySegment devInfoData,
                                                 MemorySegment propertyKey) {
        try (var arena = Arena.openConfined()) {
            var propertyValue = getProperty(devInfo, devInfoData, propertyKey,
                    SetupAPI.DEVPROP_TYPE_STRING(), arena);
            return Win.createStringFromSegment(propertyValue);
        }
    }

    public static List<String> getDeviceStringListProperty(MemorySegment devInfo, MemorySegment devInfoData,
                                                           MemorySegment propertyKey) {
        try (var arena = Arena.openConfined()) {
            var propertyValue = getProperty(devInfo, devInfoData, propertyKey,
                    SetupAPI.DEVPROP_TYPE_STRING() | SetupAPI.DEVPROP_TYPEMOD_LIST(), arena);

            return Win.createStringListFromSegment(propertyValue);
        }
    }

    private static MemorySegment getProperty(MemorySegment devInfo, MemorySegment devInfoData, MemorySegment propertyKey,
                                             int propertyType, Arena arena) {
        var propertyTypeHolder = arena.allocate(JAVA_INT);
        var requiredSizeHolder = arena.allocate(JAVA_INT);
        if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder, NULL, 0,
                requiredSizeHolder, 0) == 0) {
            // TODO: Reactivate when proper GetLastError() handling is available
            //                int err = Kernel32.GetLastError();
            //                if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
            //                    throwLastError("Internal error (SetupDiGetDevicePropertyW)");
        }

        if (propertyTypeHolder.get(JAVA_INT, 0) != propertyType)
            throwException("Internal error (unexpected property type)");

        int stringLen = requiredSizeHolder.get(JAVA_INT, 0) / 2 - 1;

        var propertyValueHolder = arena.allocateArray(JAVA_CHAR, stringLen + 1);
        if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
            throwLastError("Internal error (SetupDiGetDevicePropertyW)");

        return propertyValueHolder;
    }

    public static List<String> findDeviceInterfaceGUIDs(MemorySegment devInfoSetHandle, MemorySegment devInfo, Arena arena) {

        try (var cleanup = new ScopeCleanup()) {
            // open device registry key
            var regKey = SetupAPI.SetupDiOpenDevRegKey(devInfoSetHandle, devInfo, SetupAPI.DICS_FLAG_GLOBAL(),
                    0, SetupAPI.DIREG_DEV(), Advapi32.KEY_READ());
            if (Win.IsInvalidHandle(regKey))
                throwLastError("Cannot open device registry key");
            cleanup.add(() -> Advapi32.RegCloseKey(regKey));

            // read registry value (without buffer, to query length)
            var keyNameSegment = Win.createSegmentFromString("DeviceInterfaceGUIDs", arena);
            var valueTypeHolder = arena.allocate(JAVA_INT);
            var valueSizeHolder = arena.allocate(JAVA_INT);
            var res = Advapi32.RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, NULL, valueSizeHolder);
            if (res == Kernel32.ERROR_FILE_NOT_FOUND())
                return List.of(); // no device interface GUIDs
            if (res != 0 && res != Kernel32.ERROR_MORE_DATA())
                throwException(res, "Internal error (RegQueryValueExW)");

            // read registry value (with buffer)
            var valueSize = valueSizeHolder.get(JAVA_INT, 0);
            var value = arena.allocate(valueSize);
            res = Advapi32.RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, value, valueSizeHolder);
            if (res != 0)
                throwException(res, "Internal error (RegQueryValueExW)");

            return Win.createStringListFromSegment(value);
        }
    }

    public static String getDevicePath(String instanceID, MemorySegment interfaceGuid) {
        try (var arena = Arena.openConfined(); var cleanup = new ScopeCleanup()) {
            // get device info set for instance
            var instanceIDSegment = Win.createSegmentFromString(instanceID, arena);
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(interfaceGuid, instanceIDSegment, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throwException("internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            cleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            // retrieve first element of enumeration
            var devIntfData = arena.allocate(SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            if (SetupAPI.SetupDiEnumDeviceInterfaces(devInfoSetHandle, NULL, interfaceGuid, 0, devIntfData) == 0)
                throwException("internal error (SetupDiEnumDeviceInterfaces)");

            // get device path
            // (SP_DEVICE_INTERFACE_DETAIL_DATA_W is of variable length and requires a bigger allocation so
            // the device path fits)
            final int devicePathOffset = 4;
            var intfDetailData = arena.allocate(4 + 260 * 2);
            SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$set(intfDetailData,
                    (int) SP_DEVICE_INTERFACE_DETAIL_DATA_W.sizeof());
            int intfDetailDataSize = (int) intfDetailData.byteSize();
            if (SetupAPI.SetupDiGetDeviceInterfaceDetailW(devInfoSetHandle, devIntfData, intfDetailData,
                    intfDetailDataSize, NULL, NULL) == 0)
                throwLastError("Internal error (SetupDiGetDeviceInterfaceDetailW)");

            return Win.createStringFromSegment(intfDetailData.asSlice(devicePathOffset));
        }
    }

    public static MemorySegment createDEVPROPKEY(int data1, short data2, short data3, byte data4_0, byte data4_1,
                                                 byte data4_2, byte data4_3, byte data4_4, byte data4_5, byte data4_6
            , byte data4_7, int pid) {
        var propKey = Win.GLOBAL_ALLOCATOR.allocate(GUID.sizeof() + JAVA_INT.byteSize());
        Win.setGUID(propKey, data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4, data4_5, data4_6,
                data4_7);
        propKey.set(JAVA_INT, 16, pid);
        return propKey;
    }

}
