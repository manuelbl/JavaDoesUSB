package net.codecrete.usb.examples

import net.codecrete.usb.*
import kotlin.math.min


fun main() {
    Enumerate().enumerate()
}

class Enumerate {

    private var classInfo = UsbClassInfo()

    fun enumerate() {
        for (device in Usb.getDevices()) {
            printDevice(device)
        }
    }

    private fun printDevice(device: UsbDevice) {
        println("Device:")
        System.out.printf("  VID: 0x%04x%n", device.vendorId)
        System.out.printf("  PID: 0x%04x%n", device.productId)
        if (device.manufacturer != null) System.out.printf("  Manufacturer:  %s%n", device.manufacturer)
        if (device.product != null) System.out.printf("  Product name:  %s%n", device.product)
        if (device.serialNumber != null) System.out.printf("  Serial number: %s%n", device.serialNumber)
        System.out.printf("  Device class:    0x%02x", device.classCode)
        printInParens(classInfo.lookupClass(device.classCode))
        System.out.printf("  Device subclass: 0x%02x", device.subclassCode)
        printInParens(classInfo.lookupSubclass(device.classCode, device.subclassCode))
        System.out.printf("  Device protocol: 0x%02x", device.protocolCode)
        printInParens(classInfo.lookupProtocol(device.classCode, device.subclassCode, device.protocolCode))
        for (intf in device.interfaces) printInterface(intf)
        printRawDescriptor("Device descriptor", device.deviceDescriptor)
        printRawDescriptor("Configuration descriptor", device.configurationDescriptor)
        println()
        println()
    }

    private fun printInParens(text: String?) {
        if (text != null) {
            System.out.printf(" (%s)%n", text)
        } else {
            println()
        }
    }

    private fun printInterface(intf: UsbInterface) {
        for (alt in intf.alternates) printAlternate(alt, intf.number, alt === intf.currentAlternate)
    }

    private fun printAlternate(alt: UsbAlternateInterface, intferfaceNumber: Int, isDefault: Boolean) {
        println()
        if (isDefault) {
            System.out.printf("  Interface %d%n", intferfaceNumber)
        } else {
            System.out.printf("  Interface %d (alternate %d)%n", intferfaceNumber, alt.number)
        }
        System.out.printf("    Interface class:    0x%02x", alt.classCode)
        printInParens(classInfo.lookupClass(alt.classCode))
        System.out.printf("    Interface subclass: 0x%02x", alt.subclassCode)
        printInParens(classInfo.lookupProtocol(alt.classCode, alt.subclassCode, alt.protocolCode))
        System.out.printf("    Interface protocol: 0x%02x", alt.protocolCode)
        printInParens(classInfo.lookupProtocol(alt.classCode, alt.subclassCode, alt.protocolCode))
        for (endpoint in alt.endpoints)
            printEndpoint(endpoint)
    }

    private fun printEndpoint(endpoint: UsbEndpoint) {
        println()
        System.out.printf("    Endpoint %d%n", endpoint.number)
        System.out.printf("        Direction: %s%n", endpoint.direction.name)
        System.out.printf("        Transfer type: %s%n", endpoint.transferType.name)
        System.out.printf("        Packet size: %d bytes%n", endpoint.packetSize)
    }

    private fun printRawDescriptor(title: String, descriptor: ByteArray) {
        println()
        println(title)
        val len = descriptor.size
        var i = 0
        while (i < len) {
            System.out.printf("%04x ", i)
            var j = i
            while (j < min(i + 16, len)) {
                System.out.printf(" %02x", descriptor[j].toInt() and 255)
                j += 1
            }
            println()
            i += 16
        }
    }

}