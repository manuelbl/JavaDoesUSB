//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbEndpoint;
import net.codecrete.usb.UsbTransferType;

/**
 * Implementation of {@code UsbEndpoint} interface.
 */
public class UsbEndpointImpl implements UsbEndpoint {

    private final int endpointNumber;
    private final UsbDirection transferDirection;
    private final UsbTransferType type;
    private final int maxPacketSize;

    public UsbEndpointImpl(int number, UsbDirection direction, UsbTransferType type, int packetSize) {
        endpointNumber = number;
        transferDirection = direction;
        this.type = type;
        maxPacketSize = packetSize;
    }

    @Override
    public int getNumber() {
        return endpointNumber;
    }

    @Override
    public UsbDirection getDirection() {
        return transferDirection;
    }

    @Override
    public UsbTransferType getTransferType() {
        return type;
    }

    @Override
    public int getPacketSize() {
        return maxPacketSize;
    }
}
