//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.*;

/**
 * Windows Setup API functions, constants and structures.
 */
public class SetupApi {

    public static final int DIGCF_DEFAULT = 0x00000001;
    public static final int DIGCF_PRESENT = 0x00000002;
    public static final int DIGCF_ALLCLASSES = 0x00000004;
    public static final int DIGCF_PROFILE = 0x00000008;
    public static final int DIGCF_DEVICEINTERFACE = 0x00000010;

    public static final int SPDRP_ADDRESS = 0x0000001C;  // Device Address (R)

    private static final GroupLayout GUID$Struct;
    private static final VarHandle GUID_Data1;
    private static final VarHandle GUID_Data2;
    private static final VarHandle GUID_Data3;
    private static final long GUID_Data4$Offset;

    public static final GroupLayout SP_DEVINFO_DATA$Struct;
    public static final VarHandle SP_DEVINFO_DATA_cbSize;
    public static final VarHandle SP_DEVINFO_DATA_DevInst;

    public static final GroupLayout SP_DEVICE_INTERFACE_DATA$Struct;
    public static final VarHandle SP_DEVICE_INTERFACE_DATA_cbSize;

    public static final GroupLayout SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct;
    public static final VarHandle SP_DEVICE_INTERFACE_DETAIL_DATA_cbSize;
    public static final long SP_DEVICE_INTERFACE_DETAIL_DATA_DevicePath$Offset;

    private static final MethodHandle SetupDiGetClassDevsW$Func;
    private static final MethodHandle SetupDiDestroyDeviceInfoList$Func;
    public static final MemorySegment GUID_DEVINTERFACE_USB_DEVICE;
    private static final MethodHandle SetupDiEnumDeviceInfo$Func;
    private static final MethodHandle SetupDiEnumDeviceInterfaces$Func;
    private static final MethodHandle SetupDiGetDeviceInterfaceDetailW$Func;
    private static final MethodHandle SetupDiGetDeviceRegistryPropertyW$Func;

    static {
        var session = MemorySession.openShared();
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.libraryLookup("SetupAPI", session);

        // typedef struct _GUID {
        //    unsigned long  Data1;
        //    unsigned short Data2;
        //    unsigned short Data3;
        //    unsigned char  Data4[ 8 ];
        //} GUID;
        GUID$Struct = MemoryLayout.structLayout(
                JAVA_INT.withName("Data1"),
                JAVA_SHORT.withName("Data2"),
                JAVA_SHORT.withName("Data3"),
                MemoryLayout.sequenceLayout(8, JAVA_BYTE).withName("Data4")
        );
        GUID_Data1 = GUID$Struct.varHandle(groupElement("Data1"));
        GUID_Data2 = GUID$Struct.varHandle(groupElement("Data2"));
        GUID_Data3 = GUID$Struct.varHandle(groupElement("Data3"));
        GUID_Data4$Offset = GUID$Struct.byteOffset(groupElement("Data4"));

        // typedef struct _SP_DEVINFO_DATA {
        //    DWORD cbSize;
        //    GUID  ClassGuid;
        //    DWORD DevInst;    // DEVINST handle
        //    ULONG_PTR Reserved;
        // } SP_DEVINFO_DATA, *PSP_DEVINFO_DATA;
        SP_DEVINFO_DATA$Struct = MemoryLayout.structLayout(
                JAVA_INT.withName("cbSize"),
                GUID$Struct.withName("ClassGuid"),
                JAVA_INT.withName("DevInst"),
                ADDRESS.withName("Reserved")
        );
        SP_DEVINFO_DATA_cbSize = SP_DEVINFO_DATA$Struct.varHandle(groupElement("cbSize"));
        SP_DEVINFO_DATA_DevInst = SP_DEVINFO_DATA$Struct.varHandle(groupElement("DevInst"));

        // typedef struct _SP_DEVICE_INTERFACE_DATA {
        //    DWORD cbSize;
        //    GUID  InterfaceClassGuid;
        //    DWORD Flags;
        //    ULONG_PTR Reserved;
        // } SP_DEVICE_INTERFACE_DATA, *PSP_DEVICE_INTERFACE_DATA;
        SP_DEVICE_INTERFACE_DATA$Struct = MemoryLayout.structLayout(
                JAVA_INT.withName("cbSize"),
                GUID$Struct.withName("InterfaceClassGuid"),
                JAVA_INT.withName("Flags"),
                ADDRESS.withName("Reserved")
        );
        SP_DEVICE_INTERFACE_DATA_cbSize = SP_DEVICE_INTERFACE_DATA$Struct.varHandle(groupElement("cbSize"));

        // typedef struct _SP_DEVICE_INTERFACE_DETAIL_DATA_W {
        //    DWORD  cbSize;
        //    WCHAR  DevicePath[ANYSIZE_ARRAY];
        //} SP_DEVICE_INTERFACE_DETAIL_DATA_W, *PSP_DEVICE_INTERFACE_DETAIL_DATA_W;
        SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct = MemoryLayout.structLayout(
                JAVA_INT.withName("cbSize"),
                MemoryLayout.sequenceLayout(260, JAVA_CHAR).withName("DevicePath")
        );
        SP_DEVICE_INTERFACE_DETAIL_DATA_cbSize = SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct.varHandle(groupElement("cbSize"));
        SP_DEVICE_INTERFACE_DETAIL_DATA_DevicePath$Offset = SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct.byteOffset(groupElement("DevicePath"));

        // WINSETUPAPI HDEVINFO SetupDiGetClassDevsW(
        //  [in, optional] const GUID *ClassGuid,
        //  [in, optional] PCWSTR     Enumerator,
        //  [in, optional] HWND       hwndParent,
        //  [in]           DWORD      Flags
        //);
        SetupDiGetClassDevsW$Func = linker.downcallHandle(
                lookup.lookup("SetupDiGetClassDevsW").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT)
        );

