//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import net.codecrete.usb.*;

/**
 * Sample application transferring data to and from bulk endpoints.
 * <p>
 * This example assumes that one of the interfaces has two bulk endpoints,
 * one for sending and one for receiving data. The test device fulfils
 * this requirement (see
 * <a href="https://github.com/manuelbl/JavaDoesUSB/tree/main/test-devices/loopback-stm32">loopback-stm32</a>)
 * </p>
 */
public class BulkTransfer {

    private static final int VID = 0xcafe;
    private static final int PID = 0xceaf;
    private static final int INTERFACE_NO = 0;
    private static final int ENDPOINT_OUT = 1;
    private static final int ENDPOINT_IN = 2;

    public static void main(String[] args) {
        var optionalDevice = Usb.findDevice(VID, PID);
        if (optionalDevice.isEmpty()) {
            System.out.printf("No USB device with VID=0x%04x and PID=0x%04x found.%n", VID, PID);
            return;
        }

        var device = optionalDevice.get();
        device.open();
        device.claimInterface(INTERFACE_NO);

        var data = new byte[] { 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21 };
        device.transferOut(ENDPOINT_OUT, data);
        System.out.println(data.length + " bytes sent.");

        data = device.transferIn(ENDPOINT_IN);
        System.out.println(data.length + " bytes received.");

        device.close();
    }
}
