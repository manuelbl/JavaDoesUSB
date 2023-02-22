//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.common.AsyncIOCompletion;
import net.codecrete.usb.common.EndpointInputStream;

import java.lang.foreign.MemorySegment;

public class LinuxEndpointInputStream extends EndpointInputStream {

    LinuxEndpointInputStream(LinuxUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferIn(MemorySegment buffer, int bufferSize, AsyncIOCompletion completion) {
        ((LinuxUSBDevice) device).submitTransferIn(endpointNumber, buffer, bufferSize, completion);
    }

    @Override
    protected void throwException(int errorCode, String message) {
        LinuxUSBException.throwException(errorCode, message);
    }
}
