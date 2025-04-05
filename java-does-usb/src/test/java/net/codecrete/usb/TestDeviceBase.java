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

    protected static UsbDevice testDevice;
    protected static TestDeviceConfig config;

    static UsbDevice getDevice() {
        var device = Usb.findDevice(dev -> TestDeviceConfig.getConfig(dev).isPresent());
        if (device.isEmpty())
            throw new IllegalStateException("No test device connected");
        return device.get();
    }

    static TestDeviceConfig getDeviceConfig() {
        return TestDeviceConfig.getConfig(getDevice()).orElse(null);
    }

    @BeforeAll
    static void openDevice() {
        testDevice = getDevice();
        config = getDeviceConfig();

        testDevice.open();
        testDevice.claimInterface(config.interfaceNumber());

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
        return !config.isComposite();
    }

    static boolean isCompositeDevce() {
        return config.isComposite();
    }

    private static void resetDevice() {
        if (isLoopbackDevice())
            testDevice.selectAlternateSetting(config.interfaceNumber(), 0);

        // reset buffers
        resetBuffers();

        // drain loopback data
        drainData(config.endpointLoopbackIn());

        // drain interrupt data
        if (isLoopbackDevice())
            drainData(config.endpointEchoIn());

        // reset buffers again
        resetBuffers();
    }

    static void resetBuffers() {
        testDevice.controlTransferOut(new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE,
                (byte) 0x04, (short) 0, (short) config.interfaceNumber()), null);
    }

    static void drainData(int endpointNumber) {
        while (true) {
            try {
                testDevice.transferIn(endpointNumber, 5);
            } catch (UsbTimeoutException _) {
                break;
            }
        }
    }

    static byte[] generateRandomBytes(int numBytes, long seed) {
        var random = new Random(seed);
        var bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }
}
