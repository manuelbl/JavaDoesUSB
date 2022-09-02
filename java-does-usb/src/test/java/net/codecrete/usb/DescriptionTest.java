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
    void interfaceDescriptors_present() {
        assertNotNull(device.getInterfaces());
        assertEquals(1, device.getInterfaces().size());

        var intf = device.getInterfaces().get(0);
        assertEquals(0, intf.getNumber());
        assertNotNull(intf.getAlternate());
        assertFalse(intf.isClaimed());
    }

    @Test
    void alternateInterfaceDescriptors_present() {
        var intf = device.getInterfaces().get(0);
        var altIntf = intf.getAlternate();
        assertNotNull(intf.getAlternates());
        assertEquals(1, intf.getAlternates().size());
        assertSame(intf.getAlternates().get(0), altIntf);
        assertEquals(0, altIntf.getNumber());
    }

    @Test
    void endpointDescriptors_present() {
        var altIntf = device.getInterfaces().get(0).getAlternate();
        assertNotNull(altIntf.getEndpoints());
        assertEquals(2, altIntf.getEndpoints().size());

        var endpoint = altIntf.getEndpoints().get(0);
        assertEquals(1, endpoint.getNumber());
        assertEquals(USBDirection.OUT, endpoint.getDirection());
        assertEquals(USBEndpointType.BULK, endpoint.getType());
        assertEquals(64, endpoint.getPacketSize());

        endpoint = altIntf.getEndpoints().get(1);
        assertEquals(2, endpoint.getNumber());
        assertEquals(USBDirection.IN, endpoint.getDirection());
        assertEquals(USBEndpointType.BULK, endpoint.getType());
        assertEquals(64, endpoint.getPacketSize());
    }
}
