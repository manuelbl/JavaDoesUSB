//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit tests for USB device enumeration
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

public class TimeoutTest extends TestDeviceBase {

    @Test
    @Timeout(1)
    void bulkTransferIn_timesOut() {
        assertThrows(TimeoutException.class, () -> {
            testDevice.transferIn(LOOPBACK_EP_IN, 64, 200);
        });
    }

    @Test
    @Timeout(1)
    void bulkTransferOut_timesOut() {
        // The test device has an internal buffer of about 500 bytes
        assertThrows(TimeoutException.class, () -> {
            byte[] data = generateRandomBytes(100, 9383073929L);
            for (int i = 0; i < 12; i++) {
                testDevice.transferOut(LOOPBACK_EP_OUT, data, 200);
            }
        });

        // drain data in loopback loop
        while (true) {
            try {
                testDevice.transferIn(LOOPBACK_EP_IN, 64, 200);
            } catch (TimeoutException e) {
                break;
            }
        }
    }
}
