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

import java.util.Random;

/**
 * Base class for tests using the test device.
 */
public class TestDeviceBase {
    protected static USBDevice testDevice;

    @BeforeAll
    static void openDevice() {
        testDevice = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        if (testDevice == null)
            throw new IllegalStateException("USB loopback test device must be connected");
        testDevice.open();
        testDevice.claimInterface(0);
    }

    @AfterAll
    static void closeDevice() {
        if (testDevice != null) {
            testDevice.close();
            testDevice = null;
        }
    }

    static byte[] generateRandomBytes(int numBytes, long seed) {
        var random = new Random(seed);
        var bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }
}
