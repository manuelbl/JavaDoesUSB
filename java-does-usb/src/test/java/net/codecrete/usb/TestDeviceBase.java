//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit tests for USB device enumeration
//

package net.codecrete.usb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for tests using the test device.
 */
public class TestDeviceBase {
    protected static USBDevice device;

    @BeforeAll
    static void openDevice() {
        device = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        if (device == null)
            throw new IllegalStateException("USB loopback test device must be connected");
        device.open();
        device.claimInterface(0);
    }

    @AfterAll
    static void closeDevice() {
        if (device != null) {
            device.close();
            device = null;
        }
    }
}
