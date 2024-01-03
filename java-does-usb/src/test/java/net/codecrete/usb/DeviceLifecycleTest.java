//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for device lifecycle (open/close, claim/release)
//

package net.codecrete.usb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceLifecycleTest {

    private UsbDevice device;

    @Test
    void lifecycle_showsValidState() {
        device = TestDeviceBase.getDevice();
        var interfaceNumber = TestDeviceBase.getDeviceConfig().interfaceNumber();

        var intf = device.getInterfaces().get(interfaceNumber);
        assertEquals(interfaceNumber, intf.getNumber());

        assertFalse(device.isOpened());
        assertFalse(intf.isClaimed());
        assertThrows(UsbException.class, () -> device.claimInterface(interfaceNumber));
        assertThrows(UsbException.class, () -> device.releaseInterface(interfaceNumber));

        device.open();

        assertTrue(device.isOpened());
        assertFalse(intf.isClaimed());
        assertThrows(UsbException.class, () -> device.open());
        assertThrows(UsbException.class, () -> device.releaseInterface(interfaceNumber));

        device.claimInterface(interfaceNumber);

        assertTrue(device.isOpened());
        assertTrue(intf.isClaimed());
        assertThrows(UsbException.class, () -> device.open());
        assertThrows(UsbException.class, () -> device.claimInterface(interfaceNumber));

        device.releaseInterface(interfaceNumber);

        assertTrue(device.isOpened());
        assertFalse(intf.isClaimed());
        assertThrows(UsbException.class, () -> device.open());
        assertThrows(UsbException.class, () -> device.releaseInterface(interfaceNumber));

        device.close();

        assertFalse(device.isOpened());
        assertFalse(intf.isClaimed());
        assertThrows(UsbException.class, () -> device.claimInterface(interfaceNumber));
        assertThrows(UsbException.class, () -> device.releaseInterface(interfaceNumber));
    }

    @AfterEach
    void cleanup() {
        if (device != null)
            device.close();
    }
}
