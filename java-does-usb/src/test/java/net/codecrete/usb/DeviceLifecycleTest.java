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

import static net.codecrete.usb.TestDeviceBase.*;
import static org.junit.jupiter.api.Assertions.*;

public class DeviceLifecycleTest {

    private USBDevice device;

    @Test
    void lifecycle_showsValidState() {
        device = USB.getDevice(new USBDeviceFilter(VID, PID));
        if (device == null)
            throw new IllegalStateException("USB loopback test device must be connected");

        var intf = device.interfaces().get(LOOPBACK_INTF);
        assertEquals(LOOPBACK_INTF, intf.number());

        assertFalse(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.claimInterface(LOOPBACK_INTF));
        assertThrows(USBException.class, () -> device.releaseInterface(LOOPBACK_INTF));

        device.open();

        assertTrue(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.releaseInterface(LOOPBACK_INTF));

        device.claimInterface(LOOPBACK_INTF);

        assertTrue(device.isOpen());
        assertTrue(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.claimInterface(LOOPBACK_INTF));

        device.releaseInterface(LOOPBACK_INTF);

        assertTrue(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.releaseInterface(LOOPBACK_INTF));

        device.close();

        assertFalse(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.claimInterface(LOOPBACK_INTF));
        assertThrows(USBException.class, () -> device.releaseInterface(LOOPBACK_INTF));
    }

    @AfterEach
    void cleanup() {
        if (device != null)
            device.close();
    }
}
