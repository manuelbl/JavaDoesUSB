//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.function.Consumer;

/**
 * Helper functions for the foreign memory API.
 */
public class ForeignMemory {

    /**
     * Adds a custom cleanup action which will be execution when the session of the
     * specified memory segment ends.
     * <p>
     * Note that the {@code segment}'s session will already be closed. The {@code segment}
     * can no longer be used for function call. For that reason, the cleanup
     * action is passed a copy of the segment (separate Java instance pointing to the
     * same native memory).
     * </p>
     * @param segment the segment to be accessed during the cleanup
     * @param action the cleanup action
     */
    public static void addCloseAction(MemorySegment segment, Consumer<MemorySegment> action) {
        var size = segment.byteSize();
        var address = segment.address();
        Runnable closeAction = () -> {
            try (MemorySession closingSession = MemorySession.openConfined()) {
                var dup = MemorySegment.ofAddress(address, size, closingSession);
                action.accept(dup);
            }
        };
        segment.session().addCloseAction(closeAction);
    }
}
