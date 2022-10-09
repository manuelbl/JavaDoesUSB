//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

/**
 * DFU device status.
 * <p>
 * See USB Device Class Specification for Device Firmware Upgrade, version 1.1.
 * </p>
 */
public enum DeviceStatus {

    OK, // No error condition is present
    ERR_TARGET, // File is not targeted for use by this device.
    ERR_FILE, // File is for this device but fails some vendor-specific verification test.
    ERR_WRITE, // Device is unable to write memory.
    ERR_ERASE, // Memory erase function failed.
    ERR_CHECK_ERASED, // Memory erase check failed.
    ERR_PROG, // Program memory function failed.
    ERR_VERIFY, // Programmed memory failed verification.
    ERR_ADDRESS, // Cannot program memory due to received address that is out of range.
    ERR_NOTDONE, // Received DFU_DNLOAD with wLength = 0, but device does not think it has all of the data yet.
    ERR_FIRMWARE, // Device’s firmware is corrupt. It cannot return to run-time (non-DFU) operations.
    ERR_VENDOR, // iString indicates a vendor-specific error.
    ERR_USBR, // Device detected unexpected USB reset signaling.
    ERR_POR, // Device detected unexpected power on reset.
    ERR_UNKNOWN, // Something went wrong, but the device does not know what it was.
    ERR_STALLEDPKT; // Device stalled an unexpected request.

    public byte value() {
        return (byte) ordinal();
    }

    private static final DeviceStatus[] values = values();
    
    public static DeviceStatus fromValue(byte value) {
        return values[value];
    }
}
