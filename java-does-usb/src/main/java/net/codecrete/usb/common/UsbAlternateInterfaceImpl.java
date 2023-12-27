//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbAlternateInterface;
import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbEndpoint;
import net.codecrete.usb.UsbException;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class UsbAlternateInterfaceImpl implements UsbAlternateInterface {

    private final int alternateInterfaceNumber;
    private final int alternateInterfaceClass;
    private final int alternateInterfaceSubclass;
    private final int alternateInterfaceProtocol;
    private final List<UsbEndpoint> endpointList;

    public UsbAlternateInterfaceImpl(int number, int classCode, int subclassCode, int protocolCode,
                                     List<UsbEndpoint> endpoints) {
        alternateInterfaceNumber = number;
        alternateInterfaceClass = classCode;
        alternateInterfaceSubclass = subclassCode;
        alternateInterfaceProtocol = protocolCode;
        endpointList = endpoints;
        endpointList.sort(Comparator.comparingInt(UsbEndpoint::getNumber));
    }

    @Override
    public int getNumber() {
        return alternateInterfaceNumber;
    }

    @Override
    public int getClassCode() {
        return alternateInterfaceClass;
    }

    @Override
    public int getSubclassCode() {
        return alternateInterfaceSubclass;
    }

    @Override
    public int getProtocolCode() {
        return alternateInterfaceProtocol;
    }

    @Override
    public @NotNull List<UsbEndpoint> getEndpoints() {
        return unmodifiableList(endpointList);
    }

    void addEndpoint(UsbEndpoint endpoint) {
        endpointList.add(endpoint);
    }

    @Override
    public @NotNull UsbEndpoint getEndpoint(int endpointNumber, UsbDirection direction) {
        return endpointList.stream()
                .filter(ep -> ep.getNumber() == endpointNumber && ep.getDirection() == direction).findFirst()
                .orElseThrow(() -> new UsbException(String.format("Endpoint %d (%s) does not exist", endpointNumber, direction)));
    }
}
