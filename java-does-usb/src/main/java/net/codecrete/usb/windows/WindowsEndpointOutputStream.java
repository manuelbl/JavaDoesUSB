//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBDirection;
import net.codecrete.usb.common.AsyncIOCompletion;
import net.codecrete.usb.common.EndpointOutputStream;

import java.lang.foreign.MemorySegment;

public class WindowsEndpointOutputStream extends EndpointOutputStream {

    WindowsEndpointOutputStream(WindowsUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
        device.configureForAsyncIo(USBDirection.OUT, endpointNumber);
    }

    @Override
    protected void submitTransferOut(MemorySegment data, int dataSize, AsyncIOCompletion completion) {
        ((WindowsUSBDevice) device).submitTransferOut(endpointNumber, data, dataSize, completion);
    }

    @Override
    protected void throwException(int errorCode, String message) {
        WindowsUSBException.throwException(errorCode, message);
    }
}
