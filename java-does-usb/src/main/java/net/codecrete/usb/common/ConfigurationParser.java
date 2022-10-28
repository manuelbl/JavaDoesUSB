//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBAlternateInterface;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBException;
import net.codecrete.usb.USBTransferType;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;
import net.codecrete.usb.usbstandard.EndpointDescriptor;
import net.codecrete.usb.usbstandard.InterfaceAssociationDescriptor;
import net.codecrete.usb.usbstandard.InterfaceDescriptor;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;

import static net.codecrete.usb.usbstandard.Constants.*;

/**
 * Parser for USB configuration descriptors
 */
public class ConfigurationParser {

    /**
     * Parse a USB configuration descriptor (incl. interface and endpoint descriptors)
     *
     * @param desc configuration descriptor
     * @return parsed configuration data
     */
    public static Configuration parseConfigurationDescriptor(MemorySegment desc) {
        var parser = new ConfigurationParser(desc);
        return parser.parse();
    }

    private final MemorySegment descriptor;
    private Configuration configuration;

    /**
     * Creates a new parser for USB configuration descriptors (incl. interface and endpoint descriptors)
     *
     * @param descriptor configuration descriptor
     */
    public ConfigurationParser(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    public Configuration parse() {
        parseHeader();

        USBAlternateInterfaceImpl lastAlternate = null;
        int offset = peekDescLength(0);

        while (offset < descriptor.byteSize()) {

            int descLength = peekDescLength(offset);
            int descType = peekDescType(offset);

            if (descType == INTERFACE_DESCRIPTOR_TYPE) {
                var intf = parseInterface(offset);

                var parent = configuration.findInterfaceByNumber(intf.number());
                if (parent != null) {
                    parent.addAlternate(intf.alternate());
                } else {
                    configuration.addInterface(intf);
                }
                lastAlternate = (USBAlternateInterfaceImpl) intf.alternate();

                var function = configuration.findFunction(intf.number());
                if (function == null) {
                    function = new CompositeFunction(intf.number(), 1, lastAlternate.classCode(),
                            lastAlternate.subclassCode(), lastAlternate.protocolCode());
                    configuration.addFunction(function);
                }

            } else if (descType == ENDPOINT_DESCRIPTOR_TYPE) {
                var endpoint = parseEndpoint(offset);
                if (lastAlternate != null)
                    lastAlternate.addEndpoint(endpoint);

            } else if (descType == INTERFACE_ASSOCIATION_DESCRIPTOR_TYPE) {
                parseIAD(offset);
            }

            offset += descLength;
        }

        return configuration;
    }

    private void parseHeader() {
        var desc = new ConfigurationDescriptor(descriptor);
        if (CONFIGURATION_DESCRIPTOR_TYPE != desc.descriptorType())
            throw new USBException("Invalid USB configuration descriptor");

        int totalLength = desc.totalLength();
        if (descriptor.byteSize() != totalLength)
            throw new USBException("Invalid USB configuration descriptor length");

        configuration = new Configuration(desc.configurationValue(), desc.attributes(), desc.maxPower());
    }

    private USBInterfaceImpl parseInterface(int offset) {
        var desc = new InterfaceDescriptor(descriptor.asSlice(offset, InterfaceDescriptor.LAYOUT.byteSize()));
        var alternate = new USBAlternateInterfaceImpl(desc.alternateSetting(), desc.interfaceClass(),
                desc.interfaceSubClass(), desc.interfaceProtocol(), new ArrayList<>());
        var alternates = new ArrayList<USBAlternateInterface>();
        alternates.add(alternate);
        return new USBInterfaceImpl(desc.interfaceNumber(), alternates);
    }

    private void parseIAD(int offset) {
        var desc = new InterfaceAssociationDescriptor(
                descriptor.asSlice(offset, InterfaceAssociationDescriptor.LAYOUT.byteSize()));
        var function = new CompositeFunction(desc.firstInterface(), desc.interfaceCount(), desc.functionClass(),
                desc.functionSubClass(), desc.functionProtocol());
        configuration.addFunction(function);
    }

    private USBEndpointImpl parseEndpoint(int offset) {
        var desc = new EndpointDescriptor(descriptor.asSlice(offset, EndpointDescriptor.LAYOUT.byteSize()));
        var address = desc.endpointAddress();
        return new USBEndpointImpl(getEndpointNumber(address), getEndpointDirection(address),
                getEndpointType(desc.attributes()), desc.maxPacketSize());
    }

    private static USBDirection getEndpointDirection(int address) {
        return (address & 0x80) != 0 ? USBDirection.IN : USBDirection.OUT;
    }

    private static int getEndpointNumber(int address) {
        return address & 0x7f;
    }

    private static USBTransferType getEndpointType(int attributes) {
        return switch (attributes & 0x3) {
            case 1 -> USBTransferType.ISOCHRONOUS;
            case 2 -> USBTransferType.BULK;
            case 3 -> USBTransferType.INTERRUPT;
            default -> null;
        };
    }

    /**
     * Get descriptor length.
     *
     * @param offset offset to the descriptor of interest
     * @return descriptor length (in bytes)
     */
    private int peekDescLength(int offset) {
        return 0xff & descriptor.get(ValueLayout.JAVA_BYTE, offset);
    }

    /**
     * Get descriptor type.
     *
     * @param offset offset to the descriptor of interest
     * @return descriptor type
     */
    private int peekDescType(int offset) {
        return 0xff & descriptor.get(ValueLayout.JAVA_BYTE, offset + 1);
    }

}
