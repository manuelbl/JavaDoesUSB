//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Base class for unit tests using the connected test device
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
     * Loopback test device vendor ID
     */
    static final int VID_LOOPBACK = 0xcafe;
    /**
     * Loopback test device product ID
     */
    static final int PID_LOOPBACK = 0xceaf;
    /**
     * Loopback test device loopback interface number
     */
    static final int LOOPBACK_INTF_LOOPBACK = 0;
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
    static final int LOOPBACK_INTF_COMPOSITE = 3;
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
        var optionalDevice = USB.getDevice(VID_COMPOSITE, PID_COMPOSITE);
        if (optionalDevice.isEmpty())
            optionalDevice = USB.getDevice(VID_LOOPBACK, PID_LOOPBACK);
        if (optionalDevice.isEmpty())
            throw new IllegalStateException("No test device connected");
        return optionalDevice.get();
    }

    static int getInterfaceNumber(USBDevice device) {
        return device.productId() == PID_COMPOSITE ? LOOPBACK_INTF_COMPOSITE : LOOPBACK_INTF_LOOPBACK;
    }

    @BeforeAll
    static void openDevice() {
        testDevice = getDevice();
        vid = testDevice.vendorId();
        pid = testDevice.productId();
        interfaceNumber = getInterfaceNumber(testDevice);

        testDevice.open();
        testDevice.claimInterface(interfaceNumber);

        resetDevice();
    }

    @AfterAll
    static void closeDevice() {
        if (testDevice != null) {
            testDevice.close();
            testDevice = null;
        }
    }

    static boolean isLoopbackDevice() {
        return pid == PID_LOOPBACK;
    }

    static boolean isCompositeDevce() {
        return pid == PID_COMPOSITE;
    }

    private static void resetDevice() {
        if (isLoopbackDevice())
            testDevice.selectAlternateSetting(LOOPBACK_INTF_LOOPBACK, 0);

        // reset buffers
        resetBuffers();

        // drain loopback data
        while (true) {
            try {
                testDevice.transferIn(LOOPBACK_EP_IN, 1);
            } catch (USBTimeoutException e) {
                break;
            }
        }

        // drain interrupt data
        if (isLoopbackDevice()) {
            while (true) {
                try {
                    testDevice.transferIn(ECHO_EP_IN, 1);
                } catch (USBTimeoutException e) {
                    break;
                }
            }
        }

        // reset buffers again
        resetBuffers();
    }

    static void resetBuffers() {
        testDevice.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE,
                (byte) 0x04, (short) 0, (short) interfaceNumber), null);
    }

    static byte[] generateRandomBytes(int numBytes, long seed) {
        var random = new Random(seed);
        var bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }
}
