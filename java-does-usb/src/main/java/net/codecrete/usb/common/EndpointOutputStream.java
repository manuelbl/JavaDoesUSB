//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDevice;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Output stream transmitting data to a USB bulk endpoint.
 * <p>
 * The output stream uses a buffer that fits a single data packet.
 * </p>
 */
public class EndpointOutputStream extends OutputStream {

    private USBDevice device_;
    private final int endpointNumber_;
    private byte[] buffer_;
    private int bufferedBytes_;

    EndpointOutputStream(USBDevice device, int endpointNumber, int packetSize) {
        device_ = device;
        endpointNumber_ = endpointNumber;
        buffer_ = new byte[packetSize];
    }

    @Override
    public void write(int b) throws IOException {
        if (device_ == null)
            throw new IOException("Bulk endpoint output stream has been closed");

        buffer_[bufferedBytes_] = (byte)b;
        bufferedBytes_ += 1;
        if (bufferedBytes_ == buffer_.length)
            flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (device_ == null)
            throw new IOException("Bulk endpoint output stream has been closed");

        while (len > 0) {
            int chunkSize = Math.min(len, buffer_.length - bufferedBytes_);
            System.arraycopy(b, off, buffer_, bufferedBytes_, chunkSize);
            bufferedBytes_ += chunkSize;
            off += chunkSize;
            len -= chunkSize;

            if (bufferedBytes_ == buffer_.length)
                flush();
        }
    }

    @Override
    public void flush() throws IOException {
        if (device_ == null)
            throw new IOException("Bulk endpoint output stream has been closed");

        if (bufferedBytes_ == 0)
            return;

        byte[] data;
        if (bufferedBytes_ < buffer_.length) {
            data = Arrays.copyOfRange(buffer_, 0, bufferedBytes_);
        } else {
            data = buffer_;
        }
        device_.transferOut(endpointNumber_, data);
        bufferedBytes_ = 0;
    }

    @Override
    public void close() throws IOException {
        flush();
        buffer_ = null;
        device_ = null;
    }
}
