//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.common.EndpointOutputStream;
import net.codecrete.usb.common.Transfer;

public class LinuxEndpointOutputStream extends EndpointOutputStream {

    LinuxEndpointOutputStream(LinuxUsbDevice device, int endpointNumber, int bufferSize) {
        super(device, endpointNumber, bufferSize);
    }

    @Override
    protected void submitTransferOut(Transfer transfer) {
        ((LinuxUsbDevice) device).submitTransfer(UsbDirection.OUT, endpointNumber, (LinuxTransfer) transfer);
    }
}
