//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.string.string;

import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

/**
 * Helper functions for Linux
 */
public class Linux {

    /**
     * Call state for capturing the {@code errno} value.
     */
    public static final Linker.Option.CaptureCallState ERRNO_STATE = Linker.Option.captureCallState("errno");
    private static final VarHandle callState_errno$VH =
            ERRNO_STATE.layout().varHandle(MemoryLayout.PathElement.groupElement("errno"));

    /**
     * Gets the error message for the specified error code (returned by {@code errno}).
     *
     * @param err error code
     * @return error message
     */
    public static String getErrorMessage(int err) {
        return string.strerror(err).getUtf8String(0);
    }

    /**
     * Gets the error code from the memory segment.
     * <p>
     * The memory segment is assumed to have the layout {@link #ERRNO_STATE}.
     * </p>
     *
     * @param errno memory segment with error code
     * @return error code
     */
    public static int getErrno(MemorySegment errno) {
        return (int) callState_errno$VH.get(errno);
    }
}
