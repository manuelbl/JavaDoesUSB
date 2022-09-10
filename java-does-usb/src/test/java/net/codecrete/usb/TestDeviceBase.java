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
    /**
     * Test device vendor ID
     */
    static final int VID = 0xcafe;
    /**
     * Test device product ID
     */
    static final int PID = 0xceaf;
    /**
     * Test device loopback interface number
     */
    static final int LOOPBACK_INTF = 0;
    protected static final int LOOPBACK_EP_OUT = 1;
    protected static final int LOOPBACK_EP_IN = 2;
    protected static final int LOOPBACK_MAX_PACKET_SIZE = 64;
    protected static final int ECHO_EP_OUT = 3;
    protected static final int ECHO_EP_IN = 3;
    protected static final int ECHO_MAX_PACKET_SIZE = 16;
    protected static USBDevice testDevice;

    @BeforeAll
    static void openDevice() {
        testDevice = USB.getDevice(new USBDeviceFilter(VID, PID));
        if (testDevice == null)
            throw new IllegalStateException("USB loopback test device must be connected");
        testDevice.open();
        testDevice.claimInterface(LOOPBACK_INTF);
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
