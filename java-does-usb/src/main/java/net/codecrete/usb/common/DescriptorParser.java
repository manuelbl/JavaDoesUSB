//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBEndpointType;
import net.codecrete.usb.USBException;
import net.codecrete.usb.USBInterface;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for USB descriptors
 */
public class DescriptorParser {

    /**
     * Parse a USB configuration descriptor (incl. interface and endpoint descriptors)
     *
     * @param descriptor configuration descriptor
     * @return parsed configuration data
     */
    public static Configuration parseConfigurationDescriptor(byte[] descriptor) {
        var desc = MemorySegment.ofArray(descriptor);
        return parseConfigurationDescriptor(desc);
    }

    /**
     * Parse a USB configuration descriptor (incl. interface and endpoint descriptors)
     *
     * @param desc configuration descriptor
     * @return parsed configuration data
     */
    public static Configuration parseConfigurationDescriptor(MemorySegment desc) {
        var config = parseConfiguration(desc, 0);

        USBAlternateInterfaceImpl lastAlternate = null;
        USBEndpointImpl lastEndpoint;
        int offset = peekDescLength(desc, 0);

        while (offset < desc.byteSize()) {

            int descLength = peekDescLength(desc, offset);
            int descType = peekDescType(desc, offset);

            if (descType == USBDescriptors.INTERFACE_DESCRIPTOR_TYPE) {
                var intf = parseInterface(desc, offset);
                var parent = config.findInterfaceByNumber(intf.getNumber());
                if (parent != null) {
                    parent.addAlternate(intf.getAlternate());
                } else {
                    config.addInterface(intf);
                }
                lastAlternate = (USBAlternateInterfaceImpl) intf.getAlternate();

            } else if (descType == USBDescriptors.ENDPOINT_DESCRIPTOR_TYPE) {
                lastEndpoint = parseEndpoint(desc, offset);
                if (lastAlternate != null)
                    lastAlternate.addEndpoint(lastEndpoint);

            } else {
                System.err.println("Warning: unsupported USB descriptor type " + descType);
            }

            offset += descLength;
        }

        return config;
    }

    private static Configuration parseConfiguration(MemorySegment descriptor, int offset) {
        var desc = descriptor.asSlice(offset, USBDescriptors.Configuration.byteSize());
        var config = new Configuration();
        if (USBDescriptors.CONFIGURATION_DESCRIPTOR_TYPE != (byte) USBDescriptors.Configuration_bDescriptorType.get(desc))
            throw new USBException("Invalid USB configuration descriptor");

        config.configValue = (byte) USBDescriptors.Configuration_bConfigurationValue.get(desc);
        config.attributes = (byte) USBDescriptors.Configuration_bmAttributes.get(desc);
        config.maxPower = (byte) USBDescriptors.Configuration_bMaxPower.get(desc);
        config.interfaces = new ArrayList<>();
        return config;
    }

    private static USBInterfaceImpl parseInterface(MemorySegment descriptor, int offset) {
        var desc = descriptor.asSlice(offset, USBDescriptors.Interface.byteSize());
        var number = 255 & (byte) USBDescriptors.Interface_bInterfaceNumber.get(desc);
        var altSetting = 255 & (byte) USBDescriptors.Interface_bAlternateSetting.get(desc);
        var classCode = 255 & (byte) USBDescriptors.Interface_bInterfaceClass.get(desc);
        var subclassCode = 255 & (byte) USBDescriptors.Interface_bInterfaceSubClass.get(desc);
        var protocol = 255 & (byte) USBDescriptors.Interface_bInterfaceProtocol.get(desc);
        var alternate = new USBAlternateInterfaceImpl(altSetting, classCode, subclassCode, protocol, new ArrayList<>());
        return new USBInterfaceImpl(number, List.of(alternate));
    }

    private static USBEndpointImpl parseEndpoint(MemorySegment descriptor, int offset) {
        var desc = descriptor.asSlice(offset, USBDescriptors.Endpoint.byteSize());
        var address = (byte) USBDescriptors.Endpoint_bEndpointAddress.get(desc);
        var attributes = (byte) USBDescriptors.Endpoint_bmAttributes.get(desc);
        var maxPacketSize = (short) USBDescriptors.Endpoint_wMaxPacketSize.get(desc);
        return new USBEndpointImpl(getEndpointNumber(address), getEndpointDirection(address),
                getEndpointType(attributes), maxPacketSize);
    }

    private static USBDirection getEndpointDirection(byte address) {
        return (address & 0x80) != 0 ? USBDirection.IN : USBDirection.OUT;
    }

    private static int getEndpointNumber(byte address) {
        return address & 0x7f;
    }

    private static USBEndpointType getEndpointType(byte attributes) {
        return switch (attributes & 0x3) {
            case 1 -> USBEndpointType.ISOCHRONOUS;
            case 2 -> USBEndpointType.BULK;
            case 3 -> USBEndpointType.INTERRUPT;
            default -> null;
        };
    }

    /**
     * Get descriptor length.
     *
     * @param desc   byte array containing multiple descriptors
     * @param offset offset to the descriptor of interest
     * @return descriptor length (in bytes)
     */
    private static int peekDescLength(MemorySegment desc, int offset) {
        return 255 & desc.get(ValueLayout.JAVA_BYTE, offset);
    }

    /**
     * Get descriptor type.
     *
     * @param desc   byte array containing multiple descriptors
     * @param offset offset to the descriptor of interest
     * @return descriptor type
     */
    private static int peekDescType(MemorySegment desc, int offset) {
        return 255 & desc.get(ValueLayout.JAVA_BYTE, offset + 1);
    }

    public static class Configuration {
        public List<USBInterface> interfaces;
        public byte configValue;
        public byte attributes;
        public byte maxPower;

        void addInterface(USBInterface intf) {
            interfaces.add(intf);
        }

        public USBInterfaceImpl findInterfaceByNumber(int number) {
            return (USBInterfaceImpl) interfaces.stream()
                    .filter((intf) -> intf.getNumber() == number).findFirst().orElse(null);
        }
    }
}
