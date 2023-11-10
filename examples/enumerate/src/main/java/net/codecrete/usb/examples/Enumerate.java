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
        for (var device : Usb.getDevices())
            printDevice(device);
    }

    private static void printDevice(UsbDevice device) {
        System.out.println("Device:");
        System.out.printf("  VID: 0x%04x%n", device.getVendorId());
        System.out.printf("  PID: 0x%04x%n", device.getProductId());
        if (device.getManufacturer() != null)
            System.out.printf("  Manufacturer:  %s%n", device.getManufacturer());
        if (device.getProduct() != null)
            System.out.printf("  Product name:  %s%n", device.getProduct());
        if (device.getSerialNumber() != null)
            System.out.printf("  Serial number: %s%n", device.getSerialNumber());
        System.out.printf("  Device class:    0x%02x", device.getClassCode());
        printInParens(USBClassInfo.lookupClass(device.getClassCode()));
        System.out.printf("  Device subclass: 0x%02x", device.getSubclassCode());
        printInParens(USBClassInfo.lookupSubclass(device.getClassCode(), device.getSubclassCode()));
        System.out.printf("  Device protocol: 0x%02x", device.getProtocolCode());
        printInParens(USBClassInfo.lookupProtocol(device.getClassCode(), device.getSubclassCode(), device.getProtocolCode()));

        for (var intf: device.getInterfaces())
            printInterface(intf);

        printRawDescriptor("Device descriptor", device.getDeviceDescriptor());
        printRawDescriptor("Configuration descriptor", device.getConfigurationDescriptor());

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

    private static void printInterface(UsbInterface intf) {
        for (var alt : intf.getAlternates())
            printAlternate(alt, intf.getNumber(), alt == intf.getCurrentAlternate());
    }

    private static void printAlternate(UsbAlternateInterface alt, int intferaceNumber, boolean isDefault) {
        System.out.println();
        if (isDefault) {
            System.out.printf("  Interface %d%n", intferaceNumber);
        } else {
            System.out.printf("  Interface %d (alternate %d)%n", intferaceNumber, alt.getNumber());
        }

        System.out.printf("    Interface class:    0x%02x", alt.getClassCode());
        printInParens(USBClassInfo.lookupClass(alt.getClassCode()));
        System.out.printf("    Interface subclass: 0x%02x", alt.getSubclassCode());
        printInParens(USBClassInfo.lookupProtocol(alt.getClassCode(), alt.getSubclassCode(), alt.getProtocolCode()));
        System.out.printf("    Interface protocol: 0x%02x", alt.getProtocolCode());
        printInParens(USBClassInfo.lookupProtocol(alt.getClassCode(), alt.getSubclassCode(), alt.getProtocolCode()));

        for (var endpoint : alt.getEndpoints())
            printEndpoint(endpoint);
    }

    private static void printEndpoint(UsbEndpoint endpoint) {
        System.out.println();
        System.out.printf("    Endpoint %d%n", endpoint.getNumber());
        System.out.printf("        Direction: %s%n", endpoint.getDirection().name());
        System.out.printf("        Transfer type: %s%n", endpoint.getTransferType().name());
        System.out.printf("        Packet size: %d bytes%n", endpoint.getPacketSize());
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
