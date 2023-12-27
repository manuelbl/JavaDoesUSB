//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.common.EndpointOutputStream;
import net.codecrete.usb.common.Transfer;

public class WindowsEndpointOutputStream extends EndpointOutputStream {

    WindowsEndpointOutputStream(WindowsUsbDevice device, int endpointNumber, int bufferSize) {
        super(device, endpointNumber, bufferSize);
    }

    @Override
    protected void submitTransferOut(Transfer request) {
        ((WindowsUsbDevice) device).submitTransferOut(endpointNumber, (WindowsTransfer) request);
    }

    @Override
    protected void configureEndpoint() {
        ((WindowsUsbDevice) device).configureForAsyncIo(UsbDirection.OUT, endpointNumber);
    }
}
