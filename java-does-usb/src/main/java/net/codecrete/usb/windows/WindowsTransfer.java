//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.common.Transfer;

import java.lang.foreign.MemorySegment;

public class WindowsTransfer extends Transfer {
    MemorySegment overlapped;
}
