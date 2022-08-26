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
public interface USBDeviceRegistry {
    List<USBDeviceInfo> getAllDevices();

    void setOnDeviceConnected(Consumer<USBDeviceInfo> handler);

    void setOnDeviceDisconnected(Consumer<USBDeviceInfo> handler);
}
