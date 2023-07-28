//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBAlternateInterface;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBEndpoint;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public class USBAlternateInterfaceImpl implements USBAlternateInterface {

    private final int alternateInterfaceNumber;
    private final int alternateInterfaceClass;
    private final int alternateInterfaceSubclass;
    private final int alternateInterfaceProtocol;
    private final List<USBEndpoint> endpointList;

    public USBAlternateInterfaceImpl(int number, int classCode, int subclassCode, int protocolCode,
                                     List<USBEndpoint> endpoints) {
        alternateInterfaceNumber = number;
        alternateInterfaceClass = classCode;
        alternateInterfaceSubclass = subclassCode;
        alternateInterfaceProtocol = protocolCode;
        endpointList = endpoints;
    }

    @Override
    public int number() {
        return alternateInterfaceNumber;
    }

    @Override
    public int classCode() {
        return alternateInterfaceClass;
    }

    @Override
    public int subclassCode() {
        return alternateInterfaceSubclass;
    }

    @Override
    public int protocolCode() {
        return alternateInterfaceProtocol;
    }

    @Override
    public List<USBEndpoint> endpoints() {
        return unmodifiableList(endpointList);
    }

    void addEndpoint(USBEndpoint endpoint) {
        endpointList.add(endpoint);
    }

    @Override
    public USBEndpoint getEndpoint(int endpointNumber, USBDirection direction) {
        return endpointList.stream().filter(ep -> ep.number() == endpointNumber && ep.direction() == direction).findFirst().orElse(null);
    }
}
