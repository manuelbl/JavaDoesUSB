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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeviceEnumerationTest extends TestDeviceBase {

    @Test
    void getAllDevices_includesLoopback() {
        var found = USB.getAllDevices().stream()
                .anyMatch(device -> device.vendorId() == 0xcafe && device.productId() == 0xceaf);
        assertTrue(found);
    }

    @Test
    void getDevicesWithFilter_returnsLoopback() {
        var result = USB.getDevices(new USBDeviceFilter(0xcafe, 0xceaf));
        assertEquals(1, result.size());
        assertEquals(0xcafe, result.get(0).vendorId());
        assertEquals(0xceaf, result.get(0).productId());
    }

    @Test
    void getDevicesWithMultipleFilters_returnsLoopback() {
        var result = USB.getDevices(List.of(
                new USBDeviceFilter(0xcafe, 0xceaf),
                new USBDeviceFilter(0x0000, 0xffff)
        ));
        assertEquals(1, result.size());
        assertEquals(0xcafe, result.get(0).vendorId());
        assertEquals(0xceaf, result.get(0).productId());
    }

    @Test
    void getDeviceWithFilter_returnsLoopback() {
        var device = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        assertEquals(0xcafe, device.vendorId());
        assertEquals(0xceaf, device.productId());
    }

    @Test
    void getDeviceWithMultipleFilters_returnsLoopback() {
        var device = USB.getDevice(List.of(
                new USBDeviceFilter(0xcafe, 0xceaf),
                new USBDeviceFilter(0x0000, 0xffff)
        ));
        assertEquals(0xcafe, device.vendorId());
        assertEquals(0xceaf, device.productId());
    }
}
