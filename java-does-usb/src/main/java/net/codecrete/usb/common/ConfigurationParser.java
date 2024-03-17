//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbAlternateInterface;
import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbTransferType;
import net.codecrete.usb.usbstandard.ConfigurationDescriptor;
import net.codecrete.usb.usbstandard.EndpointDescriptor;
import net.codecrete.usb.usbstandard.InterfaceAssociationDescriptor;
import net.codecrete.usb.usbstandard.InterfaceDescriptor;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;

import static net.codecrete.usb.usbstandard.Constants.CONFIGURATION_DESCRIPTOR_TYPE;
import static net.codecrete.usb.usbstandard.Constants.ENDPOINT_DESCRIPTOR_TYPE;
import static net.codecrete.usb.usbstandard.Constants.INTERFACE_ASSOCIATION_DESCRIPTOR_TYPE;
import static net.codecrete.usb.usbstandard.Constants.INTERFACE_DESCRIPTOR_TYPE;

/**
 * Parser for USB configuration descriptors.
 *
 * <p>
 * It extracts the information about endpoints, interfaces (incl. alternate interfaces) and associations
 * between interfaces to derive the functions. Other descriptor types are ignored.
 * </p>
 */
public class ConfigurationParser {

    /**
     * Parses a USB configuration descriptor (incl. interface and endpoint descriptors)
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

        UsbAlternateInterfaceImpl lastAlternate = null;
        var offset = peekDescLength(0);

        while (offset < descriptor.byteSize()) {

            var descLength = peekDescLength(offset);
            var descType = peekDescType(offset);

            if (descType == INTERFACE_DESCRIPTOR_TYPE) {
                var intf = parseInterface(offset);

                var parent = configuration.findInterfaceByNumber(intf.getNumber());
                if (parent != null) {
                    parent.addAlternate(intf.getCurrentAlternate());
                } else {
                    configuration.addInterface(intf);
                }
                lastAlternate = (UsbAlternateInterfaceImpl) intf.getCurrentAlternate();

                var function = configuration.findFunction(intf.getNumber());
                if (function == null) {
                    function = new CompositeFunction(intf.getNumber(), 1, lastAlternate.getClassCode(),
                            lastAlternate.getSubclassCode(), lastAlternate.getProtocolCode());
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
            throw new UsbException("invalid USB configuration descriptor");

        var totalLength = desc.totalLength();
        if (descriptor.byteSize() != totalLength)
            throw new UsbException("invalid USB configuration descriptor (invalid length)");

        configuration = new Configuration(desc.configurationValue(), desc.attributes(), desc.maxPower());
    }

    private UsbInterfaceImpl parseInterface(int offset) {
        var desc = new InterfaceDescriptor(descriptor, offset);
        var alternate = new UsbAlternateInterfaceImpl(desc.alternateSetting(), desc.interfaceClass(),
                desc.interfaceSubClass(), desc.interfaceProtocol(), new ArrayList<>());
        var alternates = new ArrayList<UsbAlternateInterface>();
        alternates.add(alternate);
        return new UsbInterfaceImpl(desc.interfaceNumber(), alternates);
    }

    private void parseIAD(int offset) {
        var desc = new InterfaceAssociationDescriptor(descriptor, offset);
        var function = new CompositeFunction(desc.firstInterface(), desc.interfaceCount(), desc.functionClass(),
                desc.functionSubClass(), desc.functionProtocol());
        configuration.addFunction(function);
    }

    private UsbEndpointImpl parseEndpoint(int offset) {
        var desc = new EndpointDescriptor(descriptor, offset);
        var address = desc.endpointAddress();
        return new UsbEndpointImpl(getEndpointNumber(address), getEndpointDirection(address),
                getEndpointType(desc.attributes()), desc.maxPacketSize());
    }

    private static UsbDirection getEndpointDirection(int address) {
        return (address & 0x80) != 0 ? UsbDirection.IN : UsbDirection.OUT;
    }

    private static int getEndpointNumber(int address) {
        return address & 0x7f;
    }

    private static UsbTransferType getEndpointType(int attributes) {
        return switch (attributes & 0x3) {
            case 1 -> UsbTransferType.ISOCHRONOUS;
            case 2 -> UsbTransferType.BULK;
            case 3 -> UsbTransferType.INTERRUPT;
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
        return 0xff & descriptor.get(ValueLayout.JAVA_BYTE, offset + 1L);
    }

}
