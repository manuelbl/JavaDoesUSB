//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.EndpointOutputStream;
import net.codecrete.usb.common.Transfer;

public class MacosEndpointOutputStream extends EndpointOutputStream {

    MacosEndpointOutputStream(MacosUSBDevice device, int endpointNumber, int bufferSize) {
        super(device, endpointNumber, bufferSize);
    }

    @Override
    protected void submitTransferOut(Transfer request) {
        ((MacosUSBDevice) device).submitTransferOut(endpointNumber, (MacosTransfer) request, 0);
    }
}
