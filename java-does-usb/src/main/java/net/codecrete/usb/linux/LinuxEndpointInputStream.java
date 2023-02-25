//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.common.EndpointInputStream;
import net.codecrete.usb.common.Transfer;

public class LinuxEndpointInputStream extends EndpointInputStream {

    LinuxEndpointInputStream(LinuxUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferIn(Transfer transfer) {
        ((LinuxUSBDevice) device).submitTransfer(USBDirection.IN, endpointNumber, (LinuxTransfer) transfer);
    }
}
