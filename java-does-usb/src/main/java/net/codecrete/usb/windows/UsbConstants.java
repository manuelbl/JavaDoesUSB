//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.MemorySegment;

/**
 * USB constants (general ones and Windows specific ones)
 */
@SuppressWarnings({"java:S125", "java:S1192", "java:S115", "java:S100"})
class UsbConstants {

    private UsbConstants() {
    }

    static final byte USB_REQUEST_GET_DESCRIPTOR = 0x06;

    // A5DCBF10-6530-11D2-901F-00C04FB951ED
    static final MemorySegment GUID_DEVINTERFACE_USB_DEVICE = Win.createGUID(0xA5DCBF10, (short) 0x6530,
            (short) 0x11D2, (byte) 0x90, (byte) 0x1F, (byte) 0x00, (byte) 0xC0, (byte) 0x4F, (byte) 0xB9, (byte) 0x51
            , (byte) 0xED);

    // f18a0e88-c30c-11d0-8815-00a0c906bed8
    static final MemorySegment GUID_DEVINTERFACE_USB_HUB = Win.createGUID(0xf18a0e88, (short) 0xc30c,
            (short) 0x11d0, (byte) 0x88, (byte) 0x15, (byte) 0x00, (byte) 0xa0, (byte) 0xc9, (byte) 0x06, (byte) 0xbe
            , (byte) 0xd8);
}
