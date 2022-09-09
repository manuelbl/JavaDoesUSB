//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import net.codecrete.usb.USB;
import net.codecrete.usb.USBDevice;

import java.io.IOException;

/**
 * Sample application monitoring USB devices as they are connected and disconnected.
 */
public class Monitor {

    public static void main(String[] args) throws IOException {
        // register callbacks for events
        USB.setOnDeviceConnected((device) -> printDetails(device, "Connected"));
        USB.setOnDeviceDisconnected((device) -> printDetails(device, "Disconnected"));

        // display the already present USB devices
        for (var device : USB.getAllDevices())
            printDetails(device, "Present");

        // wait for ENTER to quit program
        System.out.println("Monitoring... Press ENTER to quit.");
        System.in.read();
    }

    private static void printDetails(USBDevice device, String event) {
        System.out.printf("%-14s", event + ":");
        System.out.println(device.toString());
    }
}
