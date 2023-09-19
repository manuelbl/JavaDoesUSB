//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.common.Transfer;

import java.lang.foreign.MemorySegment;

class WindowsTransfer extends Transfer {
    private MemorySegment overlapped;

    public MemorySegment overlapped() {
        return overlapped;
    }

    public void setOverlapped(MemorySegment overlapped) {
        this.overlapped = overlapped;
    }
}
