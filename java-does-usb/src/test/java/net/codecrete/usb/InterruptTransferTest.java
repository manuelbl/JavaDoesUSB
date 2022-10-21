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

public class InterruptTransferTest extends TestDeviceBase {

    @Test
    void smallTransfer_succeeds() {
        Assumptions.assumeTrue(isLoopbackDevice(),
                "Interrupt transfer only supported by loopback test device");

        byte[] sampleData = generateRandomBytes(12, 293872394);
        testDevice.transferOut(ECHO_EP_OUT, sampleData);

        // receive first echo
        byte[] echo = testDevice.transferIn(ECHO_EP_IN, ECHO_MAX_PACKET_SIZE);
        assertArrayEquals(sampleData, echo);

        // receive second echo
        echo = testDevice.transferIn(ECHO_EP_IN, ECHO_MAX_PACKET_SIZE);
        assertArrayEquals(sampleData, echo);
    }
}
