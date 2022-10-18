//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for bulk transfer
//

package net.codecrete.usb;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AlternateInterfaceTest extends TestDeviceBase {

    @BeforeAll
    static void precondition() {
        Assumptions.assumeTrue(isLoopbackDevice(),
                "Alternate interface only supported by loopback test device");
    }

    @Test
    void selectAlternateIntf_succeeds() {

        testDevice.selectAlternateSetting(LOOPBACK_INTF_LOOPBACK, 1);

        var altIntf = testDevice.getInterface(LOOPBACK_INTF_LOOPBACK).alternate();
        assertNotNull(altIntf);
        assertEquals(2, altIntf.endpoints().size());
        assertEquals(0xff, altIntf.classCode());

        testDevice.selectAlternateSetting(LOOPBACK_INTF_LOOPBACK, 0);
    }

    @Test
    void selectInvalidAlternateIntf_fails() {
        assertThrows(USBException.class, () -> {
            testDevice.selectAlternateSetting(1, 0);
        });

        assertThrows(USBException.class, () -> {
            testDevice.selectAlternateSetting(LOOPBACK_INTF_LOOPBACK, 2);
        });
    }

    @Test
    void transferOnValidEndpoint_succeeds() {
        testDevice.selectAlternateSetting(LOOPBACK_INTF_LOOPBACK, 1);

        byte[] sampleData = generateRandomBytes(12, 293872394);
        testDevice.transferOut(LOOPBACK_EP_OUT, sampleData);
        var received = testDevice.transferIn(LOOPBACK_EP_IN, 64);
        assertArrayEquals(sampleData, received);
    }

    @Test
    void transferOnInvalidEndpoint_fails() {
        testDevice.selectAlternateSetting(LOOPBACK_INTF_LOOPBACK, 1);

        assertThrows(USBException.class, () -> {
            testDevice.transferOut(ECHO_EP_OUT, new byte[] { 1, 2, 3 });
        });

        assertThrows(USBException.class, () -> {
            testDevice.transferIn(ECHO_EP_IN, 16);
        });
    }
}
