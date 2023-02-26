//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.common.EndpointOutputStream;
import net.codecrete.usb.common.Transfer;

public class WindowsEndpointOutputStream extends EndpointOutputStream {

    WindowsEndpointOutputStream(WindowsUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferOut(Transfer request) {
        ((WindowsUSBDevice) device).submitTransferOut(endpointNumber, (WindowsTransfer) request);
    }

    @Override
    protected void configureEndpoint() {
        ((WindowsUSBDevice) device).configureForAsyncIo(USBDirection.OUT, endpointNumber);
    }
}
