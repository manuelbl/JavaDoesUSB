//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.common.AsyncIOCompletion;
import net.codecrete.usb.common.EndpointOutputStream;

import java.lang.foreign.MemorySegment;

public class LinuxEndpointOutputStream extends EndpointOutputStream {

    LinuxEndpointOutputStream(LinuxUSBDevice device, int endpointNumber) {
        super(device, endpointNumber);
    }

    @Override
    protected void submitTransferOut(MemorySegment data, int dataSize, AsyncIOCompletion completion) {
        ((LinuxUSBDevice) device).submitTransferOut(endpointNumber, data, dataSize, completion);
    }

    @Override
    protected void throwException(int errorCode, String message) {
        LinuxUSBException.throwException(errorCode, message);
    }
}
