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

    public static final long CONTROL = 0xc0185500;
    public static final long BULK = 0xc0185502;
    public static final long CLAIMINTERFACE = 0x8004550F;
    public static final long RELEASEINTERFACE = 0x80045510;
}
