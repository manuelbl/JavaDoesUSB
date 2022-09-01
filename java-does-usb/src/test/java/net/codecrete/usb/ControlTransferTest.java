//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for control transfer
//

package net.codecrete.usb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ControlTransferTest {

    private static USBDevice device;

    @Test
    void storeValue_succeeds() {
        device.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x01, (short) 10730, (short) 0), null);
    }

    @Test
    void retrieveValue_isSameAsStored() {
        device.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x01, (short) 0x9a41, (short) 0), null);
        var valueBytes = device.controlTransferIn(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x03, (short) 0, (short) 0), 4);
        var expectedBytes = new byte[] { (byte)0x41, (byte)0x9a, (byte)0x00, (byte)0x00 };
        assertArrayEquals(expectedBytes, valueBytes);
    }

    @Test
    void storeValueInDataStage_canBeRetrieved() {
        var sentValue = new byte[] { (byte)0x83, (byte)0x03, (byte)0xda, (byte)0x3e };
        device.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x02, (short) 0, (short) 0), sentValue);
        var retrievedValue = device.controlTransferIn(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x03, (short) 0, (short) 0), 4);
        assertArrayEquals(sentValue, retrievedValue);
    }

    @BeforeAll
    static void openDevice() {
        device = USB.getDevice(new USBDeviceFilter(0xcafe, 0xceaf));
        if (device == null)
            throw new IllegalStateException("USB loopback test device must be connected");
        device.open();
        device.claimInterface(0);
    }

    @AfterAll
    static void closeDevice() throws Exception {
        if (device != null) {
            device.close();
            device = null;
        }
    }
}
