//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.AsyncIOCompletion;
import net.codecrete.usb.common.EndpointOutputStream;

import java.lang.foreign.MemorySegment;

public class MacosEndpointOutputStream extends EndpointOutputStream {

    MacosEndpointOutputStream(MacosUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferOut(MemorySegment data, int dataSize, AsyncIOCompletion completion) {
        ((MacosUSBDevice) device).submitTransferOut(endpointNumber, data, dataSize, 0, completion);
    }

    @Override
    protected void throwException(int errorCode, String message) {
        MacosUSBException.throwException(errorCode, message);
    }
}
