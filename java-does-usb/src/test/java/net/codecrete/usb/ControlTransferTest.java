//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for control transfer
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests control transfers
 */
class ControlTransferTest extends TestDeviceBase {

    @Test
    void storeValue_succeeds() {
        testDevice.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x01, (short) 10730, (short) interfaceNumber), null);
    }

    @Test
    void retrieveValue_isSameAsStored() {
        testDevice.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x01, (short) 0x9a41, (short) interfaceNumber), null);
        var valueBytes = testDevice.controlTransferIn(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x03, (short) 0, (short) interfaceNumber), 4);
        var expectedBytes = new byte[]{(byte) 0x41, (byte) 0x9a, (byte) 0x00, (byte) 0x00};
        assertArrayEquals(expectedBytes, valueBytes);
    }

    @Test
    void storeValueInDataStage_canBeRetrieved() {
        var sentValue = new byte[]{(byte) 0x83, (byte) 0x03, (byte) 0xda, (byte) 0x3e};
        testDevice.controlTransferOut(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x02, (short) 0, (short) interfaceNumber), sentValue);
        var retrievedValue = testDevice.controlTransferIn(new USBControlTransfer(USBRequestType.VENDOR, USBRecipient.INTERFACE, (byte) 0x03, (short) 0, (short) interfaceNumber), 4);
        assertArrayEquals(sentValue, retrievedValue);
    }
}
