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
        var endpointIn = config.endpointLoopbackIn();
        var endpointOut = config.endpointLoopbackOut();
        haltEndpoint(UsbDirection.OUT, endpointOut);

        var data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        assertThrows(UsbStallException.class, () -> testDevice.transferOut(endpointOut, data));

        testDevice.clearHalt(UsbDirection.OUT, endpointOut);

        testDevice.transferOut(endpointOut, data);
        var receivedData = testDevice.transferIn(endpointIn);
        assertArrayEquals(data, receivedData);
    }

    @Test
    void stalledBulkTransferIn_recovers() {
        var endpointIn = config.endpointLoopbackIn();
        var endpointOut = config.endpointLoopbackOut();
        haltEndpoint(UsbDirection.IN, endpointIn);

        assertThrows(UsbStallException.class, () -> testDevice.transferIn(endpointIn));

        testDevice.clearHalt(UsbDirection.IN, endpointIn);

        var data = new byte[] { 9, 8, 7, 6, 5, 4, 3, 2 };
        testDevice.transferOut(endpointOut, data);
        var receivedData = testDevice.transferIn(endpointIn);
        assertArrayEquals(data, receivedData);
    }

    @Test
    void invalidControlTransfer_throws() {
        var request = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, (byte) 0x08,
                (short) 0, (short) config.interfaceNumber());
        assertThrows(UsbStallException.class, () -> testDevice.controlTransferIn(request, 2));
    }

    void haltEndpoint(UsbDirection direction, int endpointNumber) {
        final var SET_FEATURE = 0x03;
        final var ENDPOINT_HALT = 0x00;
        var endpointAddress = (direction == UsbDirection.IN ? 0x80 : 0x00) | endpointNumber;
        testDevice.controlTransferOut(new UsbControlTransfer(UsbRequestType.STANDARD, UsbRecipient.ENDPOINT, SET_FEATURE, ENDPOINT_HALT, endpointAddress), null);
    }
}
