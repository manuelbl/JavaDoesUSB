//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for interrupt transfers
//

package net.codecrete.usb;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class InterruptTransferTest extends TestDeviceBase {

    @Test
    void smallTransfer_succeeds() {
        Assumptions.assumeTrue(isLoopbackDevice(),
                "Interrupt transfer only supported by loopback test device");

        var sampleData = generateRandomBytes(12, 293872394);
        testDevice.transferOut(config.endpointEchoOut(), sampleData);

        // receive first echo
        var echo = testDevice.transferIn(config.endpointEchoIn());
        assertArrayEquals(sampleData, echo);

        // receive second echo
        echo = testDevice.transferIn(config.endpointEchoIn());
        assertArrayEquals(sampleData, echo);
    }
}
