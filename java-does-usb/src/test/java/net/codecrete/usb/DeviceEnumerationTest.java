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

import static org.assertj.core.api.Assertions.assertThat;

class DeviceEnumerationTest extends TestDeviceBase {

    @Test
    void getAllDevices_includesLoopback() {
        var deviceList = Usb.getDevices();
        assertThat(deviceList)
                .isNotEmpty()
                .anyMatch(device -> device.getVendorId() == config.vid() && device.getProductId() == config.pid());
    }

    @Test
    void getDevices_includesLoopback() {
        var deviceList = Usb.findDevices(device -> device.getVendorId() == config.vid() && device.getProductId() == config.pid());
        assertThat(deviceList)
                .isNotEmpty()
                .anyMatch(device -> device.getVendorId() == config.vid() && device.getProductId() == config.pid());
    }

    @Test
    void getDevicePredicate_returnsLoopback() {
        var device = Usb.findDevice(dev -> dev.getVendorId() == config.vid() && dev.getProductId() == config.pid());
        assertThat(device).isPresent();
        assertThat(device.get().getProductId()).isEqualTo(config.pid());
        assertThat(device.get().getVendorId()).isEqualTo(config.vid());
    }

    @Test
    void getDeviceVidPid_returnsLoopback() {
        var device = Usb.findDevice(config.vid(), config.pid());
        assertThat(device).isPresent();
        assertThat(device.get().getProductId()).isEqualTo(config.pid());
        assertThat(device.get().getVendorId()).isEqualTo(config.vid());
    }
}
