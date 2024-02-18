//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.windows.gen.kernel32._GUID;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Windows helpers.
 */
public class Win {
    private Win() {
    }

    /**
     * Call state for capturing the {@code GetLastError()} value.
     */
    public static final Linker.Option LAST_ERROR_STATE = Linker.Option.captureCallState("GetLastError");
    private static final StructLayout LAST_ERROR_STATE_LAYOUT = Linker.Option.captureStateLayout();

    private static final VarHandle callState_GetLastError$VH =
            LAST_ERROR_STATE_LAYOUT.varHandle(PathElement.groupElement("GetLastError"));

    static MemorySegment allocateErrorState(Arena arena) {
        return arena.allocate(LAST_ERROR_STATE_LAYOUT);
    }

    /**
     * Returns the error code captured using the call state {@link #LAST_ERROR_STATE}.
     *
     * @param callState the call state
     * @return the error code
     */
    public static int getLastError(MemorySegment callState) {
        return callState.get(JAVA_INT, 0);
        // TODO: revert to varhandle
        // return (int) callState_GetLastError$VH.get(callState);
    }

    /**
     * Checks if a Windows handle is invalid.
     *
     * @param handle Windows handle
     * @return {@code true} if the handle is invalid, {@code false} otherwise
     */
    public static boolean isInvalidHandle(MemorySegment handle) {
        return handle.address() == -1L;
    }

    /**
     * Creates a memory segment as a copy of a Java string.
     * <p>
     * The memory segment contains a copy of the string (null-terminated, UTF-16/wide characters).
     * </p>
     *
     * @param str   the string to copy
     * @param arena the arena for the memory segment
     * @return the resulting memory segment
     */
    public static MemorySegment createSegmentFromString(String str, Arena arena) {
        // allocate segment (including space for terminating null)
        var segment = arena.allocate(ValueLayout.JAVA_CHAR, str.length() + 1L);
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
        var len = 0;
        while (segment.get(JAVA_CHAR, len) != 0) {
            len += 2;
        }

        return new String(segment.asSlice(0, len).toArray(JAVA_CHAR));
    }

    /**
     * Creates a copy of the string list in the memory segment.
     * <p>
     * The string list a a series of null-terminated UTF-16 (wide character) strings.
     * The list is terminated with yet another null character.
     * </p>
     *
     * @param segment the memory segment
     * @return copied string list
     */
    public static List<String> createStringListFromSegment(MemorySegment segment) {
        var stringList = new ArrayList<String>();
        var offset = 0;
        while (segment.get(JAVA_CHAR, offset) != '\0') {
            var str = Win.createStringFromSegment(segment.asSlice(offset));
            offset += str.length() * 2 + 2;
            stringList.add(str);
        }
        return stringList;
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
    @SuppressWarnings({"java:S117", "java:S107", "resource"})
    public static MemorySegment createGUID(int data1, short data2, short data3, byte data4_0, byte data4_1,
                                           byte data4_2, byte data4_3, byte data4_4, byte data4_5, byte data4_6,
                                           byte data4_7) {
        var guid = Arena.global().allocate(_GUID.layout());
        setGUID(guid, data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4, data4_5, data4_6, data4_7);
        return guid;
    }

    @SuppressWarnings({"java:S117", "java:S107"})
    public static void setGUID(MemorySegment guid, int data1, short data2, short data3, byte data4_0, byte data4_1,
                               byte data4_2, byte data4_3, byte data4_4, byte data4_5, byte data4_6, byte data4_7) {
        _GUID.Data1(guid, data1);
        _GUID.Data2(guid, data2);
        _GUID.Data3(guid, data3);
        var data4 = _GUID.Data4(guid);
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
