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
     * Simple test device vendor ID
     */
    static final int VID_SIMPLE = 0xcafe;
    /**
     * Simple test device product ID
     */
    static final int PID_SIMPLE = 0xceaf;
    /**
     * Simple test device loopback interface number
     */
    static final int LOOPBACK_INTF_SIMPLE = 0;
    /**
     * Composite test device vendor ID
     */
    static final int VID_COMPOSITE = 0xcafe;
    /**
     * Composite test device product ID
     */
    static final int PID_COMPOSITE = 0xcea0;
    /**
     * Composite test device loopback interface number
     */
    static final int LOOPBACK_INTF_COMPOSITE = 2;
    /**
     * Interface number of connected test device
     */
    protected static int vid = -1;
    protected static int pid = -1;
    protected static int interfaceNumber = -1;
    protected static final int LOOPBACK_EP_OUT = 1;
    protected static final int LOOPBACK_EP_IN = 2;
    protected static final int LOOPBACK_MAX_PACKET_SIZE = 64;
    protected static final int ECHO_EP_OUT = 3;
    protected static final int ECHO_EP_IN = 3;
    protected static final int ECHO_MAX_PACKET_SIZE = 16;
    protected static USBDevice testDevice;

    static USBDevice getDevice() {
        var device = USB.getDevice(new USBDeviceFilter(VID_COMPOSITE, PID_COMPOSITE));
        if (device == null)
            device = USB.getDevice(new USBDeviceFilter(VID_SIMPLE, PID_SIMPLE));
        if (device == null)
            throw new IllegalStateException("No test device connected");
        return device;
    }

    static int getInterfaceNumber(USBDevice device) {
        return device.productId() == PID_COMPOSITE ? LOOPBACK_INTF_COMPOSITE : LOOPBACK_INTF_SIMPLE;
    }

    @BeforeAll
    static void openDevice() {
        testDevice = getDevice();
        vid = testDevice.vendorId();
        pid = testDevice.productId();
        interfaceNumber = getInterfaceNumber(testDevice);

        testDevice.open();
        testDevice.claimInterface(interfaceNumber);
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
