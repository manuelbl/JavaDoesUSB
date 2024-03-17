//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for changing alternate interface setting
//

package net.codecrete.usb;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlternateInterfaceTest extends TestDeviceBase {

    @BeforeAll
    static void precondition() {
        Assumptions.assumeTrue(isLoopbackDevice(),
                "Alternate interface only supported by loopback test device");
    }

    @Test
    void selectAlternateIntf_succeeds() {

        testDevice.selectAlternateSetting(config.interfaceNumber(), 1);

        var altIntf = testDevice.getInterface(config.interfaceNumber()).getCurrentAlternate();
        assertNotNull(altIntf);
        assertEquals(2, altIntf.getEndpoints().size());
        assertEquals(0xff, altIntf.getClassCode());

        testDevice.selectAlternateSetting(config.interfaceNumber(), 0);
    }

    @Test
    void selectInvalidAlternateIntf_fails() {
        assertThrows(UsbException.class, () -> testDevice.selectAlternateSetting(1, 0));

        var interface_number = config.interfaceNumber();
        assertThrows(UsbException.class, () -> testDevice.selectAlternateSetting(interface_number, 2));
    }

    @Test
    void transferOnValidEndpoint_succeeds() {
        testDevice.selectAlternateSetting(config.interfaceNumber(), 1);

        var sampleData = generateRandomBytes(12, 293872394);
        testDevice.transferOut(config.endpointLoopbackOut(), sampleData);
        var received = testDevice.transferIn(config.endpointLoopbackIn());
        assertArrayEquals(sampleData, received);
    }

    @Test
    void transferOnInvalidEndpoint_fails() {
        testDevice.selectAlternateSetting(config.interfaceNumber(), 1);

        var endpointOut = config.endpointEchoOut();
        assertThrows(UsbException.class, () -> testDevice.transferOut(endpointOut, new byte[] { 1, 2, 3 }));

        var endpointIn = config.endpointEchoIn();
        assertThrows(UsbException.class, () -> testDevice.transferIn(endpointIn));
    }
}
