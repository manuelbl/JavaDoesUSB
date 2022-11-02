//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit tests for checking descriptive device information
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
        assertEquals(isLoopbackDevice() ? "Loopback" : "Composite", testDevice.product());
        assertEquals(12, testDevice.serialNumber().length());

        if (interfaceNumber == 2) {
            // composite device
            assertEquals(0xef, testDevice.classCode());
            assertEquals(0x02, testDevice.subclassCode());
            assertEquals(0x01, testDevice.protocolCode());
        } else {
            // simple device
            assertEquals(0xff, testDevice.classCode());
            assertEquals(0x00, testDevice.subclassCode());
            assertEquals(0x00, testDevice.protocolCode());
        }

        boolean isComposite = isCompositeDevce();

        assertEquals(2, testDevice.usbVersion().major());
        assertEquals(isComposite ? 1 : 0, testDevice.usbVersion().minor());
        assertEquals(0, testDevice.usbVersion().subminor());

        assertEquals(0, testDevice.deviceVersion().major());
        assertEquals(isComposite ? 3 : 7, testDevice.deviceVersion().minor());
        assertEquals(isComposite ? 6 : 4, testDevice.deviceVersion().subminor());
    }

    @Test
    void interfaceDescriptor_isCorrect() {
        assertNotNull(testDevice.interfaces());
        assertEquals(interfaceNumber + 1, testDevice.interfaces().size());

        var intf = testDevice.interfaces().get(interfaceNumber);
        assertEquals(interfaceNumber, intf.number());
        assertNotNull(intf.alternate());
        assertTrue(intf.isClaimed());
    }

    @Test
    void alternateInterfaceDescriptor_isCorrect() {
        var intf = testDevice.interfaces().get(interfaceNumber);
        var altIntf = intf.alternate();
        assertNotNull(intf.alternates());
        assertEquals(isLoopbackDevice() ? 2 : 1, intf.alternates().size());
        assertSame(intf.alternates().get(0), altIntf);
        assertEquals(0, altIntf.number());

        assertEquals(0xff, altIntf.classCode());
        assertEquals(0x00, altIntf.subclassCode());
        assertEquals(0x00, altIntf.protocolCode());

        if (isLoopbackDevice()) {
            altIntf = intf.alternates().get(1);
            assertEquals(1, altIntf.number());

            assertEquals(0xff, altIntf.classCode());
            assertEquals(0x00, altIntf.subclassCode());
            assertEquals(0x00, altIntf.protocolCode());
        }
    }

    @Test
    void endpointDescriptors_areCorrect() {
        var altIntf = testDevice.interfaces().get(interfaceNumber).alternate();
        assertNotNull(altIntf.endpoints());
        assertEquals(isLoopbackDevice() ? 4 : 2, altIntf.endpoints().size());

        var endpoint = altIntf.endpoints().get(0);
        assertEquals(1, endpoint.number());
        assertEquals(USBDirection.OUT, endpoint.direction());
        assertEquals(USBTransferType.BULK, endpoint.transferType());
        assertTrue(endpoint.packetSize() == 64 || endpoint.packetSize() == 512);

        endpoint = altIntf.endpoints().get(1);
        assertEquals(2, endpoint.number());
        assertEquals(USBDirection.IN, endpoint.direction());
        assertEquals(USBTransferType.BULK, endpoint.transferType());
        assertTrue(endpoint.packetSize() == 64 || endpoint.packetSize() == 512);

        if (isLoopbackDevice()) {
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

            // test alternate interface 1
            altIntf = testDevice.interfaces().get(interfaceNumber).alternates().get(1);
            assertEquals(2, altIntf.endpoints().size());

            endpoint = altIntf.endpoints().get(0);
            assertEquals(1, endpoint.number());
            assertEquals(USBDirection.OUT, endpoint.direction());
            assertEquals(USBTransferType.BULK, endpoint.transferType());
            assertTrue(endpoint.packetSize() == 64 || endpoint.packetSize() == 512);

            endpoint = altIntf.endpoints().get(1);
            assertEquals(2, endpoint.number());
            assertEquals(USBDirection.IN, endpoint.direction());
            assertEquals(USBTransferType.BULK, endpoint.transferType());
            assertTrue(endpoint.packetSize() == 64 || endpoint.packetSize() == 512);
        }
    }

    @Test
    void configurationDescription_isAvailable() {
        int expectedLength = isLoopbackDevice() ? 69 : 98;

        byte[] configDesc = testDevice.configurationDescriptor();
        assertNotNull(configDesc);
        assertEquals(expectedLength, configDesc.length);
        assertEquals(2, configDesc[1]);
        assertEquals((byte) expectedLength, configDesc[2]);
        assertEquals((byte) 0, configDesc[3]);
    }
}
