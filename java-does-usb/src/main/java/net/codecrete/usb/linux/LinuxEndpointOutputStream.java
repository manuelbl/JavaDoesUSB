//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.common.EndpointOutputStream;
import net.codecrete.usb.common.Transfer;

public class LinuxEndpointOutputStream extends EndpointOutputStream {

    LinuxEndpointOutputStream(LinuxUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferOut(Transfer transfer) {
        ((LinuxUSBDevice) device).submitTransferOut(endpointNumber, (LinuxTransfer) transfer);
    }
}
