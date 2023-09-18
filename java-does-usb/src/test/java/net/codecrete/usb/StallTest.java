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

class StallTest extends TestDeviceBase {

    @Test
    void stalledBulkTransferOut_recovers() {
        haltEndpoint(USBDirection.OUT, LOOPBACK_EP_OUT);

        var data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        assertThrows(USBStallException.class, () -> testDevice.transferOut(LOOPBACK_EP_OUT, data));

        testDevice.clearHalt(USBDirection.OUT, LOOPBACK_EP_OUT);

        testDevice.transferOut(LOOPBACK_EP_OUT, data);
        var receivedData = testDevice.transferIn(LOOPBACK_EP_IN);
        assertArrayEquals(data, receivedData);
    }

    @Test
    void stalledBulkTransferIn_recovers() {
        haltEndpoint(USBDirection.IN, LOOPBACK_EP_IN);

        assertThrows(USBStallException.class, () -> testDevice.transferIn(LOOPBACK_EP_IN));

        testDevice.clearHalt(USBDirection.IN, LOOPBACK_EP_IN);

        var data = new byte[] { 9, 8, 7, 6, 5, 4, 3, 2 };
        testDevice.transferOut(LOOPBACK_EP_OUT, data);
        var receivedData = testDevice.transferIn(LOOPBACK_EP_IN);
        assertArrayEquals(data, receivedData);
    }

    @Test
    void invalidControlTransfer_throws() {
        var request = new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x08,
                (short) 0, (short) interfaceNumber);
        assertThrows(USBStallException.class, () -> testDevice.controlTransferIn(request, 2));
    }

    void haltEndpoint(USBDirection direction, int endpointNumber) {
        final var SET_FEATURE = 0x03;
        final var ENDPOINT_HALT = 0x00;
        var endpointAddress = (direction == USBDirection.IN ? 0x80 : 0x00) | endpointNumber;
        testDevice.controlTransferOut(new USBControlTransfer(USBRequestType.STANDARD, USBRecipient.ENDPOINT, SET_FEATURE, ENDPOINT_HALT, endpointAddress), null);
    }
}
