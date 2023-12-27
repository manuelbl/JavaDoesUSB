//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.common.EndpointInputStream;
import net.codecrete.usb.common.Transfer;

public class LinuxEndpointInputStream extends EndpointInputStream {

    LinuxEndpointInputStream(LinuxUsbDevice device, int endpointNumber, int bufferSize) {
        super(device, endpointNumber, bufferSize);
    }

    @Override
    protected void submitTransferIn(Transfer transfer) {
        ((LinuxUsbDevice) device).submitTransfer(UsbDirection.IN, endpointNumber, (LinuxTransfer) transfer);
    }
}
