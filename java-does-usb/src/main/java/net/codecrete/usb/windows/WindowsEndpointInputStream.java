//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.common.EndpointInputStream;
import net.codecrete.usb.common.Transfer;

public class WindowsEndpointInputStream extends EndpointInputStream {

    WindowsEndpointInputStream(WindowsUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferIn(Transfer transfer) {
        ((WindowsUSBDevice) device).submitTransferIn(endpointNumber, (WindowsTransfer) transfer);
    }

    @Override
    protected void configureEndpoint() {
        ((WindowsUSBDevice) device).configureForAsyncIo(USBDirection.IN, endpointNumber);
    }
}
