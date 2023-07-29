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
        var deviceList = USB.getAllDevices();
        assertThat(deviceList)
                .isNotEmpty()
                .anyMatch(device -> device.vendorId() == vid && device.productId() == pid);
    }

    @Test
    void getDevices_includesLoopback() {
        var deviceList = USB.getDevices(device -> device.vendorId() == vid && device.productId() == pid);
        assertThat(deviceList)
                .isNotEmpty()
                .anyMatch(device -> device.vendorId() == vid && device.productId() == pid);
    }

    @Test
    void getDevicePredicate_returnsLoopback() {
        var device = USB.getDevice(dev -> dev.vendorId() == vid && dev.productId() == pid);
        assertThat(device).isPresent();
        assertThat(device.get().productId()).isEqualTo(pid);
        assertThat(device.get().vendorId()).isEqualTo(vid);
    }

    @Test
    void getDeviceVidPid_returnsLoopback() {
        var device = USB.getDevice(vid, pid);
        assertThat(device).isPresent();
        assertThat(device.get().productId()).isEqualTo(pid);
        assertThat(device.get().vendorId()).isEqualTo(vid);
    }
}
