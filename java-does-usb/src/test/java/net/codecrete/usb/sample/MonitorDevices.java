//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.sample;

import net.codecrete.usb.USB;
import net.codecrete.usb.USBDeviceInfo;

/**
 * Sample program enumerating the connected USB devices
 */
public class MonitorDevices {

    public static void main(String[] args) {
        USB.setOnDeviceConnected((dev) -> System.out.println("Connected:    " + dev.toString()));
        USB.setOnDeviceDisconnected((dev) -> System.out.println("Disconnected: " + dev.toString()));
    }
}
