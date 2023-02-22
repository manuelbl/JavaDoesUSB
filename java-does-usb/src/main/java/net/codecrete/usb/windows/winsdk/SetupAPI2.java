//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows.winsdk;

import net.codecrete.usb.windows.Win;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Native function calls for SetupAPI.
 * <p>
 * This code is manually created to include the additional parameters for capturing
 * {@code GetLastError()} until jextract catches up and can generate the corresponding code.
 * </p>
 */
public class SetupAPI2 {

    static {
        System.loadLibrary("SetupAPI");
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    private static final FunctionDescriptor SetupDiGetDevicePropertyW$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT);

    private static final MethodHandle SetupDiGetDevicePropertyW$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiGetDevicePropertyW").get(), SetupDiGetDevicePropertyW$FUNC, Win.LAST_ERROR_STATE);

    public static int SetupDiGetDevicePropertyW(MemorySegment DeviceInfoSet, MemorySegment DeviceInfoData,
                                                MemorySegment PropertyKey, MemorySegment PropertyType,
                                                MemorySegment PropertyBuffer, int PropertyBufferSize,
                                                MemorySegment RequiredSize, int Flags, MemorySegment lastErrorState) {
        try {
            return (int) SetupDiGetDevicePropertyW$MH.invokeExact(lastErrorState, DeviceInfoSet, DeviceInfoData,
                    PropertyKey, PropertyType, PropertyBuffer, PropertyBufferSize, RequiredSize, Flags);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiEnumDeviceInfo$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS,
            JAVA_INT, ADDRESS);

    private static final MethodHandle SetupDiEnumDeviceInfo$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiEnumDeviceInfo").get(), SetupDiEnumDeviceInfo$FUNC, Win.LAST_ERROR_STATE);

    public static int SetupDiEnumDeviceInfo(MemorySegment DeviceInfoSet, int MemberIndex,
                                            MemorySegment DeviceInfoData, MemorySegment lastErrorState) {
        try {
            return (int) SetupDiEnumDeviceInfo$MH.invokeExact(lastErrorState, DeviceInfoSet, MemberIndex,
                    DeviceInfoData);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiOpenDevRegKey$FUNC = FunctionDescriptor.of(ADDRESS, ADDRESS,
            ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);

    private static final MethodHandle SetupDiOpenDevRegKey$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiOpenDevRegKey").get(), SetupDiOpenDevRegKey$FUNC, Win.LAST_ERROR_STATE);

    public static MemorySegment SetupDiOpenDevRegKey(MemorySegment DeviceInfoSet, MemorySegment DeviceInfoData,
                                                     int Scope, int HwProfile, int KeyType, int samDesired,
                                                     MemorySegment lastErrorState) {
        try {
            return (MemorySegment) SetupDiOpenDevRegKey$MH.invokeExact(lastErrorState, DeviceInfoSet, DeviceInfoData,
                    Scope, HwProfile, KeyType, samDesired);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiGetClassDevsW$FUNC = FunctionDescriptor.of(ADDRESS, ADDRESS,
            ADDRESS, ADDRESS, JAVA_INT);

    private static final MethodHandle SetupDiGetClassDevsW$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiGetClassDevsW").get(), SetupDiGetClassDevsW$FUNC, Win.LAST_ERROR_STATE);

    public static MemorySegment SetupDiGetClassDevsW(MemorySegment ClassGuid, MemorySegment Enumerator,
                                                     MemorySegment hwndParent, int Flags,
                                                     MemorySegment lastErrorState) {
        try {
            return (MemorySegment) SetupDiGetClassDevsW$MH.invokeExact(lastErrorState, ClassGuid, Enumerator,
                    hwndParent, Flags);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiEnumDeviceInterfaces$FUNC = FunctionDescriptor.of(JAVA_INT,
            ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

    private static final MethodHandle SetupDiEnumDeviceInterfaces$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiEnumDeviceInterfaces").get(), SetupDiEnumDeviceInterfaces$FUNC, Win.LAST_ERROR_STATE);

    public static int SetupDiEnumDeviceInterfaces(MemorySegment DeviceInfoSet, MemorySegment DeviceInfoData,
                                                  MemorySegment InterfaceClassGuid, int MemberIndex,
                                                  MemorySegment DeviceInterfaceData, MemorySegment lastErrorState) {
        try {
            return (int) SetupDiEnumDeviceInterfaces$MH.invokeExact(lastErrorState, DeviceInfoSet, DeviceInfoData,
                    InterfaceClassGuid, MemberIndex, DeviceInterfaceData);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiGetDeviceInterfaceDetailW$FUNC = FunctionDescriptor.of(JAVA_INT,
            ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    private static final MethodHandle SetupDiGetDeviceInterfaceDetailW$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiGetDeviceInterfaceDetailW").get(), SetupDiGetDeviceInterfaceDetailW$FUNC, Win.LAST_ERROR_STATE);

    public static int SetupDiGetDeviceInterfaceDetailW(MemorySegment DeviceInfoSet, MemorySegment DeviceInterfaceData
            , MemorySegment DeviceInterfaceDetailData, int DeviceInterfaceDetailDataSize, MemorySegment RequiredSize,
                                                       MemorySegment DeviceInfoData, MemorySegment lastErrorState) {
        try {
            return (int) SetupDiGetDeviceInterfaceDetailW$MH.invokeExact(lastErrorState, DeviceInfoSet,
                    DeviceInterfaceData, DeviceInterfaceDetailData, DeviceInterfaceDetailDataSize, RequiredSize,
                    DeviceInfoData);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiCreateDeviceInfoList$FUNC = FunctionDescriptor.of(ADDRESS, ADDRESS
            , ADDRESS);

    private static final MethodHandle SetupDiCreateDeviceInfoList$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiCreateDeviceInfoList").get(), SetupDiCreateDeviceInfoList$FUNC, Win.LAST_ERROR_STATE);

    public static MemorySegment SetupDiCreateDeviceInfoList(MemorySegment ClassGuid, MemorySegment hwndParent,
                                                            MemorySegment lastErrorState) {
        try {
            return (MemorySegment) SetupDiCreateDeviceInfoList$MH.invokeExact(lastErrorState, ClassGuid, hwndParent);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiOpenDeviceInfoW$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

    private static final MethodHandle SetupDiOpenDeviceInfoW$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiOpenDeviceInfoW").get(), SetupDiOpenDeviceInfoW$FUNC, Win.LAST_ERROR_STATE);

    public static int SetupDiOpenDeviceInfoW(MemorySegment DeviceInfoSet, MemorySegment DeviceInstanceId,
                                             MemorySegment hwndParent, int OpenFlags, MemorySegment DeviceInfoData,
                                             MemorySegment lastErrorState) {
        try {
            return (int) SetupDiOpenDeviceInfoW$MH.invokeExact(lastErrorState, DeviceInfoSet, DeviceInstanceId,
                    hwndParent, OpenFlags, DeviceInfoData);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor SetupDiOpenDeviceInterfaceW$FUNC = FunctionDescriptor.of(JAVA_INT,
            ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

    private static final MethodHandle SetupDiOpenDeviceInterfaceW$MH = LINKER.downcallHandle(LOOKUP.find(
            "SetupDiOpenDeviceInterfaceW").get(), SetupDiOpenDeviceInterfaceW$FUNC, Win.LAST_ERROR_STATE);

    public static int SetupDiOpenDeviceInterfaceW(MemorySegment DeviceInfoSet, MemorySegment DevicePath,
                                                  int OpenFlags, MemorySegment DeviceInterfaceData,
                                                  MemorySegment lastErrorState) {
        try {
            return (int) SetupDiOpenDeviceInterfaceW$MH.invokeExact(lastErrorState, DeviceInfoSet, DevicePath,
                    OpenFlags, DeviceInterfaceData);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

}
