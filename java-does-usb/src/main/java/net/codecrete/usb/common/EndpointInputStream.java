//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDevice;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream receiving data from a USB bulk endpoint.
 * <p>
 * The input stream tries to receive a packet at a time.
 * </p>
 */
public class EndpointInputStream extends InputStream {

    private USBDevice device_;
    private final int endpointNumber_;
    private byte[] packet_;
    private int readOffset_;

    EndpointInputStream(USBDevice device, int endpointNumber) {
        device_ = device;
        endpointNumber_ = endpointNumber;
    }

    @Override
    public int read() throws IOException {
        if (device_ == null)
            throw new IOException("Bulk endpoint input stream has been closed");

        if (available() == 0)
            receiveMoreData();

        int b = packet_[readOffset_] & 0xff;
        readOffset_ += 1;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (device_ == null)
            throw new IOException("Bulk endpoint input stream has been closed");

        if (available() == 0)
            receiveMoreData();

        int n = Math.min(len, packet_.length - readOffset_);
        System.arraycopy(packet_, readOffset_, b, off, n);
        readOffset_ += n;

        return n;
    }

    @Override
    public int available() throws IOException {
        return (packet_ != null ? packet_.length : 0) - readOffset_;
    }

    private void receiveMoreData() throws IOException {
        if (device_ == null)
            throw new IOException("Bulk endpoint input stream has been closed");

        readOffset_ = 0;
        // skip zero-length packets
        do {
            packet_ = device_.transferIn(endpointNumber_);
        } while (packet_.length == 0);
    }

    @Override
    public void close() throws IOException {
        device_ = null;
        packet_ = null;
    }
}
