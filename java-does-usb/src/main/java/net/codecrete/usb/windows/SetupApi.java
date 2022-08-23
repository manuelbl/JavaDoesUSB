//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.*;

/**
 * Windows Setup API functions, constants and structures.
 */
public class SetupApi {

    private static final GroupLayout GUID$Struct;
    private static final VarHandle GUID_Data1;
    private static final VarHandle GUID_Data2;
    private static final VarHandle GUID_Data3;
    private static final long GUID_Data4$Offset;

    public static final GroupLayout SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct;
    public static final VarHandle SP_DEVICE_INTERFACE_DETAIL_DATA_W_cbSize;
    public static final long SP_DEVICE_INTERFACE_DETAIL_DATA_W_DevicePath$Offset;

    public static final MemorySegment GUID_DEVINTERFACE_USB_DEVICE;

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

        // typedef struct _SP_DEVICE_INTERFACE_DETAIL_DATA_W {
        //    DWORD  cbSize;
        //    WCHAR  DevicePath[ANYSIZE_ARRAY];
        //} SP_DEVICE_INTERFACE_DETAIL_DATA_W, *PSP_DEVICE_INTERFACE_DETAIL_DATA_W;
        SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct = MemoryLayout.structLayout(
                JAVA_INT.withName("cbSize"),
                MemoryLayout.sequenceLayout(260, JAVA_CHAR).withName("DevicePath")
        );
        SP_DEVICE_INTERFACE_DETAIL_DATA_W_cbSize = SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct.varHandle(groupElement("cbSize"));
        SP_DEVICE_INTERFACE_DETAIL_DATA_W_DevicePath$Offset = SP_DEVICE_INTERFACE_DETAIL_DATA_W$Struct.byteOffset(groupElement("DevicePath"));

        // A5DCBF10-6530-11D2-901F-00C04FB951ED
        GUID_DEVINTERFACE_USB_DEVICE = CreateGUID(0xA5DCBF10, (short) 0x6530, (short) 0x11D2,
                (byte) 0x90, (byte) 0x1F, (byte) 0x00, (byte) 0xC0,
                (byte) 0x4F, (byte) 0xB9, (byte) 0x51, (byte) 0xED);
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
