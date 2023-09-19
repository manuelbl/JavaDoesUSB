//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for invalid operations (invalid interface number, invalid endpoint number etc.)
//

package net.codecrete.usb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InvalidOperationTest extends TestDeviceBase {

    @Test
    void claimInvalidInterface_throws() {
        // throws error because it's already claimed
        Assertions.assertThrows(USBException.class, () -> testDevice.claimInterface(interfaceNumber));
        // throws error because it's an invalid interface number
        Assertions.assertThrows(USBException.class, () -> testDevice.claimInterface(3));
        // throws error because it's an invalid interface number
        Assertions.assertThrows(USBException.class, () -> testDevice.claimInterface(888));
    }

    @Test
    void releaseInvalidInterface_throws() {
        Assertions.assertThrows(USBException.class, () -> testDevice.releaseInterface(1));
    }


    @Test
    void invalidEndpoint_throws() {
        var data = new byte[] { 34, 23, 99, 0, 17 };

        Assertions.assertThrows(USBException.class, () -> testDevice.transferOut(2, data));

        Assertions.assertThrows(USBException.class, () -> testDevice.transferOut(0, data));

        Assertions.assertThrows(USBException.class, () -> testDevice.transferOut(4, data));

        Assertions.assertThrows(USBException.class, () -> testDevice.transferIn(1));

        Assertions.assertThrows(USBException.class, () -> testDevice.transferIn(0));

        Assertions.assertThrows(USBException.class, () -> testDevice.transferIn(5));
    }
}
