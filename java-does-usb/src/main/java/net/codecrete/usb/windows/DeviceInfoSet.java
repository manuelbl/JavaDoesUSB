package net.codecrete.usb.windows;

import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.windows.gen.advapi32.Advapi32;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.kernel32._GUID;
import net.codecrete.usb.windows.gen.ole32.Ole32;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;
import net.codecrete.usb.windows.gen.setupapi._SP_DEVICE_INTERFACE_DATA;
import net.codecrete.usb.windows.gen.setupapi._SP_DEVICE_INTERFACE_DETAIL_DATA_W;
import net.codecrete.usb.windows.gen.setupapi._SP_DEVINFO_DATA;
import net.codecrete.usb.windows.winsdk.SetupAPI2;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.windows.DeviceProperty.DEVPKEY_Device_Service;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUSBException.throwException;
import static net.codecrete.usb.windows.WindowsUSBException.throwLastError;

/**
 * Device information set (of Windows Setup API).
 *
 * <p>
 * An instance of this class represents a device information set ({@code HDEVINFO})
 * and a current element within the set.
 * </p>
 */
public class DeviceInfoSet implements AutoCloseable {

    @FunctionalInterface
    interface InfoSetCreator {
        MemorySegment create(Arena arena, MemorySegment errorState);
    }

    private final Arena arena;
    private final MemorySegment errorState;
    private final MemorySegment devInfoSet;
    private final MemorySegment devInfoData;
    private MemorySegment devIntfData;
    private int iterationIndex = -1;

    /**
     * Creates a new empty device info set.
     *
     * @return device info set
     */
    static DeviceInfoSet ofEmpty() {
        return new DeviceInfoSet((arena, errorState) -> SetupAPI2.SetupDiCreateDeviceInfoList(NULL, NULL, errorState));
    }

    /**
     * Creates a new device info set containing the present devices of the specified device class and
     * optionally device instance ID.
     *
     * <p>
     * After creation, there is no current element. {@link #next()} must be called first.
     * </p>
     *
     * @param interfaceGuid device interface class GUID
     * @param instanceId device instance ID
     * @return device info set
     */
    static DeviceInfoSet ofPresentDevices(MemorySegment interfaceGuid, String instanceId) {
        return new DeviceInfoSet((arena, errorState) -> {
            var instanceIdSegment = instanceId != null ? Win.createSegmentFromString(instanceId, arena) : NULL;
            return SetupAPI2.SetupDiGetClassDevsW(interfaceGuid, instanceIdSegment, NULL,
                    SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE(), errorState);
        });
    }

    private DeviceInfoSet(InfoSetCreator creator) {
        arena = Arena.ofConfined();
        try {
            errorState = allocateErrorState(arena);

            devInfoSet = creator.create(arena, errorState);
            if (Win.isInvalidHandle(devInfoSet))
                throwLastError(errorState, "internal error (creating device info set)");

            // allocate SP_DEVINFO_DATA (will receive device details)
            devInfoData = _SP_DEVINFO_DATA.allocate(arena);
            _SP_DEVINFO_DATA.cbSize$set(devInfoData, (int) _SP_DEVINFO_DATA.$LAYOUT().byteSize());

        } catch (Exception e) {
            arena.close();
            throw e;
        }
    }

