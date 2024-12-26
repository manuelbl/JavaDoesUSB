//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

/**
 * DFU GET_STATUS response.
 * <p>
 * See USB Device Class Specification for Device Firmware Upgrade, version 1.1.
 * </p>
 */
public record DFUStatus(DeviceStatus status, int pollTimeout, DeviceState state) {

    public static DFUStatus fromBytes(byte[] data) {
        var status = DeviceStatus.fromValue(data[0]);
        var pollTimeout = (data[1] & 0xff) + 256 * (data[2] & 0xff) + 256 * 256 * (data[3] & 0xff);
        var state = DeviceState.fromValue(data[4]);
        return new DFUStatus(status, pollTimeout, state);
    }
}
