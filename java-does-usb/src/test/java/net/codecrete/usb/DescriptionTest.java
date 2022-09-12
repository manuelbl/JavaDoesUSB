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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the interface, alternate settings and endpoint descriptions.
 */
public class DescriptionTest extends TestDeviceBase {

    @Test
    void deviceInfo_isCorrect() {
        assertEquals("JavaDoesUSB", testDevice.manufacturer());
        assertEquals("Loopback", testDevice.product());
        assertEquals(12, testDevice.serialNumber().length());

        assertEquals(0xef, testDevice.classCode());
        assertEquals(0x02, testDevice.subclassCode());
        assertEquals(0x01, testDevice.protocolCode());
    }

    @Test
    void interfaceDescriptors_isCorrect() {
        assertNotNull(testDevice.interfaces());
        assertEquals(LOOPBACK_INTF + 1, testDevice.interfaces().size());

        var intf = testDevice.interfaces().get(LOOPBACK_INTF);
        assertEquals(LOOPBACK_INTF, intf.number());
        assertNotNull(intf.alternate());
        assertTrue(intf.isClaimed());
    }

    @Test
    void alternateInterfaceDescriptors_isCorrect() {
        var intf = testDevice.interfaces().get(LOOPBACK_INTF);
        var altIntf = intf.alternate();
        assertNotNull(intf.alternates());
        assertEquals(1, intf.alternates().size());
        assertSame(intf.alternates().get(0), altIntf);
        assertEquals(0, altIntf.number());

        assertEquals(0xff, altIntf.classCode());
        assertEquals(0x00, altIntf.subclassCode());
        assertEquals(0x00, altIntf.protocolCode());
    }

    @Test
    void endpointDescriptors_isCorrect() {
        var altIntf = testDevice.interfaces().get(LOOPBACK_INTF).alternate();
        assertNotNull(altIntf.endpoints());
        assertEquals(4, altIntf.endpoints().size());

        var endpoint = altIntf.endpoints().get(0);
        assertEquals(1, endpoint.number());
        assertEquals(USBDirection.OUT, endpoint.direction());
        assertEquals(USBTransferType.BULK, endpoint.transferType());
        assertEquals(64, endpoint.packetSize());

        endpoint = altIntf.endpoints().get(1);
        assertEquals(2, endpoint.number());
        assertEquals(USBDirection.IN, endpoint.direction());
        assertEquals(USBTransferType.BULK, endpoint.transferType());
        assertEquals(64, endpoint.packetSize());

        endpoint = altIntf.endpoints().get(2);
        assertEquals(3, endpoint.number());
        assertEquals(USBDirection.OUT, endpoint.direction());
        assertEquals(USBTransferType.INTERRUPT, endpoint.transferType());
        assertEquals(16, endpoint.packetSize());

        endpoint = altIntf.endpoints().get(3);
        assertEquals(3, endpoint.number());
        assertEquals(USBDirection.IN, endpoint.direction());
        assertEquals(USBTransferType.INTERRUPT, endpoint.transferType());
        assertEquals(16, endpoint.packetSize());
    }
}
