//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

/**
 * Data structures and constants related to the USB device file system.
 */
public class USBDevFS {

    public static final long CONTROL = 0xc0185500L;
    public static final long BULK = 0xc0185502L;
    public static final long CLAIMINTERFACE = 0x8004550FL;
    public static final long RELEASEINTERFACE = 0x80045510L;
    public static final long SETINTERFACE = 0x80085504L;
    public static final long CLEAR_HALT = 0x80045515L;
}
