//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import net.codecrete.usb.*;

import java.util.Optional;

/**
 * Sample application enumerating connected USB devices.
 */
@SuppressWarnings("java:S106")
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
        System.out.printf("  Device class:    0x%02x", device.classCode());
        printInParens(USBClassInfo.lookupClass(device.classCode()));
        System.out.printf("  Device subclass: 0x%02x", device.subclassCode());
        printInParens(USBClassInfo.lookupSubclass(device.classCode(), device.subclassCode()));
        System.out.printf("  Device protocol: 0x%02x", device.protocolCode());
        printInParens(USBClassInfo.lookupProtocol(device.classCode(), device.subclassCode(), device.protocolCode()));

        for (var intf: device.interfaces())
            printInterface(intf);

        printRawDescriptor("Device descriptor", device.deviceDescriptor());
        printRawDescriptor("Configuration descriptor", device.configurationDescriptor());

        System.out.println();
        System.out.println();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void printInParens(Optional<String> text) {
        if (text.isPresent()) {
            System.out.printf(" (%s)%n", text.get());
        } else {
            System.out.println();
        }
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

        System.out.printf("    Interface class:    0x%02x", alt.classCode());
        printInParens(USBClassInfo.lookupClass(alt.classCode()));
        System.out.printf("    Interface subclass: 0x%02x", alt.subclassCode());
        printInParens(USBClassInfo.lookupProtocol(alt.classCode(), alt.subclassCode(), alt.protocolCode()));
        System.out.printf("    Interface protocol: 0x%02x", alt.protocolCode());
        printInParens(USBClassInfo.lookupProtocol(alt.classCode(), alt.subclassCode(), alt.protocolCode()));

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

    private static void printRawDescriptor(String title, byte[] descriptor) {
        System.out.println();
        System.out.println(title);

        int len = descriptor.length;
        for (int i = 0; i < len; i += 16) {
            System.out.printf("%04x ", i);
            for (int j = i; j < Math.min(i + 16, len); j += 1)
                System.out.printf(" %02x", descriptor[j] & 255);
            System.out.println();
        }
    }
}
