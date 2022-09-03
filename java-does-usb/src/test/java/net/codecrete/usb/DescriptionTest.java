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

        assertEquals(0xff, testDevice.classCode());
        assertEquals(0x00, testDevice.subclassCode());
        assertEquals(0x00, testDevice.protocolCode());
    }

    @Test
    void interfaceDescriptors_isCorrect() {
        assertNotNull(testDevice.interfaces());
        assertEquals(1, testDevice.interfaces().size());

        var intf = testDevice.interfaces().get(0);
        assertEquals(0, intf.number());
        assertNotNull(intf.alternate());
        assertFalse(intf.isClaimed());
    }

    @Test
    void alternateInterfaceDescriptors_isCorrect() {
        var intf = testDevice.interfaces().get(0);
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
        var altIntf = testDevice.interfaces().get(0).alternate();
        assertNotNull(altIntf.endpoints());
        assertEquals(2, altIntf.endpoints().size());

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
    }
}
