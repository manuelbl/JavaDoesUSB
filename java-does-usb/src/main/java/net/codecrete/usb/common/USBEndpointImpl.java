//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBEndpoint;
import net.codecrete.usb.USBTransferType;

/**
 * Implementation of {@code USBEndpoint} interface.
 */
public class USBEndpointImpl implements USBEndpoint {

    private final int endpointNumber;
    private final USBDirection transferDirection;
    private final USBTransferType type;
    private final int maxPacketSize;

    public USBEndpointImpl(int number, USBDirection direction, USBTransferType type, int packetSize) {
        endpointNumber = number;
        transferDirection = direction;
        this.type = type;
        maxPacketSize = packetSize;
    }

    @Override
    public int number() {
        return endpointNumber;
    }

    @Override
    public USBDirection direction() {
        return transferDirection;
    }

    @Override
    public USBTransferType transferType() {
        return type;
    }

    @Override
    public int packetSize() {
        return maxPacketSize;
    }
}
