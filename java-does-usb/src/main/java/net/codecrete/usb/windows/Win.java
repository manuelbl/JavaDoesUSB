//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.nio.charset.StandardCharsets.UTF_16LE;

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
        return (int) callState_GetLastError$VH.get(callState, 0);
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
     * Checks if a Windows handle is invalid.
     *
     * @param handle Windows handle
     * @return {@code true} if the handle is invalid, {@code false} otherwise
     */
    public static boolean isInvalidHandle(long handle) {
        return handle == -1L;
    }

    /**
     * Creates a copy of the string list in the memory segment.
     * <p>
     * The string list is a series of null-terminated UTF-16 (wide character) strings.
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
            var str = segment.getString(offset, UTF_16LE);
            offset += str.length() * 2 + 2;
            stringList.add(str);
        }
        return stringList;
    }
}
