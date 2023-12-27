//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.examples

import net.codecrete.usb.Usb
import net.codecrete.usb.UsbDevice

/**
 * Sample application monitoring USB devices as they are connected and disconnected.
 */

fun main() {
    Monitor().monitor()
}

class Monitor {
    fun monitor() {
        // register callbacks for events
        Usb.setOnDeviceConnected { device -> printDetails(device, "Connected") }
        Usb.setOnDeviceDisconnected { device ->
            printDetails( device, "Disconnected")
        }

        // display the already present USB devices
        for (device in Usb.getDevices())
            printDetails(device, "Present")

        // wait for ENTER to quit program
        println("Monitoring... Press ENTER to quit.")
        System.`in`.read()
    }

    private fun printDetails(device: UsbDevice, event: String) {
        print(String.format("%-14s", "$event:"))
        println(device.toString())
    }
}