//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.linux.gen.errno.errno;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public class IO {

    public static int getErrno() {
        return errno.__errno_location().get(JAVA_INT, 0);
    }
}
