//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import net.codecrete.usb.*;

/**
 * Sample application enumerating connected USB devices.
 */
public class Enumerate {

    public static void main(String[] args) {
        // display the already present USB devices
        for (var device : USB.getAllDevices())
            printDevice(device);
    }

    private static void printDevice(USBDevice device) {
        System.out.println("Device:");
        System.out.printf("  VID: 0x%04x%n", device.vendorId());
        System.out.printf("  PID: 0x%04x%n", device.productId());
        if (device.manufacturer() != null)
            System.out.printf("  Manufacturer:  %s%n", device.manufacturer());
        if (device.product() != null)
            System.out.printf("  Product name:  %s%n", device.product());
        if (device.serialNumber() != null)
            System.out.printf("  Serial number: %s%n", device.serialNumber());
        System.out.printf("  Device class:    0x%02x%n", device.classCode());
        System.out.printf("  Device subclass: 0x%02x%n", device.subclassCode());
        System.out.printf("  Device protocol: 0x%02x%n", device.protocolCode());

        for (var intf: device.interfaces())
            printInterface(intf);

        System.out.println();
        System.out.println();
    }

    private static void printInterface(USBInterface intf) {
        for (var alt : intf.alternates())
            printAlternate(alt, intf.number(), alt == intf.alternate());
    }

    private static void printAlternate(USBAlternateInterface alt, int intferaceNumber, boolean isDefault) {
        System.out.println();
        if (isDefault) {
            System.out.printf("  Interface %d%n", intferaceNumber);
        } else {
            System.out.printf("  Interface %d (alternate %d)%n", intferaceNumber, alt.number());
        }

        System.out.printf("    Interface class:    0x%02x%n", alt.classCode());
        System.out.printf("    Interface subclass: 0x%02x%n", alt.subclassCode());
        System.out.printf("    Interface protocol: 0x%02x%n", alt.protocolCode());

        for (var endpoint : alt.endpoints())
            printEndpoint(endpoint);
    }

    private static void printEndpoint(USBEndpoint endpoint) {
        System.out.println();
        System.out.printf("    Endpoint %d%n", endpoint.number());
        System.out.printf("        Direction: %s%n", endpoint.direction().name());
        System.out.printf("        Transfer type: %s%n", endpoint.transferType().name());
        System.out.printf("        Packet size: %d bytes%n", endpoint.packetSize());
    }
}
