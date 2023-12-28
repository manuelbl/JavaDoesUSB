//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

/**
 * Data structures and constants related to the USB device file system.
 * <p>
 * The <i>usbdev_fs</i> header files compute these constants using function like macros.
 * Thus, they cannot be generated using <i>jextract</i>.
 * </p>
 */
class UsbDevFS {

    private UsbDevFS() {
    }

    // constants that jextract cannot generate as they are built from function-like macros
    static final long CLAIMINTERFACE = 0x8004550FL;
    static final long RELEASEINTERFACE = 0x80045510L;
    static final long SETINTERFACE = 0x80085504L;
    static final long CLEAR_HALT = 0x80045515L;
    static final long SUBMITURB = 0x8038550AL;
    static final long DISCARDURB = 0x550BL;
    static final long REAPURBNDELAY = 0x4008550DL;
    static final long DISCONNECT_CLAIM = 0x8108551BL;
    static final int CONNECT = 0x5517;
    static final long IOCTL = 0xC0105512L;
}
