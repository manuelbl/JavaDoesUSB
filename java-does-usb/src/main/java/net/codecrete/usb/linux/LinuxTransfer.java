//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.common.Transfer;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_urb;

import java.lang.foreign.MemorySegment;

public class LinuxTransfer extends Transfer {
    /**
     * USB request buffer.
     *
     * @see usbdevfs_urb
     */
    MemorySegment urb;
}