    @Override
    public void close() {
        if (devIntfData != null)
            SetupAPI.SetupDiDeleteDeviceInterfaceData(devInfoSet, devIntfData);
        SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSet);
        arena.close();
    }

    /**
     * Iterates to the next element in this set.
     *
     * @return {@code true} if there is a current element, {@code false} if the iteration moved beyond the last element
     */
    boolean next() {
        iterationIndex += 1;
        if (SetupAPI2.SetupDiEnumDeviceInfo(devInfoSet, iterationIndex, devInfoData, errorState) == 0) {
            var err = Win.getLastError(errorState);
            if (err == Kernel32.ERROR_NO_MORE_ITEMS())
                return false;
            throwLastError(errorState, "internal error (SetupDiEnumDeviceInfo)");
        }

        return true;
    }

    /**
     * Adds the device with the specified instance ID to this device info set.
     *
     * <p>
     * The added device becomes the current element.
     * </p>
     *
     * @param instanceId instance ID
     */
    void addInstance(String instanceId) {
        var instanceIdSegment = Win.createSegmentFromString(instanceId, arena);
        if (SetupAPI2.SetupDiOpenDeviceInfoW(devInfoSet, instanceIdSegment, NULL, 0, devInfoData, errorState) == 0)
            throwLastError(errorState, "internal error (SetupDiOpenDeviceInfoW)");
    }

    /**
     * Adds the device with the specified path to this device info set.
     *
     * <p>
     * The added device becomes the current element.
     * </p>
     *
     * @param devicePath device path
     */
    void addDevice(String devicePath) {
        if (devIntfData != null)
            throw new AssertionError("calling addDevice() multiple times is not implemented");

        // load device information into dev info set
        var intfData = _SP_DEVICE_INTERFACE_DATA.allocate(arena);
        _SP_DEVICE_INTERFACE_DATA.cbSize$set(intfData, (int) intfData.byteSize());
        var devicePathSegment = Win.createSegmentFromString(devicePath, arena);
        if (SetupAPI2.SetupDiOpenDeviceInterfaceW(devInfoSet, devicePathSegment, 0, intfData, errorState) == 0)
            throwLastError(errorState, "internal error (SetupDiOpenDeviceInterfaceW)");

        devIntfData = intfData; // for later cleanup

        if (SetupAPI2.SetupDiGetDeviceInterfaceDetailW(devInfoSet, intfData, NULL, 0, NULL,
                devInfoData, errorState) == 0) {
            var err = Win.getLastError(errorState);
            if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
                throwException(err, "internal error (SetupDiGetDeviceInterfaceDetailW)");
        }
    }


    /**
     * Checks if the current element is a composite USB device
     *
     * @return {@code true} if it is a composite device
     */
    boolean isCompositeDevice() {
        var deviceService = getStringProperty(DEVPKEY_Device_Service);

        // usbccgp is the USB Generic Parent Driver used for composite devices
        return "usbccgp".equalsIgnoreCase(deviceService);
    }

    /**
     * Gets the device path for the device with the given instance ID.
     * <p>
     * The device path is looked up by checking the GUIDs associated with the current element.
     * </p>
     *
     * @param instanceId  device instance ID
     * @return the device path, {@code null} if not found
     */
    String getDevicePathByGUID(String instanceId) {
        var guids = findDeviceInterfaceGUIDs(arena);

        for (var guid : guids) {
            // check for class GUID
            var guidSegment = Win.createSegmentFromString(guid, arena);
            var clsid = _GUID.allocate(arena);
            if (Ole32.CLSIDFromString(guidSegment, clsid) != 0)
                continue;

            try {
                return getDevicePath(instanceId, clsid);
            } catch (Exception e) {
                // ignore and try next one
            }
        }

        return null;
    }

    /**
     * Gets a list of {@code DeviceInterfaceGUIDs} from the current element's device configuration information
     * in the registry.
     *
     * @param arena arena for allocating memory
     * @return list of GUIDs
     */
    private List<String> findDeviceInterfaceGUIDs(Arena arena) {

        try (var cleanup = new ScopeCleanup()) {
            // open device registry key
            var regKey = SetupAPI2.SetupDiOpenDevRegKey(devInfoSet, devInfoData, SetupAPI.DICS_FLAG_GLOBAL(), 0,
                    SetupAPI.DIREG_DEV(), Advapi32.KEY_READ(), errorState);
            if (Win.isInvalidHandle(regKey))
                throwLastError(errorState, "internal error (SetupDiOpenDevRegKey)");
            cleanup.add(() -> Advapi32.RegCloseKey(regKey));

            // read registry value (without buffer, to query length)
            var keyNameSegment = Win.createSegmentFromString("DeviceInterfaceGUIDs", arena);
            var valueTypeHolder = arena.allocate(JAVA_INT);
            var valueSizeHolder = arena.allocate(JAVA_INT);
            var res = Advapi32.RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, NULL, valueSizeHolder);
            if (res == Kernel32.ERROR_FILE_NOT_FOUND())
                return List.of(); // no device interface GUIDs
            if (res != 0 && res != Kernel32.ERROR_MORE_DATA())
                throwException(res, "internal error (RegQueryValueExW)");

            // read registry value (with buffer)
            var valueSize = valueSizeHolder.get(JAVA_INT, 0);
            var value = arena.allocate(valueSize);
            res = Advapi32.RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, value, valueSizeHolder);
            if (res != 0)
                throwException(res, "internal error (RegQueryValueExW)");

            return Win.createStringListFromSegment(value);
        }
    }

    /**
     * Gets the integer device property of the current element.
     *
     * @param propertyKey property key (of type {@code DEVPKEY})
     * @return property value
     */
    @SuppressWarnings("SameParameterValue")
    int getIntProperty(MemorySegment propertyKey) {
        var propertyTypeHolder = arena.allocate(JAVA_INT);
        var propertyValueHolder = arena.allocate(JAVA_INT);
        if (SetupAPI2.SetupDiGetDevicePropertyW(devInfoSet, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0, errorState) == 0)
            throwLastError(errorState, "internal error (SetupDiGetDevicePropertyW - A)");

        if (propertyTypeHolder.get(JAVA_INT, 0) != SetupAPI.DEVPROP_TYPE_UINT32())
            throwException("internal error (expected property type UINT32)");

        return propertyValueHolder.get(JAVA_INT, 0);
    }

    /**
     * Gets the string device property of the current element.
     *
     * @param propertyKey property key (of type {@code DEVPKEY})
     * @return property value
     */
    String getStringProperty(MemorySegment propertyKey) {
        var propertyValue = getVariableLengthProperty(propertyKey, SetupAPI.DEVPROP_TYPE_STRING(), arena);
        if (propertyValue == null)
            return null;
        return Win.createStringFromSegment(propertyValue);
    }

    /**
     * Gets the string list device property of the current element.
     *
     * @param propertyKey property key (of type {@code DEVPKEY})
     * @return property value
     */
    @SuppressWarnings("java:S1168")
    List<String> getStringListProperty(MemorySegment propertyKey) {
        var propertyValue = getVariableLengthProperty(propertyKey,
                SetupAPI.DEVPROP_TYPE_STRING() | SetupAPI.DEVPROP_TYPEMOD_LIST(), arena);
        if (propertyValue == null)
            return null;

        return Win.createStringListFromSegment(propertyValue);
    }

    private MemorySegment getVariableLengthProperty(MemorySegment propertyKey, int propertyType, Arena arena) {

        // query length (thus no buffer)
        var propertyTypeHolder = arena.allocate(JAVA_INT);
        var requiredSizeHolder = arena.allocate(JAVA_INT);
        if (SetupAPI2.SetupDiGetDevicePropertyW(devInfoSet, devInfoData, propertyKey, propertyTypeHolder, NULL, 0,
                requiredSizeHolder, 0, errorState) == 0) {
            var err = Win.getLastError(errorState);
            if (err == Kernel32.ERROR_NOT_FOUND())
                return null;
            if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
                throwException(err, "internal error (SetupDiGetDevicePropertyW - B)");
        }

        if (propertyTypeHolder.get(JAVA_INT, 0) != propertyType)
            throwException("internal error (unexpected property type)");

        var stringLen = requiredSizeHolder.get(JAVA_INT, 0) / 2 - 1;

        // allocate buffer
        var propertyValueHolder = arena.allocateArray(JAVA_CHAR, stringLen + 1L);

        // get property value
        if (SetupAPI2.SetupDiGetDevicePropertyW(devInfoSet, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0, errorState) == 0)
            throwLastError(errorState, "internal error (SetupDiGetDevicePropertyW - C)");

        return propertyValueHolder;
    }

    /**
     * Gets the device path for the device with the given device instance ID and device interface class.
     *
     * @param instanceId    device instance ID
     * @param interfaceGuid device interface class GUID
     * @return the device path
     */
    static String getDevicePath(String instanceId, MemorySegment interfaceGuid) {
        try (var arena = Arena.ofConfined();
             var deviceInfoSet = DeviceInfoSet.ofPresentDevices(interfaceGuid, instanceId)) {

            // retrieve first element of enumeration
            var errorState = allocateErrorState(arena);
            var devIntfData = _SP_DEVICE_INTERFACE_DATA.allocate(arena);
            _SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            if (SetupAPI2.SetupDiEnumDeviceInterfaces(deviceInfoSet.devInfoSet, NULL, interfaceGuid, 0, devIntfData,
                    errorState) == 0)
                throwLastError(errorState, "internal error (SetupDiEnumDeviceInterfaces)");

            // get device path
            // (SP_DEVICE_INTERFACE_DETAIL_DATA_W is of variable length and requires a bigger allocation so
            // the device path fits)
            final var devicePathOffset = 4;
            var intfDetailData = arena.allocate(4L + 260 * 2);
            _SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$set(intfDetailData,
                    (int) _SP_DEVICE_INTERFACE_DETAIL_DATA_W.sizeof());
            if (SetupAPI2.SetupDiGetDeviceInterfaceDetailW(deviceInfoSet.devInfoSet, devIntfData, intfDetailData,
                    (int) intfDetailData.byteSize(), NULL, NULL, errorState) == 0)
                throwLastError(errorState, "Internal error (SetupDiGetDeviceInterfaceDetailW)");

            return Win.createStringFromSegment(intfDetailData.asSlice(devicePathOffset));
        }
    }
}
