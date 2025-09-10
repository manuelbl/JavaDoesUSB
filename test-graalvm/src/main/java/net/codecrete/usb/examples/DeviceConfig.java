//
// Java Does USB
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Configuration information about test device
//

package net.codecrete.usb.examples;

import net.codecrete.usb.UsbDevice;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Test device configuration
 * @param vid vendor ID
 * @param pid product ID
 * @param isComposite indicates if this is the composite test device
 * @param interfaceNumber interface number for loopback and echo endpoints
 * @param endpointLoopbackOut loopback OUT endpoint number
 * @param endpointLoopbackIn loopback IN endpoint number
 * @param endpointEchoOut echo OUT endpoint number
 * @param endpointEchoIn echo IN endpoint number
 */
public record DeviceConfig(int vid, int pid,
                               boolean isComposite,
                               int interfaceNumber,
                               int endpointLoopbackOut, int endpointLoopbackIn,
                               int endpointEchoOut, int endpointEchoIn
) {

    private static final DeviceConfig LOOPBACK_DEVICE = new DeviceConfig(
            0xcafe,
            0xceaf,
            false,
            0,
            1,
            2,
            3,
            3
    );

    private static final DeviceConfig COMPOSITE_DEVICE = new DeviceConfig(
            0xcafe,
            0xcea0,
            true,
            3,
            1,
            2,
            -1,
            -1
    );


    /**
     * Gets the configuration fo the specified USB device.
     * @param device USB device
     * @return configuration, or empty if the USB device is not a test device
     */
    public static Optional<DeviceConfig> getConfig(UsbDevice device) {
        return Stream.of(LOOPBACK_DEVICE, COMPOSITE_DEVICE)
                .filter(config -> device.getVendorId() == config.vid() && device.getProductId() == config.pid())
                .findFirst();
    }
}