        // BOOL
        // SetupDiDestroyDeviceInfoList(
        //    _In_ HDEVINFO DeviceInfoSet
        // );
        SetupDiDestroyDeviceInfoList$Func = linker.downcallHandle(
                lookup.lookup("SetupDiDestroyDeviceInfoList").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );

        //BOOL
        //SetupDiEnumDeviceInfo(
        //    _In_ HDEVINFO DeviceInfoSet,
        //    _In_ DWORD MemberIndex,
        //    _Out_ PSP_DEVINFO_DATA DeviceInfoData
        //    );
        SetupDiEnumDeviceInfo$Func = linker.downcallHandle(
                lookup.lookup("SetupDiEnumDeviceInfo").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS)
        );

        // WINSETUPAPI BOOL SetupDiEnumDeviceInterfaces(
        //  [in]           HDEVINFO                  DeviceInfoSet,
        //  [in, optional] PSP_DEVINFO_DATA          DeviceInfoData,
        //  [in]           const GUID                *InterfaceClassGuid,
        //  [in]           DWORD                     MemberIndex,
        //  [out]          PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData
        //);
        SetupDiEnumDeviceInterfaces$Func = linker.downcallHandle(
                lookup.lookup("SetupDiEnumDeviceInterfaces").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS)
        );

        // WINSETUPAPI BOOL SetupDiGetDeviceInterfaceDetailW(
        //  [in]            HDEVINFO                           DeviceInfoSet,
        //  [in]            PSP_DEVICE_INTERFACE_DATA          DeviceInterfaceData,
        //  [out, optional] PSP_DEVICE_INTERFACE_DETAIL_DATA_W DeviceInterfaceDetailData,
        //  [in]            DWORD                              DeviceInterfaceDetailDataSize,
        //  [out, optional] PDWORD                             RequiredSize,
        //  [out, optional] PSP_DEVINFO_DATA                   DeviceInfoData
        //);
        SetupDiGetDeviceInterfaceDetailW$Func = linker.downcallHandle(
                lookup.lookup("SetupDiGetDeviceInterfaceDetailW").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
        );

        // WINSETUPAPI BOOL SetupDiGetDeviceRegistryPropertyW(
        //  [in]            HDEVINFO         DeviceInfoSet,
        //  [in]            PSP_DEVINFO_DATA DeviceInfoData,
        //  [in]            DWORD            Property,
        //  [out, optional] PDWORD           PropertyRegDataType,
        //  [out, optional] PBYTE            PropertyBuffer,
        //  [in]            DWORD            PropertyBufferSize,
        //  [out, optional] PDWORD           RequiredSize
        //);
        SetupDiGetDeviceRegistryPropertyW$Func = linker.downcallHandle(
                lookup.lookup("SetupDiGetDeviceRegistryPropertyW").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS)
        );

        // A5DCBF10-6530-11D2-901F-00C04FB951ED
        GUID_DEVINTERFACE_USB_DEVICE = CreateGUID(0xA5DCBF10, (short) 0x6530, (short) 0x11D2,
                (byte) 0x90, (byte) 0x1F, (byte) 0x00, (byte) 0xC0,
                (byte) 0x4F, (byte) 0xB9, (byte) 0x51, (byte) 0xED);
    }

    // WINSETUPAPI HDEVINFO SetupDiGetClassDevsW(
    //  [in, optional] const GUID *ClassGuid,
    //  [in, optional] PCWSTR     Enumerator,
    //  [in, optional] HWND       hwndParent,
    //  [in]           DWORD      Flags
    //);
    public static MemoryAddress SetupDiGetClassDevsW(Addressable classGuid, Addressable enumerator,
                                                     Addressable hwndParent, int flags) {
        try {
            return (MemoryAddress) SetupDiGetClassDevsW$Func.invokeExact(classGuid, enumerator,
                    hwndParent, flags);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // BOOL
    // SetupDiDestroyDeviceInfoList(
    //    _In_ HDEVINFO DeviceInfoSet
    // );
    public static boolean SetupDiDestroyDeviceInfoList(Addressable deviceInfoSet) {
        try {
            return (int) SetupDiDestroyDeviceInfoList$Func.invokeExact(deviceInfoSet) != 0;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    //BOOL
    //SetupDiEnumDeviceInfo(
    //    _In_ HDEVINFO DeviceInfoSet,
    //    _In_ DWORD MemberIndex,
    //    _Out_ PSP_DEVINFO_DATA DeviceInfoData
    //    );
    public static boolean SetupDiEnumDeviceInfo(Addressable deviceInfoSet, int memberIndex, Addressable deviceInfoData) {
        try {
            return (int) SetupDiEnumDeviceInfo$Func.invokeExact(deviceInfoSet, memberIndex, deviceInfoData) != 0;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // WINSETUPAPI BOOL SetupDiEnumDeviceInterfaces(
    //  [in]           HDEVINFO                  DeviceInfoSet,
    //  [in, optional] PSP_DEVINFO_DATA          DeviceInfoData,
    //  [in]           const GUID                *InterfaceClassGuid,
    //  [in]           DWORD                     MemberIndex,
    //  [out]          PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData
    //);
    public static boolean SetupDiEnumDeviceInterfaces(Addressable deviceInfoSet, Addressable deviceInfoData,
                                                      Addressable interfaceClassGuid, int memberIndex,
                                                      Addressable deviceInterfaceData) {
        try {
            return (int) SetupDiEnumDeviceInterfaces$Func.invokeExact(deviceInfoSet, deviceInfoData,
                    interfaceClassGuid, memberIndex,
                    deviceInterfaceData) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // WINSETUPAPI BOOL SetupDiGetDeviceInterfaceDetailW(
    //  [in]            HDEVINFO                           DeviceInfoSet,
    //  [in]            PSP_DEVICE_INTERFACE_DATA          DeviceInterfaceData,
    //  [out, optional] PSP_DEVICE_INTERFACE_DETAIL_DATA_W DeviceInterfaceDetailData,
    //  [in]            DWORD                              DeviceInterfaceDetailDataSize,
    //  [out, optional] PDWORD                             RequiredSize,
    //  [out, optional] PSP_DEVINFO_DATA                   DeviceInfoData
    //);
    public static boolean SetupDiGetDeviceInterfaceDetailW(Addressable deviceInfoSet, Addressable deviceInterfaceData,
                                                           Addressable deviceInterfaceDetailData, int deviceInterfaceDetailDataSize,
                                                           Addressable requiredSize, Addressable deviceInfoData) {
        try {
            return (int) SetupDiGetDeviceInterfaceDetailW$Func.invokeExact(deviceInfoSet, deviceInterfaceData,
                    deviceInterfaceDetailData, deviceInterfaceDetailDataSize,
                    requiredSize, deviceInfoData) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // WINSETUPAPI BOOL SetupDiGetDeviceRegistryPropertyW(
    //  [in]            HDEVINFO         DeviceInfoSet,
    //  [in]            PSP_DEVINFO_DATA DeviceInfoData,
    //  [in]            DWORD            Property,
    //  [out, optional] PDWORD           PropertyRegDataType,
    //  [out, optional] PBYTE            PropertyBuffer,
    //  [in]            DWORD            PropertyBufferSize,
    //  [out, optional] PDWORD           RequiredSize
    //);
    public static boolean SetupDiGetDeviceRegistryPropertyW(Addressable deviceInfoSet, Addressable deviceInfoData,
                                                            int property, Addressable propertyRegDataType,
                                                            Addressable propertyBuffer, int propertyBufferSize,
                                                            Addressable requiredSize) {
        try {
            return (int) SetupDiGetDeviceRegistryPropertyW$Func.invokeExact(deviceInfoSet, deviceInfoData,
                    property, propertyRegDataType,
                    propertyBuffer, propertyBufferSize,
                    requiredSize) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a GUID in native memory.
     *
     * @param data1   Group 1 (4 bytes).
     * @param data2   Group 2 (2 bytes).
     * @param data3   Group 3 (2 bytes).
     * @param data4_0 Byte 0 of group 4
     * @param data4_1 Byte 1 of group 4
     * @param data4_2 Byte 2 of group 4
     * @param data4_3 Byte 3 of group 4
     * @param data4_4 Byte 4 of group 4
     * @param data4_5 Byte 5 of group 4
     * @param data4_6 Byte 6 of group 4
     * @param data4_7 Byte 7 of group 4
     * @return GUID as memory segment
     */
    public static MemorySegment CreateGUID(int data1, short data2, short data3,
                                           byte data4_0, byte data4_1, byte data4_2, byte data4_3,
                                           byte data4_4, byte data4_5, byte data4_6, byte data4_7) {
        var guid = MemorySegment.allocateNative(GUID$Struct, MemorySession.global());
        GUID_Data1.set(guid, data1);
        GUID_Data2.set(guid, data2);
        GUID_Data3.set(guid, data3);
        guid.set(JAVA_BYTE, GUID_Data4$Offset, data4_0);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 1, data4_1);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 2, data4_2);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 3, data4_3);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 4, data4_4);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 5, data4_5);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 6, data4_6);
        guid.set(JAVA_BYTE, GUID_Data4$Offset + 7, data4_7);
        return guid;
    }
}
