//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.sample;

import net.codecrete.usb.USB;

/**
 * Sample program displaying information when USB devices are connected or disconnected.
 * <p>
 * Quit with Ctrl-C or whatever stops a program on your platform.
 * </p>
 */
public class MonitorDevices {

    public static void main(String[] args) throws InterruptedException {
        USB.setOnDeviceConnected((device) -> System.out.println("Connected:    " + device.toString()));
        USB.setOnDeviceDisconnected((device) -> System.out.println("Disconnected: " + device.toString()));

        for (var device : USB.getAllDevices())
            System.out.println("Present:      " + device.toString());
        System.out.println("Monitoring...");

        //noinspection InfiniteLoopStatement
        while (true) {
            Thread.sleep(1000000);
        }
    }
}
