//
// Java Does USB
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import java.io.IOException;

public class CommunicationException extends RuntimeException {

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(IOException cause) {
        super("Error in communication with USB device", cause);
    }
}
