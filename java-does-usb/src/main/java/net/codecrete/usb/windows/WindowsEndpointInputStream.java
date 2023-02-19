//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.common.AsyncEndpointInputStream;
import net.codecrete.usb.common.AsyncIOCompletion;

import java.lang.foreign.MemorySegment;

public class WindowsEndpointInputStream extends AsyncEndpointInputStream {

    WindowsEndpointInputStream(WindowsUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
        device.configureForAsyncIo(USBDirection.IN, endpointNumber);
    }

    @Override
    protected void submitTransferIn(MemorySegment buffer, int bufferSize, AsyncIOCompletion completion) {
        ((WindowsUSBDevice)device).submitTransferIn(endpointNumber, buffer, bufferSize, completion);
    }

    @Override
    protected void throwException(int errorCode, String message) {
        WindowsUSBException.throwException(errorCode, message);
    }
}
