//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.windows.gen.advapi32.Advapi32;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.kernel32._GUID;
import net.codecrete.usb.windows.gen.ole32.Ole32;
import net.codecrete.usb.windows.gen.setupapi.*;
import net.codecrete.usb.windows.winsdk.SetupAPI2;

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
class DeviceProperty {

    private DeviceProperty() {
    }

    static final MemorySegment DEVPKEY_Device_Address = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 30);

    static final MemorySegment DEVPKEY_Device_InstanceId = createDEVPROPKEY(0x78c34fc8, (short) 0x104a,
            (short) 0x4aca, (byte) 0x9e, (byte) 0xa4, (byte) 0x52, (byte) 0x4d, (byte) 0x52, (byte) 0x99, (byte) 0x6e
            , (byte) 0x57, 256);

    static final MemorySegment DEVPKEY_Device_Parent = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa,
            (short) 0x4706, (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64, (byte) 0x80, (byte) 0x08, (byte) 0xa5
            , (byte) 0xa7, 8);

    static final MemorySegment DEVPKEY_Device_Service = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 6);

    static final MemorySegment DEVPKEY_Device_Children = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa,
            (short) 0x4706, (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64, (byte) 0x80, (byte) 0x08, (byte) 0xa5
            , (byte) 0xa7, 9);

    static final MemorySegment DEVPKEY_Device_HardwareIds = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 3);

    /**
     * Gets the device property with integer type.
     *
     * @param devInfoSet  device information set containing the device ({@code HDEVINFO})
     * @param devInfoData {@code SP_DEVINFO_DATA} structure specifying the element (device) in the information set
     * @param propertyKey property key (of type {@code DEVPKEY})
     * @return property value
     */
    @SuppressWarnings("SameParameterValue")
    static int getDeviceIntProperty(MemorySegment devInfoSet, MemorySegment devInfoData,
                                    MemorySegment propertyKey) {
        try (var arena = Arena.ofConfined()) {
            var propertyTypeHolder = arena.allocate(JAVA_INT);
            var propertyValueHolder = arena.allocate(JAVA_INT);
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
            if (SetupAPI2.SetupDiGetDevicePropertyW(devInfoSet, devInfoData, propertyKey, propertyTypeHolder,
                    propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0, lastErrorState) == 0)
                throwLastError(lastErrorState, "Internal error (SetupDiGetDevicePropertyW - A)");

            if (propertyTypeHolder.get(JAVA_INT, 0) != SetupAPI.DEVPROP_TYPE_UINT32())
                throwException("Internal error (expected property type UINT32)");

            return propertyValueHolder.get(JAVA_INT, 0);
        }
    }

    /**
     * Gets the device property with string type.
     *
     * @param devInfoSet  device information set containing the device ({@code HDEVINFO})
     * @param devInfoData {@code SP_DEVINFO_DATA} structure specifying the element (device) in the information set
     * @param propertyKey property key (of type {@code DEVPKEY})
     * @return property value
     */
    static String getDeviceStringProperty(MemorySegment devInfoSet, MemorySegment devInfoData,
                                                 MemorySegment propertyKey) {
        try (var arena = Arena.ofConfined()) {
            var propertyValue = getVariableLengthProperty(devInfoSet, devInfoData, propertyKey,
                    SetupAPI.DEVPROP_TYPE_STRING(), arena);
            if (propertyValue == null)
                return null;
            return Win.createStringFromSegment(propertyValue);
        }
    }

    /**
     * Gets the device property with string list type.
     *
     * @param devInfoSet  device information set containing the device ({@code HDEVINFO})
     * @param devInfoData {@code SP_DEVINFO_DATA} structure specifying the element (device) in the information set
     * @param propertyKey property key (of type {@code DEVPKEY})
     * @return property value
     */
    @SuppressWarnings("java:S1168")
    static List<String> getDeviceStringListProperty(MemorySegment devInfoSet, MemorySegment devInfoData,
                                                           MemorySegment propertyKey) {
        try (var arena = Arena.ofConfined()) {
            var propertyValue = getVariableLengthProperty(devInfoSet, devInfoData, propertyKey,
                    SetupAPI.DEVPROP_TYPE_STRING() | SetupAPI.DEVPROP_TYPEMOD_LIST(), arena);
            if (propertyValue == null)
                return null;

            return Win.createStringListFromSegment(propertyValue);
        }
    }

    private static MemorySegment getVariableLengthProperty(MemorySegment devInfoSet, MemorySegment devInfoData,
                                                           MemorySegment propertyKey, int propertyType, Arena arena) {

        // query length (thus no buffer)
        var propertyTypeHolder = arena.allocate(JAVA_INT);
        var requiredSizeHolder = arena.allocate(JAVA_INT);
        var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);
        if (SetupAPI2.SetupDiGetDevicePropertyW(devInfoSet, devInfoData, propertyKey, propertyTypeHolder, NULL, 0,
                requiredSizeHolder, 0, lastErrorState) == 0) {
            var err = Win.getLastError(lastErrorState);
            if (err == Kernel32.ERROR_NOT_FOUND())
                return null;
            if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
                throwException(err, "Internal error (SetupDiGetDevicePropertyW - B)");
        }

        if (propertyTypeHolder.get(JAVA_INT, 0) != propertyType)
            throwException("Internal error (unexpected property type)");

        var stringLen = requiredSizeHolder.get(JAVA_INT, 0) / 2 - 1;

        // allocate buffer
        var propertyValueHolder = arena.allocateArray(JAVA_CHAR, stringLen + 1L);

        // get property value
        if (SetupAPI2.SetupDiGetDevicePropertyW(devInfoSet, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0, lastErrorState) == 0)
            throwLastError(lastErrorState, "Internal error (SetupDiGetDevicePropertyW - C)");

        return propertyValueHolder;
    }

    /**
     * Gets a list of {@code DeviceInterfaceGUIDs} from device-specific configuration information in the registry.
     *
     * @param devInfoSet  device information set containing the device ({@code HDEVINFO})
     * @param devInfoData {@code SP_DEVINFO_DATA} structure specifying the element (device) in the information set
     * @param arena       arena for allocating memory
     * @return list of GUIDs
     */
    static List<String> findDeviceInterfaceGUIDs(MemorySegment devInfoSet, MemorySegment devInfoData,
                                                        Arena arena) {

        try (var cleanup = new ScopeCleanup()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            // open device registry key
            var regKey = SetupAPI2.SetupDiOpenDevRegKey(devInfoSet, devInfoData, SetupAPI.DICS_FLAG_GLOBAL(), 0,
                    SetupAPI.DIREG_DEV(), Advapi32.KEY_READ(), lastErrorState);
            if (Win.isInvalidHandle(regKey))
                throwLastError(lastErrorState, "Cannot open device registry key");
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

    /**
     * Gets the device path for the device with the given device instance ID and device interface class.
     *
     * @param instanceId    device instance ID
     * @param interfaceGuid device interface class GUID
     * @return the device path
     */
    static String getDevicePath(String instanceId, MemorySegment interfaceGuid) {
        try (var arena = Arena.ofConfined(); var cleanup = new ScopeCleanup()) {
            var lastErrorState = arena.allocate(Win.LAST_ERROR_STATE_LAYOUT);

            // get device info set for instance
            var instanceIDSegment = Win.createSegmentFromString(instanceId, arena);
            final var devInfoSet = SetupAPI2.SetupDiGetClassDevsW(interfaceGuid, instanceIDSegment, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE(), lastErrorState);
            if (Win.isInvalidHandle(devInfoSet))
                throwLastError(lastErrorState, "internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            cleanup.add(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet));

            // retrieve first element of enumeration
            var devIntfData = arena.allocate(_SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            _SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            if (SetupAPI2.SetupDiEnumDeviceInterfaces(devInfoSet, NULL, interfaceGuid, 0, devIntfData,
                    lastErrorState) == 0)
                throwLastError(lastErrorState, "internal error (SetupDiEnumDeviceInterfaces)");

            // get device path
            // (SP_DEVICE_INTERFACE_DETAIL_DATA_W is of variable length and requires a bigger allocation so
            // the device path fits)
            final var devicePathOffset = 4;
            var intfDetailData = arena.allocate(4L + 260 * 2);
            _SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$set(intfDetailData,
                    (int) _SP_DEVICE_INTERFACE_DETAIL_DATA_W.sizeof());
            var intfDetailDataSize = (int) intfDetailData.byteSize();
            if (SetupAPI2.SetupDiGetDeviceInterfaceDetailW(devInfoSet, devIntfData, intfDetailData,
                    intfDetailDataSize, NULL, NULL, lastErrorState) == 0)
                throwLastError(lastErrorState, "Internal error (SetupDiGetDeviceInterfaceDetailW)");

            return Win.createStringFromSegment(intfDetailData.asSlice(devicePathOffset));
        }
    }

    /**
     * Gets the device path for the device from the given device info set and the given instance ID.
     * <p>
     * The device path is looked up by checking the associated GUIDs.
     * </p>
     *
     * @param devInfoSet  device information set containing the device ({@code HDEVINFO})
     * @param devInfoData {@code SP_DEVINFO_DATA} structure specifying the element (device) in the information set
     * @param instanceId  device instance ID
     * @param arena       arena for allocating memory
     * @return the device path, {code null} if not found
     */
    static String getDevicePathByGUID(MemorySegment devInfoSet, MemorySegment devInfoData, String instanceId, Arena arena) {
        var guids = DeviceProperty.findDeviceInterfaceGUIDs(devInfoSet, devInfoData, arena);

        for (var guid : guids) {
            // check for class GUID
            var guidSegment = Win.createSegmentFromString(guid, arena);
            var clsid = arena.allocate(_GUID.$LAYOUT());
            if (Ole32.CLSIDFromString(guidSegment, clsid) != 0)
                continue;

            try {
                return DeviceProperty.getDevicePath(instanceId, clsid);
            } catch (Exception e) {
                // ignore and try next one
            }
        }

        return null;
    }

    /**
     * Checks if the device is a composite USB device
     *
     * @param devInfoSet  device information set containing the device ({@code HDEVINFO})
     * @param devInfoData {@code SP_DEVINFO_DATA} structure specifying the element (device) in the information set
     * @return {@code true} if it is a composite device
     */
    static boolean isCompositeDevice(MemorySegment devInfoSet, MemorySegment devInfoData) {
        var deviceService = getDeviceStringProperty(devInfoSet, devInfoData, DEVPKEY_Device_Service);

        // usbccgp is the USB Generic Parent Driver used for composite devices
        return "usbccgp".equalsIgnoreCase(deviceService);
    }

    @SuppressWarnings({"java:S107", "java:S117"})
    private static MemorySegment createDEVPROPKEY(int data1, short data2, short data3, byte data4_0, byte data4_1,
                                                  byte data4_2, byte data4_3, byte data4_4, byte data4_5,
                                                  byte data4_6, byte data4_7, int pid) {
        @SuppressWarnings("resource")
        var propKey = Arena.global().allocate(_DEVPROPKEY.$LAYOUT());
        Win.setGUID(_DEVPROPKEY.fmtid$slice(propKey), data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4
                , data4_5, data4_6, data4_7);
        _DEVPROPKEY.pid$set(propKey, pid);
        return propKey;
    }
}
