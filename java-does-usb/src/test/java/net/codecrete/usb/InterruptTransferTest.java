//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for bulk transfer
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class InterruptTransferTest extends TestDeviceBase {

    private static final int ECHO_EP_OUT = 3;
    private static final int ECHO_EP_IN = 3;
    private static final int MAX_PACKET_SIZE = 16;

    @Test
    void smallTransfer_succeeds() {
        byte[] sampleData = generateRandomBytes(12, 293872394);
        testDevice.transferOut(ECHO_EP_OUT, sampleData);

        // receive first echo
        byte[] echo = testDevice.transferIn(ECHO_EP_IN, MAX_PACKET_SIZE);
        assertArrayEquals(sampleData, echo);

        // receive second echo
        echo = testDevice.transferIn(ECHO_EP_IN, MAX_PACKET_SIZE);
        assertArrayEquals(sampleData, echo);
    }
}
