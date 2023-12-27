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
class DescriptionTest extends TestDeviceBase {

    @Test
    void deviceInfo_isCorrect() {
        assertEquals("JavaDoesUSB", testDevice.getManufacturer());
        assertEquals(isLoopbackDevice() ? "Loopback" : "Composite", testDevice.getProduct());
        assertEquals(12, testDevice.getSerialNumber().length());

        if (isLoopbackDevice()) {
            // simple device
            assertEquals(0xff, testDevice.getClassCode());
            assertEquals(0x00, testDevice.getSubclassCode());
            assertEquals(0x00, testDevice.getProtocolCode());
        } else {
            // composite device
            assertEquals(0xef, testDevice.getClassCode());
            assertEquals(0x02, testDevice.getSubclassCode());
            assertEquals(0x01, testDevice.getProtocolCode());
        }

        var isComposite = isCompositeDevce();

        assertEquals(2, testDevice.getUsbVersion().getMajor());
        assertEquals(isComposite ? 1 : 0, testDevice.getUsbVersion().getMinor());
        assertEquals(0, testDevice.getUsbVersion().getSubminor());

        assertEquals(0, testDevice.getDeviceVersion().getMajor());
        assertEquals(isComposite ? 3 : 7, testDevice.getDeviceVersion().getMinor());
        assertEquals(isComposite ? 6 : 4, testDevice.getDeviceVersion().getSubminor());
    }

    @Test
    void interfaceDescriptor_isCorrect() {
        assertNotNull(testDevice.getInterfaces());
        assertEquals(interfaceNumber + 1, testDevice.getInterfaces().size());

        var intf = testDevice.getInterfaces().get(interfaceNumber);
        assertEquals(interfaceNumber, intf.getNumber());
        assertNotNull(intf.getCurrentAlternate());
        assertTrue(intf.isClaimed());
    }

    @Test
    void invalidInterfaceNumber_shouldThrow() {
        assertThrows(UsbException.class, () -> testDevice.getInterface(4));
    }

    @Test
    void alternateInterfaceDescriptor_isCorrect() {
        var intf = testDevice.getInterfaces().get(interfaceNumber);
        var altIntf = intf.getCurrentAlternate();
        assertNotNull(intf.getAlternates());
        assertEquals(isLoopbackDevice() ? 2 : 1, intf.getAlternates().size());
        assertSame(intf.getAlternates().get(0), altIntf);
        assertEquals(0, altIntf.getNumber());

        assertEquals(0xff, altIntf.getClassCode());
        assertEquals(0x00, altIntf.getSubclassCode());
        assertEquals(0x00, altIntf.getProtocolCode());

        if (isLoopbackDevice()) {
            altIntf = intf.getAlternates().get(1);
            assertEquals(1, altIntf.getNumber());

            assertEquals(0xff, altIntf.getClassCode());
            assertEquals(0x00, altIntf.getSubclassCode());
            assertEquals(0x00, altIntf.getProtocolCode());
        }
    }

    @Test
    void endpointDescriptors_areCorrect() {
        var altIntf = testDevice.getInterfaces().get(interfaceNumber).getCurrentAlternate();
        assertNotNull(altIntf.getEndpoints());
        assertEquals(isLoopbackDevice() ? 4 : 2, altIntf.getEndpoints().size());

        var endpoint = altIntf.getEndpoints().get(0);
        assertEquals(1, endpoint.getNumber());
        assertEquals(UsbDirection.OUT, endpoint.getDirection());
        assertEquals(UsbTransferType.BULK, endpoint.getTransferType());
        assertTrue(endpoint.getPacketSize() == 64 || endpoint.getPacketSize() == 512);

        endpoint = altIntf.getEndpoints().get(1);
        assertEquals(2, endpoint.getNumber());
        assertEquals(UsbDirection.IN, endpoint.getDirection());
        assertEquals(UsbTransferType.BULK, endpoint.getTransferType());
        assertTrue(endpoint.getPacketSize() == 64 || endpoint.getPacketSize() == 512);

        if (isLoopbackDevice()) {
            endpoint = altIntf.getEndpoints().get(2);
            assertEquals(3, endpoint.getNumber());
            assertEquals(UsbDirection.OUT, endpoint.getDirection());
            assertEquals(UsbTransferType.INTERRUPT, endpoint.getTransferType());
            assertEquals(16, endpoint.getPacketSize());

            endpoint = altIntf.getEndpoints().get(3);
            assertEquals(3, endpoint.getNumber());
            assertEquals(UsbDirection.IN, endpoint.getDirection());
            assertEquals(UsbTransferType.INTERRUPT, endpoint.getTransferType());
            assertEquals(16, endpoint.getPacketSize());

            // test alternate interface 1
            altIntf = testDevice.getInterfaces().get(interfaceNumber).getAlternates().get(1);
            assertEquals(2, altIntf.getEndpoints().size());

            endpoint = altIntf.getEndpoints().get(0);
            assertEquals(1, endpoint.getNumber());
            assertEquals(UsbDirection.OUT, endpoint.getDirection());
            assertEquals(UsbTransferType.BULK, endpoint.getTransferType());
            assertTrue(endpoint.getPacketSize() == 64 || endpoint.getPacketSize() == 512);

            endpoint = altIntf.getEndpoints().get(1);
            assertEquals(2, endpoint.getNumber());
            assertEquals(UsbDirection.IN, endpoint.getDirection());
            assertEquals(UsbTransferType.BULK, endpoint.getTransferType());
            assertTrue(endpoint.getPacketSize() == 64 || endpoint.getPacketSize() == 512);
        }
    }

    @Test
    void invalidEndpoint_shouldThrow() {
        assertThrows(UsbException.class, () -> testDevice.getEndpoint(UsbDirection.IN, 1));
        assertThrows(UsbException.class, () -> testDevice.getEndpoint(UsbDirection.OUT, 4));
        assertThrows(UsbException.class, () -> testDevice.getEndpoint(UsbDirection.IN, 0));
        assertThrows(UsbException.class, () -> testDevice.getEndpoint(UsbDirection.OUT, 0));
    }

    @Test
    void configurationDescription_isAvailable() {
        var expectedLength = isLoopbackDevice() ? 69 : 115;

        var configDesc = testDevice.getConfigurationDescriptor();
        assertNotNull(configDesc);
        assertEquals(expectedLength, configDesc.length);
        assertEquals(2, configDesc[1]);
        assertEquals((byte) expectedLength, configDesc[2]);
        assertEquals((byte) 0, configDesc[3]);
    }
}
