//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.EndpointInputStream;
import net.codecrete.usb.common.Transfer;

public class MacosEndpointInputStream extends EndpointInputStream {

    MacosEndpointInputStream(MacosUsbDevice device, int endpointNumber, int bufferSize) {
        super(device, endpointNumber, bufferSize);
    }

    @Override
    protected void submitTransferIn(Transfer transfer) {
        ((MacosUsbDevice) device).submitTransferIn(endpointNumber, (MacosTransfer) transfer, 0);
    }
}
