//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for data overruns
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StallTest extends TestDeviceBase {

    @Test
    void stalledBulkTransferOut_recovers() {
        haltEndpoint(USBDirection.OUT, LOOPBACK_EP_OUT);

        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        assertThrows(USBException.class, () -> {
            testDevice.transferOut(LOOPBACK_EP_OUT, data);
        });

        testDevice.clearHalt(USBDirection.OUT, LOOPBACK_EP_OUT);

        testDevice.transferOut(LOOPBACK_EP_OUT, data);
        var receivedData = testDevice.transferIn(LOOPBACK_EP_IN, 20);
        assertArrayEquals(data, receivedData);
    }

    @Test
    void stalledBulkTransferIn_recovers() {
        haltEndpoint(USBDirection.IN, LOOPBACK_EP_IN);

        assertThrows(USBException.class, () -> {
            testDevice.transferIn(LOOPBACK_EP_IN, LOOPBACK_MAX_PACKET_SIZE);
        });

        testDevice.clearHalt(USBDirection.IN, LOOPBACK_EP_IN);

        byte[] data = new byte[] { 9, 8, 7, 6, 5, 4, 3, 2 };
        testDevice.transferOut(LOOPBACK_EP_OUT, data);
        var receivedData = testDevice.transferIn(LOOPBACK_EP_IN, 20);
        assertArrayEquals(data, receivedData);
    }

    @Test
    void invalidControlTransfer_throws() {
        assertThrows(USBException.class, () -> {
            testDevice.controlTransferIn(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x08, (short) 0, (short) interfaceNumber), 2);
        });
    }

    void haltEndpoint(USBDirection direction, int endpointNumber) {
        final int SET_FEATURE = 0x03;
        final int ENDPOINT_HALT = 0x00;
        int endpointAddress = (direction == USBDirection.IN ? 0x80 : 0x00) | endpointNumber;
        testDevice.controlTransferOut(new USBControlTransfer(USBRequestType.STANDARD, USBRecipient.ENDPOINT, SET_FEATURE, ENDPOINT_HALT, endpointAddress), null);
    }
}
