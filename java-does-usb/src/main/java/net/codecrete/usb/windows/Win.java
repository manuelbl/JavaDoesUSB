//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.windows.gen.kernel32.GUID;
import net.codecrete.usb.windows.gen.stdlib.StdLib;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;

/**
 * Windows helpers.
 */
public class Win {

    /**
     * Checks if a Windows handle is invalid.
     *
     * @param handle Windows handle
     * @return {@code true} if the handle is invalid, {@code false} otherwise
     */
    public static boolean IsInvalidHandle(MemoryAddress handle) {
        return handle.toRawLongValue() == -1L;
    }

    /**
     * Creates a memory segment as a copy of a Java string.
     * <p>
     * The memory segment contains a copy of the string (null-terminated, UTF-16/wide characters).
     * </p>
     *
     * @param str     the string to copy
     * @param session the memory session for the memory segment
     * @return the resulting memory segment
     */
    public static MemorySegment createSegmentFromString(String str, MemorySession session) {
        // allocate segment (including space for terminating null)
        var segment = session.allocateArray(ValueLayout.JAVA_CHAR, str.length() + 1);
        // copy characters
        segment.copyFrom(MemorySegment.ofArray(str.toCharArray()));
        return segment;
    }

    /**
     * Creates a copy of the string in the memory segment.
     * <p>
     * The string must be a null-terminated UTF-16 (wide character) string.
     * </p>
     *
     * @param segment the memory segment
     * @return copied string
     */
    public static String createStringFromSegment(MemorySegment segment) {
        long strLen = StdLib.wcslen(segment);
        return new String(segment.asSlice(0, 2L * strLen).toArray(JAVA_CHAR));
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
    public static MemorySegment CreateGUID(int data1, short data2, short data3, byte data4_0, byte data4_1,
                                           byte data4_2, byte data4_3, byte data4_4, byte data4_5, byte data4_6,
                                           byte data4_7) {
        var guid = MemorySegment.allocateNative(GUID.$LAYOUT(), MemorySession.global());
        setGUID(guid, data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4, data4_5, data4_6, data4_7);
        return guid;
    }

    public static void setGUID(MemorySegment guid, int data1, short data2, short data3, byte data4_0, byte data4_1,
                               byte data4_2, byte data4_3, byte data4_4, byte data4_5, byte data4_6, byte data4_7) {
        GUID.Data1$set(guid, data1);
        GUID.Data2$set(guid, data2);
        GUID.Data3$set(guid, data3);
        var data4 = GUID.Data4$slice(guid);
        data4.set(JAVA_BYTE, 0, data4_0);
        data4.set(JAVA_BYTE, 1, data4_1);
        data4.set(JAVA_BYTE, 2, data4_2);
        data4.set(JAVA_BYTE, 3, data4_3);
        data4.set(JAVA_BYTE, 4, data4_4);
        data4.set(JAVA_BYTE, 5, data4_5);
        data4.set(JAVA_BYTE, 6, data4_6);
        data4.set(JAVA_BYTE, 7, data4_7);
    }
}
