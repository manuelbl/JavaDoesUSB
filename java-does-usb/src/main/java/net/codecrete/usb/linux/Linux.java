//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.string.string;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Helper functions for Linux
 */
class Linux {

    private Linux() {
    }

    /**
     * Call state for capturing the {@code errno} value.
     */
    static final Linker.Option ERRNO_STATE = Linker.Option.captureCallState("errno");
    private static final StructLayout ERRNO_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle callState_errno$VH =
            ERRNO_STATE_LAYOUT.varHandle(PathElement.groupElement("errno"));

    static MemorySegment allocateErrorState(Arena arena) {
        return arena.allocate(ERRNO_STATE_LAYOUT.byteSize());
    }

    /**
     * Gets the error message for the specified error code (returned by {@code errno}).
     *
     * @param err error code
     * @return error message
     */
    static String getErrorMessage(int err) {
        return string.strerror(err).getString(0);
    }

    /**
     * Gets the error code from the memory segment.
     * <p>
     * The memory segment is assumed to have the layout {@link #ERRNO_STATE}.
     * </p>
     *
     * @param errorState memory segment with error code
     * @return error code
     */
    static int getErrno(MemorySegment errorState) {
        return errorState.get(ValueLayout.JAVA_INT, 0);
        // TODO: revert to varhandle
        // return (int) callState_errno$VH.get(errorState);
    }
}
