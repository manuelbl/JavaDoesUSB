//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for data overruns
//

package net.codecrete.usb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class StallTest extends TestDeviceBase {

    @Test
    void stallOnBulkTransfer_throws() {
        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        testDevice.transferOut(LOOPBACK_EP_OUT, data);
        Assertions.assertThrows(USBException.class, () -> {
            testDevice.transferIn(LOOPBACK_EP_IN, 5);
        });

        testDevice.clearHalt(USBDirection.IN, LOOPBACK_EP_IN);

        var receivedData = testDevice.transferIn(LOOPBACK_EP_IN, 20);
        Assertions.assertArrayEquals(data, receivedData);
    }

    @Test
    void stallOnInterruptTransfer_throws() {
        Assumptions.assumeTrue(isLoopbackDevice(),
                "Interrupt transfer only supported by loopback test device");

        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        testDevice.transferOut(ECHO_EP_OUT, data);
        Assertions.assertThrows(USBException.class, () -> {
            testDevice.transferIn(ECHO_EP_IN, 4);
        });

        testDevice.clearHalt(USBDirection.IN, ECHO_EP_IN);

        // first echo
        var receivedData = testDevice.transferIn(ECHO_EP_IN, 20);
        Assertions.assertArrayEquals(data, receivedData);

        // second echo
        receivedData = testDevice.transferIn(ECHO_EP_IN, 20);
        Assertions.assertArrayEquals(data, receivedData);
    }

    @Test
    void invalidControlTransfer_throws() {
        Assertions.assertThrows(USBException.class, () -> {
            testDevice.controlTransferIn(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x08, (short) 0, (short) interfaceNumber), 2);
        });
    }
}
