//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for bulk transfer
//

package net.codecrete.usb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeviceLifecycleTest {

    private USBDevice device;

    @Test
    void lifecycle_showsValidState() {
        device = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        if (device == null)
            throw new IllegalStateException("USB loopback test device must be connected");

        var intf = device.interfaces().get(0);
        assertEquals(0, intf.number());

        assertFalse(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.claimInterface(0));
        assertThrows(USBException.class, () -> device.releaseInterface(0));

        device.open();

        assertTrue(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.releaseInterface(0));

        device.claimInterface(0);

        assertTrue(device.isOpen());
        assertTrue(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.claimInterface(0));

        device.releaseInterface(0);

        assertTrue(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.releaseInterface(0));

        device.close();

        assertFalse(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.claimInterface(0));
        assertThrows(USBException.class, () -> device.releaseInterface(0));
    }

    @AfterEach
    void cleanup() {
        if (device != null)
            device.close();
    }
}
