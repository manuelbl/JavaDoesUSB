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
        device = TestDeviceBase.getDevice();
        int interfaceNumber = TestDeviceBase.getInterfaceNumber(device);

        var intf = device.interfaces().get(interfaceNumber);
        assertEquals(interfaceNumber, intf.number());

        assertFalse(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.claimInterface(interfaceNumber));
        assertThrows(USBException.class, () -> device.releaseInterface(interfaceNumber));

        device.open();

        assertTrue(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.releaseInterface(interfaceNumber));

        device.claimInterface(interfaceNumber);

        assertTrue(device.isOpen());
        assertTrue(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.claimInterface(interfaceNumber));

        device.releaseInterface(interfaceNumber);

        assertTrue(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.open());
        assertThrows(USBException.class, () -> device.releaseInterface(interfaceNumber));

        device.close();

        assertFalse(device.isOpen());
        assertFalse(intf.isClaimed());
        assertThrows(USBException.class, () -> device.claimInterface(interfaceNumber));
        assertThrows(USBException.class, () -> device.releaseInterface(interfaceNumber));
    }

    @AfterEach
    void cleanup() {
        if (device != null)
            device.close();
    }
}
