//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for isSame()
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IsSameDeviceTest {

    @Test
    void getAllDevices_includesTestDevice() throws Exception {
        var testDeviceInfo = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        try (var testDevice = testDeviceInfo.open()) {
            var deviceList = USB.getAllDevices();

            long count = deviceList.stream().filter(testDevice::isSameDevice).count();
            assertEquals(1, count);

            count = deviceList.stream().filter((info) -> info.isSameDevice(testDevice)).count();
            assertEquals(1, count);

            count = deviceList.stream().filter(testDeviceInfo::isSameDevice).count();
            assertEquals(1, count);

            count = deviceList.stream().filter((info) -> info.isSameDevice(testDeviceInfo)).count();
            assertEquals(1, count);
        }
    }

    @Test
    void openDevice_isSameAsDeviceInfo() throws Exception {
        var testDeviceInfo = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        try (var testDevice = testDeviceInfo.open()) {
            assertTrue(testDevice.isSameDevice(testDeviceInfo));
            assertTrue(testDeviceInfo.isSameDevice(testDevice));
        }
    }
}
