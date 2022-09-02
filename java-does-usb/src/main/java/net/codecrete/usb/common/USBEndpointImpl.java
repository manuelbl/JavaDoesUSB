//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBEndpoint;
import net.codecrete.usb.USBEndpointType;

/**
 * Implementation of {@code USBEndpoint} interface.
 */
public class USBEndpointImpl implements USBEndpoint {

    private final int number;
    private final USBDirection direction;
    private final USBEndpointType type;
    private final int packetSize;

    public USBEndpointImpl(int number, USBDirection direction, USBEndpointType type, int packetSize) {
        this.number = number;
        this.direction = direction;
        this.type = type;
        this.packetSize = packetSize;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public USBDirection getDirection() {
        return direction;
    }

    @Override
    public USBEndpointType getType() {
        return type;
    }

    @Override
    public int getPacketSize() {
        return packetSize;
    }
}
