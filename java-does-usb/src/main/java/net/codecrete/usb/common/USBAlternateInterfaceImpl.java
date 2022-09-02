//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBAlternateInterface;
import net.codecrete.usb.USBEndpoint;

import java.util.Collections;
import java.util.List;

public class USBAlternateInterfaceImpl implements USBAlternateInterface {

    private final int number;
    private final int classCode;
    private final int subclassCode;
    private final int protocolCode;
    private final List<USBEndpoint> endpoints;

    public USBAlternateInterfaceImpl(int number, int classCode, int subclassCode, int protocolCode,
                                     List<USBEndpoint> endpoints) {
        this.number = number;
        this.classCode = classCode;
        this.subclassCode = subclassCode;
        this.protocolCode = protocolCode;
        this.endpoints = endpoints;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int getClassCode() {
        return classCode;
    }

    @Override
    public int getSubclassCode() {
        return subclassCode;
    }

    @Override
    public int getProtocolCode() {
        return protocolCode;
    }

    @Override
    public List<USBEndpoint> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    void addEndpoint(USBEndpoint endpoint) {
        endpoints.add(endpoint);
    }
}
