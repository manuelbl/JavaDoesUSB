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

import java.util.Collections;
import java.util.List;

public class USBAlternateInterfaceImpl implements USBAlternateInterface {

    private final int number_;
    private final int classCode_;
    private final int subclassCode_;
    private final int protocolCode_;
    private final List<USBEndpoint> endpoints_;

    public USBAlternateInterfaceImpl(int number, int classCode, int subclassCode, int protocolCode,
                                     List<USBEndpoint> endpoints) {
        number_ = number;
        classCode_ = classCode;
        subclassCode_ = subclassCode;
        protocolCode_ = protocolCode;
        endpoints_ = endpoints;
    }

    @Override
    public int number() {
        return number_;
    }

    @Override
    public int classCode() {
        return classCode_;
    }

    @Override
    public int subclassCode() {
        return subclassCode_;
    }

    @Override
    public int protocolCode() {
        return protocolCode_;
    }

    @Override
    public List<USBEndpoint> endpoints() {
        return Collections.unmodifiableList(endpoints_);
    }

    void addEndpoint(USBEndpoint endpoint) {
        endpoints_.add(endpoint);
    }

    @Override
    public USBEndpoint getEndpoint(int endpointNumber, USBDirection direction) {
        return endpoints_.stream().filter((ep) -> ep.number() == endpointNumber && ep.direction() == direction).findFirst().orElse(null);
    }
}
