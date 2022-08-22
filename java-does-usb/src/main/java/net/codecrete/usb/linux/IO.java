//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.errno.errno;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public class IO {

    public static int getErrno() {
        try (var session = MemorySession.openConfined()) {
            var location = (MemoryAddress) errno.__errno_location();
            var errnoSegment = MemorySegment.ofAddress(location, JAVA_INT.byteSize(), session);
            return errnoSegment.get(JAVA_INT, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
