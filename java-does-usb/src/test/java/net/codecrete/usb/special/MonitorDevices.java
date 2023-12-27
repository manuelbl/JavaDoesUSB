//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.special;

import net.codecrete.usb.*;

import java.io.IOException;

/**
 * Test program that communicates with the device as soon as it has been pugged in.
 * <p>
 * Quit with Ctrl-C or whatever stops a program on your platform.
 * </p>
 */
public class MonitorDevices {

    public static void main(String[] args) throws IOException {
        Usb.setOnDeviceConnected((device) -> {
            System.out.println("Connected:    " + device.toString());
            talkToTestDevice(device);
        });
        Usb.setOnDeviceDisconnected((device) -> System.out.println("Disconnected: " + device.toString()));

        for (var device : Usb.getDevices()) {
            System.out.println("Present:      " + device.toString());
            talkToTestDevice(device);
        }
        System.out.println("Monitoring...");

        //noinspection ResultOfMethodCallIgnored
        System.in.read();
    }

    private static void talkToTestDevice(UsbDevice device) {
        if (device.getVendorId() != 0xcafe)
            return; // no test device

        int interfaceNumber = device.getProductId() == 0xcea0 ? 2 : 0;
        device.open();
        device.claimInterface(interfaceNumber);
        var response = device.controlTransferIn(
                new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE,
                        (byte) 0x05, (short) 0, (short) interfaceNumber),
                1
        );

        if (response.length == 1 || interfaceNumber == response[0]) {
            System.out.println("Device responded");
        } else {
            System.err.println("Invalid response from device");
        }
    }
}
