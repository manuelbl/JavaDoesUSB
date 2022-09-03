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

    private final int number_;
    private final USBDirection direction_;
    private final USBTransferType type_;
    private final int packetSize_;

    public USBEndpointImpl(int number, USBDirection direction, USBTransferType type, int packetSize) {
        number_ = number;
        direction_ = direction;
        type_ = type;
        packetSize_ = packetSize;
    }

    @Override
    public int number() {
        return number_;
    }

    @Override
    public USBDirection direction() {
        return direction_;
    }

    @Override
    public USBTransferType transferType() {
        return type_;
    }

    @Override
    public int packetSize() {
        return packetSize_;
    }
}
