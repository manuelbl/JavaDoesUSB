//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit tests for USB device enumeration
//

package net.codecrete.usb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeviceEnumerationTest {

    @BeforeAll
    static void openDevice() {
        var deviceInfo = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        if (deviceInfo == null)
            throw new IllegalStateException("USB loopback test device must be connected");
    }

    @Test
    void getAllDevices_includesLoopback() {
        var found = USB.getAllDevices().stream()
                .anyMatch(device -> device.getVendorId() == 0xcafe && device.getProductId() == 0xceaf);
        assertTrue(found);
    }

    @Test
    void getDevicesWithFilter_returnsLoopback() {
        var result = USB.getDevices(new USBDeviceFilter(0xcafe, 0xceaf));
        assertEquals(1, result.size());
        assertEquals(0xcafe, result.get(0).getVendorId());
        assertEquals(0xceaf, result.get(0).getProductId());
    }

    @Test
    void getDevicesWithMultipleFilters_returnsLoopback() {
        var result = USB.getDevices(List.of(
                new USBDeviceFilter(0xcafe, 0xceaf),
                new USBDeviceFilter(0x0000, 0xffff)
        ));
        assertEquals(1, result.size());
        assertEquals(0xcafe, result.get(0).getVendorId());
        assertEquals(0xceaf, result.get(0).getProductId());
    }

    @Test
    void getDeviceWithFilter_returnsLoopback() {
        var device = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        assertEquals(0xcafe, device.getVendorId());
        assertEquals(0xceaf, device.getProductId());
    }

    @Test
    void getDeviceWithMultipleFilters_returnsLoopback() {
        var device = USB.getDevice(List.of(
                new USBDeviceFilter(0xcafe, 0xceaf),
                new USBDeviceFilter(0x0000, 0xffff)
        ));
        assertEquals(0xcafe, device.getVendorId());
        assertEquals(0xceaf, device.getProductId());
    }
}
