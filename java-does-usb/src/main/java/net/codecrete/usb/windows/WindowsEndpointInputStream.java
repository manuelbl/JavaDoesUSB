//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.common.EndpointInputStream;
import net.codecrete.usb.common.Transfer;

public class WindowsEndpointInputStream extends EndpointInputStream {

    WindowsEndpointInputStream(WindowsUsbDevice device, int endpointNumber, int bufferSize) {
        super(device, endpointNumber, bufferSize);
    }

    @Override
    protected void submitTransferIn(Transfer transfer) {
        ((WindowsUsbDevice) device).submitTransferIn(endpointNumber, (WindowsTransfer) transfer);
    }

    @Override
    protected void configureEndpoint() {
        ((WindowsUsbDevice) device).configureForAsyncIo(UsbDirection.IN, endpointNumber);
    }
}
