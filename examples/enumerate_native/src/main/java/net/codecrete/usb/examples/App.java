//
// Java Does USB
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import net.codecrete.usb.Usb;

public class App
{
    public static void main(String[] args) {
        for (var device : Usb.getDevices()) {
            System.out.println(device);
        }
    }
}
