//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.AsyncEndpointInputStream;
import net.codecrete.usb.common.AsyncIOCompletion;

import java.lang.foreign.MemorySegment;

public class MacosEndpointInputStream extends AsyncEndpointInputStream {

    MacosEndpointInputStream(MacosUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferIn(MemorySegment buffer, int bufferSize, AsyncIOCompletion completion) {
        ((MacosUSBDevice) device).submitTransferIn(endpointNumber, buffer, bufferSize, 0, completion);
    }

    @Override
    protected void throwException(int errorCode, String message) {
        MacosUSBException.throwException(errorCode, message);
    }
}
