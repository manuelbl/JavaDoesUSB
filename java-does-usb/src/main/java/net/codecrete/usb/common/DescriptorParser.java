//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

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
        var config = parseConfiguration(desc, 0);
        Interface lastInterface = null;
        Endpoint lastEndpoint;
        int offset = peekDescLength(desc, 0);

        while (offset < desc.byteSize()) {

            int descLength = peekDescLength(desc, offset);
            int descType = peekDescType(desc, offset);

            if (descType == USBDescriptors.INTERFACE_DESCRIPTOR_TYPE) {
                lastInterface = parseInterface(desc, offset);
                config.interfaces.add(lastInterface);

            } else if (descType == USBDescriptors.ENDPOINT_DESCRIPTOR_TYPE) {
                lastEndpoint = parseEndpoint(desc, offset);
                lastInterface.endpoints.add(lastEndpoint);
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
        config.configValue = (byte) USBDescriptors.Configuration_bConfigurationValue.get(desc);
        config.attributes = (byte) USBDescriptors.Configuration_bmAttributes.get(desc);
        config.maxPower = (byte) USBDescriptors.Configuration_bMaxPower.get(desc);
        config.interfaces = new ArrayList<>();
        return config;
    }

    private static Interface parseInterface(MemorySegment descriptor, int offset) {
        var desc = descriptor.asSlice(offset, USBDescriptors.Interface.byteSize());
        var intf = new Interface();
        intf.number = (byte) USBDescriptors.Interface_bInterfaceNumber.get(desc);
        intf.altSetting = (byte) USBDescriptors.Interface_bAlternateSetting.get(desc);
        intf.classCode = (byte) USBDescriptors.Interface_bInterfaceClass.get(desc);
        intf.subclassCode = (byte) USBDescriptors.Interface_bInterfaceSubClass.get(desc);
        intf.protocol = (byte) USBDescriptors.Interface_bInterfaceProtocol.get(desc);
        intf.endpoints = new ArrayList<>();
        return intf;
    }

    private static Endpoint parseEndpoint(MemorySegment descriptor, int offset) {
        var desc = descriptor.asSlice(offset, USBDescriptors.Endpoint.byteSize());
        var endpoint = new Endpoint();
        endpoint.address = (byte) USBDescriptors.Endpoint_bEndpointAddress.get(desc);
        endpoint.attributes = (byte) USBDescriptors.Endpoint_bmAttributes.get(desc);
        endpoint.maxPacketSize = (short) USBDescriptors.Endpoint_wMaxPacketSize.get(desc);
        endpoint.interval = (byte) USBDescriptors.Endpoint_bInterval.get(desc);
        return endpoint;
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
        public List<Interface> interfaces;
        public byte configValue;
        public byte attributes;
        public byte maxPower;
    }

    public static class Interface {
        public byte number;
        public byte altSetting;
        public List<Endpoint> endpoints;
        public byte classCode;
        public byte subclassCode;
        public byte protocol;
    }

    public static class Endpoint {
        public byte address;
        public byte attributes;
        public short maxPacketSize;
        public byte interval;
    }
}
