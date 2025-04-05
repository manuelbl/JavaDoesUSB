package net.codecrete.usb.windows;

import net.codecrete.usb.common.ScopeCleanup;
import system.Guid;
import windows.win32.devices.deviceanddriverinstallation.SP_DEVICE_INTERFACE_DATA;
import windows.win32.devices.deviceanddriverinstallation.SP_DEVICE_INTERFACE_DETAIL_DATA_W;
import windows.win32.devices.deviceanddriverinstallation.SP_DEVINFO_DATA;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static net.codecrete.usb.windows.Win.allocateErrorState;
import static net.codecrete.usb.windows.WindowsUsbException.throwException;
import static net.codecrete.usb.windows.WindowsUsbException.throwLastError;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiDeleteDeviceInterfaceData;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiDestroyDeviceInfoList;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiEnumDeviceInfo;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiEnumDeviceInterfaces;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiGetClassDevsW;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiCreateDeviceInfoList;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiGetDeviceInterfaceDetailW;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiGetDevicePropertyW;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiOpenDevRegKey;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiOpenDeviceInfoW;
import static windows.win32.devices.deviceanddriverinstallation.Apis.SetupDiOpenDeviceInterfaceW;
import static windows.win32.devices.deviceanddriverinstallation.Constants.DIREG_DEV;
import static windows.win32.devices.deviceanddriverinstallation.SETUP_DI_GET_CLASS_DEVS_FLAGS.DIGCF_DEVICEINTERFACE;
import static windows.win32.devices.deviceanddriverinstallation.SETUP_DI_GET_CLASS_DEVS_FLAGS.DIGCF_PRESENT;
import static windows.win32.devices.deviceanddriverinstallation.SETUP_DI_PROPERTY_CHANGE_SCOPE.DICS_FLAG_GLOBAL;
import static windows.win32.devices.properties.Constants.DEVPKEY_Device_Service;
import static windows.win32.devices.properties.DEVPROPTYPE.DEVPROP_TYPEMOD_LIST;
import static windows.win32.devices.properties.DEVPROPTYPE.DEVPROP_TYPE_STRING;
import static windows.win32.devices.properties.DEVPROPTYPE.DEVPROP_TYPE_UINT32;
import static windows.win32.foundation.WIN32_ERROR.ERROR_FILE_NOT_FOUND;
import static windows.win32.foundation.WIN32_ERROR.ERROR_INSUFFICIENT_BUFFER;
import static windows.win32.foundation.WIN32_ERROR.ERROR_MORE_DATA;
import static windows.win32.foundation.WIN32_ERROR.ERROR_NOT_FOUND;
import static windows.win32.foundation.WIN32_ERROR.ERROR_NO_MORE_ITEMS;
import static windows.win32.system.com.Apis.CLSIDFromString;
import static windows.win32.system.registry.Apis.RegCloseKey;
import static windows.win32.system.registry.Apis.RegQueryValueExW;
import static windows.win32.system.registry.REG_SAM_FLAGS.KEY_READ;

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
        long create(Arena arena, MemorySegment errorState);
    }

    private final Arena arena;
    private final MemorySegment errorState;
    private final long devInfoSet;
    private final MemorySegment devInfoData;
    private MemorySegment devIntfData;
    private int iterationIndex = -1;

    /**
     * Creates a new device info set containing the present devices of the specified device class and
     * optionally device instance ID.
     *
     * <p>
     * After creation, there is no current element. {@link #next()} should be called to iterate the first
     * and all subsequent elements.
     * </p>
     *
     * @param interfaceGuid device interface class GUID
     * @param instanceId device instance ID
     * @return device info set
     */
    static DeviceInfoSet ofPresentDevices(MemorySegment interfaceGuid, String instanceId) {
        return new DeviceInfoSet((arena, errorState) -> {
            var instanceIdSegment = instanceId != null ? arena.allocateFrom(instanceId, UTF_16LE) : NULL;
            return SetupDiGetClassDevsW(errorState, interfaceGuid, instanceIdSegment, NULL,
                    DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
        });
    }

    /**
     * Creates a new device info set containing a single device with the specified instance ID.
     *
     * <p>
     * The device becomes the current element. The set cannot be iterated.
     * </p>
     *
     * @param instanceId instance ID
     */
    static DeviceInfoSet ofInstance(String instanceId) {
        var devInfoSet = ofEmpty();
        try {
            devInfoSet.addInstanceId(instanceId);
        } catch (Exception t) {
            devInfoSet.close();
            throw t;
        }
        return devInfoSet;
    }

    /**
     * Creates a new device info set containing a single device with the specified path.
     *
     * <p>
     * The device becomes the current element. The set cannot be iterated.
     * </p>
     *
     * @param devicePath device path
     */
    static DeviceInfoSet ofPath(String devicePath) {
        var devInfoSet = ofEmpty();
        try {
            devInfoSet.addDevicePath(devicePath);
        } catch (Exception t) {
            devInfoSet.close();
            throw t;
        }
        return devInfoSet;
    }

    /**
     * Creates a new empty device info set.
     *
     * @return device info set
     */
    private static DeviceInfoSet ofEmpty() {
        return new DeviceInfoSet((_, errorState) -> SetupDiCreateDeviceInfoList(errorState, NULL, NULL));
    }

    private DeviceInfoSet(InfoSetCreator creator) {
        arena = Arena.ofConfined();
        try {
            errorState = allocateErrorState(arena);

            devInfoSet = creator.create(arena, errorState);
            if (Win.isInvalidHandle(devInfoSet))
                throwLastError(errorState, "internal error (creating device info set)");

            // allocate SP_DEVINFO_DATA (will receive device details)
            devInfoData = SP_DEVINFO_DATA.allocate(arena);

        } catch (Exception e) {
            arena.close();
            throw e;
        }
    }

    @Override
    public void close() {
        if (devIntfData != null)
            SetupDiDeleteDeviceInterfaceData(errorState, devInfoSet, devIntfData);
        SetupDiDestroyDeviceInfoList(errorState, devInfoSet);
        arena.close();
    }

    private void addInstanceId(String instanceId) {
        var instanceIdSegment = arena.allocateFrom(instanceId, UTF_16LE);
        if (SetupDiOpenDeviceInfoW(errorState, devInfoSet, instanceIdSegment, NULL, 0, devInfoData) == 0)
            throwLastError(errorState, "internal error (SetupDiOpenDeviceInfoW)");
    }

    private void addDevicePath(String devicePath) {
        if (devIntfData != null)
            throw new AssertionError("calling addDevice() multiple times is not implemented");

        // load device information into dev info set
        var intfData = SP_DEVICE_INTERFACE_DATA.allocate(arena);
        var devicePathSegment = arena.allocateFrom(devicePath, UTF_16LE);
        if (SetupDiOpenDeviceInterfaceW(errorState, devInfoSet, devicePathSegment, 0, intfData) == 0)
            throwLastError(errorState, "internal error (SetupDiOpenDeviceInterfaceW)");

        devIntfData = intfData; // for later cleanup

        if (SetupDiGetDeviceInterfaceDetailW(errorState, devInfoSet, intfData, NULL, 0, NULL, devInfoData) == 0) {
            var err = Win.getLastError(errorState);
            if (err != ERROR_INSUFFICIENT_BUFFER)
                throwException(err, "internal error (SetupDiGetDeviceInterfaceDetailW)");
        }
    }

    /**
     * Iterates to the next element in this set.
     *
     * @return {@code true} if there is a current element, {@code false} if the iteration moved beyond the last element
     */
    boolean next() {
        iterationIndex += 1;
        if (SetupDiEnumDeviceInfo(errorState, devInfoSet, iterationIndex, devInfoData) == 0) {
            var err = Win.getLastError(errorState);
            if (err == ERROR_NO_MORE_ITEMS)
                return false;
            throwLastError(errorState, "internal error (SetupDiEnumDeviceInfo)");
        }

        return true;
    }

    /**
     * Checks if the current element is a composite USB device
     *
     * @return {@code true} if it is a composite device
     */
    boolean isCompositeDevice() {
        var deviceService = getStringProperty(DEVPKEY_Device_Service());

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
            var guidSegment = arena.allocateFrom(guid, UTF_16LE);
            var clsid = Guid.allocate(arena);
            if (CLSIDFromString(guidSegment, clsid) != 0)
                continue;

            try {
                return getDevicePath(instanceId, clsid);
            } catch (Exception _) {
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
            var regKey = SetupDiOpenDevRegKey(errorState, devInfoSet, devInfoData, DICS_FLAG_GLOBAL, 0,
                    DIREG_DEV, KEY_READ);
            if (Win.isInvalidHandle(regKey))
                throwLastError(errorState, "internal error (SetupDiOpenDevRegKey)");
            cleanup.add(() -> RegCloseKey(regKey));

            // read registry value (without buffer, to query length)
            var keyNameSegment = arena.allocateFrom("DeviceInterfaceGUIDs", UTF_16LE);
            var valueTypeHolder = arena.allocate(JAVA_INT);
            var valueSizeHolder = arena.allocate(JAVA_INT);
            var res = RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, NULL, valueSizeHolder);
            if (res == ERROR_FILE_NOT_FOUND)
                return List.of(); // no device interface GUIDs
            if (res != 0 && res != ERROR_MORE_DATA)
                throwException(res, "internal error (RegQueryValueExW)");

            // read registry value (with buffer)
            var valueSize = valueSizeHolder.get(JAVA_INT, 0);
            var value = arena.allocate(valueSize);
            res = RegQueryValueExW(regKey, keyNameSegment, NULL, valueTypeHolder, value, valueSizeHolder);
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
        if (SetupDiGetDevicePropertyW(errorState, devInfoSet, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
            throwLastError(errorState, "internal error (SetupDiGetDevicePropertyW - A)");

        if (propertyTypeHolder.get(JAVA_INT, 0) != DEVPROP_TYPE_UINT32)
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
        var propertyValue = getVariableLengthProperty(propertyKey, DEVPROP_TYPE_STRING, arena);
        if (propertyValue == null)
            return null;
        return propertyValue.getString(0, UTF_16LE);
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
                DEVPROP_TYPE_STRING | DEVPROP_TYPEMOD_LIST, arena);
        if (propertyValue == null)
            return null;

        return Win.createStringListFromSegment(propertyValue);
    }

    private MemorySegment getVariableLengthProperty(MemorySegment propertyKey, int propertyType, Arena arena) {

        // query length (thus no buffer)
        var propertyTypeHolder = arena.allocate(JAVA_INT);
        var requiredSizeHolder = arena.allocate(JAVA_INT);
        if (SetupDiGetDevicePropertyW(errorState, devInfoSet, devInfoData, propertyKey, propertyTypeHolder, NULL, 0,
                requiredSizeHolder, 0) == 0) {
            var err = Win.getLastError(errorState);
            if (err == ERROR_NOT_FOUND)
                return null;
            if (err != ERROR_INSUFFICIENT_BUFFER)
                throwException(err, "internal error (SetupDiGetDevicePropertyW - B)");
        }

        if (propertyTypeHolder.get(JAVA_INT, 0) != propertyType)
            throwException("internal error (unexpected property type)");

        var stringLen = (requiredSizeHolder.get(JAVA_INT, 0) + 1) / 2;

        // allocate buffer
        var propertyValueHolder = arena.allocate(JAVA_CHAR, stringLen);

        // get property value
        if (SetupDiGetDevicePropertyW(errorState, devInfoSet, devInfoData, propertyKey, propertyTypeHolder,
                propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
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
        try (var deviceInfoSet = DeviceInfoSet.ofPresentDevices(interfaceGuid, instanceId)) {
            return deviceInfoSet.getDevicePathForGuid(interfaceGuid);
        }
    }

    private String getDevicePathForGuid(MemorySegment interfaceGuid) {
        // retrieve first element of enumeration
        devIntfData = SP_DEVICE_INTERFACE_DATA.allocate(arena);
        if (SetupDiEnumDeviceInterfaces(errorState, devInfoSet, NULL, interfaceGuid, 0, devIntfData) == 0)
            throwLastError(errorState, "internal error (SetupDiEnumDeviceInterfaces)");

        // get device path
        var intfDetailData = SP_DEVICE_INTERFACE_DETAIL_DATA_W.allocate(arena, 260);
        if (SetupDiGetDeviceInterfaceDetailW(errorState, devInfoSet, devIntfData, intfDetailData,
                (int) intfDetailData.byteSize(), NULL, NULL) == 0)
            throwLastError(errorState, "Internal error (SetupDiGetDeviceInterfaceDetailW)");

        var devicePath = SP_DEVICE_INTERFACE_DETAIL_DATA_W.DevicePath(intfDetailData);
        return devicePath.getString(0, UTF_16LE);
    }
}
