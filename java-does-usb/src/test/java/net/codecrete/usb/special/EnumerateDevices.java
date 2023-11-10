//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.special;

import net.codecrete.usb.Usb;

/**
 * Sample program displaying the connected USB devices
 */
public class EnumerateDevices {

    public static void main(String[] args) {
        for (var device : Usb.getDevices()) {
            System.out.println(device);
        }
    }
}
