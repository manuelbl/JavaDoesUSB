//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDeviceInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * USB device registry.
 */
public abstract class USBDeviceRegistry {
    protected Consumer<USBDeviceInfo> onDeviceConnectedHandler;
    protected Consumer<USBDeviceInfo> onDeviceDisconnectedHandler;

    public abstract List<USBDeviceInfo> getAllDevices();

    public void setOnDeviceConnected(Consumer<USBDeviceInfo> handler) {
        onDeviceConnectedHandler = handler;
    }

    public void setOnDeviceDisconnected(Consumer<USBDeviceInfo> handler) {
        onDeviceDisconnectedHandler = handler;
    }

    protected void emitOnDeviceConnected(USBDeviceInfo device) {
        if (onDeviceConnectedHandler != null)
            onDeviceConnectedHandler.accept(device);
    }

    protected void emitOnDeviceDisconnected(USBDeviceInfo device) {
        if (onDeviceDisconnectedHandler != null)
            onDeviceDisconnectedHandler.accept(device);
    }
}
