// Generated by jextract

package net.codecrete.usb.windows.gen.setupapi;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.lang.foreign.ValueLayout.OfByte;
import static java.lang.foreign.ValueLayout.OfDouble;
import static java.lang.foreign.ValueLayout.OfFloat;
import static java.lang.foreign.ValueLayout.OfInt;
import static java.lang.foreign.ValueLayout.OfLong;
import static java.lang.foreign.ValueLayout.OfShort;
public class SetupAPI  {

    public static final OfByte C_CHAR = JAVA_BYTE;
    public static final OfShort C_SHORT = JAVA_SHORT;
    public static final OfInt C_INT = JAVA_INT;
    public static final OfInt C_LONG = JAVA_INT;
    public static final OfLong C_LONG_LONG = JAVA_LONG;
    public static final OfFloat C_FLOAT = JAVA_FLOAT;
    public static final OfDouble C_DOUBLE = JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = RuntimeHelper.POINTER;
    /**
     * {@snippet :
     * #define DEVPROP_TYPEMOD_LIST 8192
     * }
     */
    public static int DEVPROP_TYPEMOD_LIST() {
        return (int)8192L;
    }
    /**
     * {@snippet :
     * #define DEVPROP_TYPE_UINT32 7
     * }
     */
    public static int DEVPROP_TYPE_UINT32() {
        return (int)7L;
    }
    /**
     * {@snippet :
     * #define DEVPROP_TYPE_STRING 18
     * }
     */
    public static int DEVPROP_TYPE_STRING() {
        return (int)18L;
    }
    /**
     * {@snippet :
     * #define DICS_FLAG_GLOBAL 1
     * }
     */
    public static int DICS_FLAG_GLOBAL() {
        return (int)1L;
    }
    /**
     * {@snippet :
     * #define DIGCF_PRESENT 2
     * }
     */
    public static int DIGCF_PRESENT() {
        return (int)2L;
    }
    /**
     * {@snippet :
     * #define DIGCF_DEVICEINTERFACE 16
     * }
     */
    public static int DIGCF_DEVICEINTERFACE() {
        return (int)16L;
    }
    /**
     * {@snippet :
     * #define DIREG_DEV 1
     * }
     */
    public static int DIREG_DEV() {
        return (int)1L;
    }
    public static MethodHandle SetupDiDestroyDeviceInfoList$MH() {
        return RuntimeHelper.requireNonNull(constants$2.const$1,"SetupDiDestroyDeviceInfoList");
    }
    /**
     * {@snippet :
     * BOOL SetupDiDestroyDeviceInfoList(HDEVINFO DeviceInfoSet);
     * }
     */
    public static int SetupDiDestroyDeviceInfoList(MemorySegment DeviceInfoSet) {
        var mh$ = SetupDiDestroyDeviceInfoList$MH();
        try {
            return (int)mh$.invokeExact(DeviceInfoSet);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle SetupDiDeleteDeviceInterfaceData$MH() {
        return RuntimeHelper.requireNonNull(constants$2.const$3,"SetupDiDeleteDeviceInterfaceData");
    }
    /**
     * {@snippet :
     * BOOL SetupDiDeleteDeviceInterfaceData(HDEVINFO DeviceInfoSet, PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData);
     * }
     */
    public static int SetupDiDeleteDeviceInterfaceData(MemorySegment DeviceInfoSet, MemorySegment DeviceInterfaceData) {
        var mh$ = SetupDiDeleteDeviceInterfaceData$MH();
        try {
            return (int)mh$.invokeExact(DeviceInfoSet, DeviceInterfaceData);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}


