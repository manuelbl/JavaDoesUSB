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

    /**
     * No error condition is present
     */
    OK,
    /**
     * File is not targeted for use by this device.
     */
    ERR_TARGET,
    /**
     * File is for this device but fails some vendor-specific verification test.
     */
    ERR_FILE,
    /**
     * Device is unable to write memory.
     */
    ERR_WRITE,
    /**
     * Memory erase function failed.
     */
    ERR_ERASE,
    /**
     * Memory erase check failed.
     */
    ERR_CHECK_ERASED,
    /**
     * Program memory function failed.
     */
    ERR_PROG,
    /**
     * Programmed memory failed verification.
     */
    ERR_VERIFY,
    /**
     * Cannot program memory due to received address that is out of range.
     */
    ERR_ADDRESS,
    /**
     * Received DFU_DNLOAD with wLength = 0, but device does not think it has all of the data yet.
     */
    ERR_NOTDONE,
    /**
     * Device’s firmware is corrupt. It cannot return to run-time (non-DFU) operations.
     */
    ERR_FIRMWARE,
    /**
     * iString indicates a vendor-specific error.
     */
    ERR_VENDOR,
    /**
     * Device detected unexpected USB reset signaling.
     */
    ERR_USBR,
    /**
     * Device detected unexpected power on reset.
     */
    ERR_POR,
    /**
     * Something went wrong, but the device does not know what it was.
     */
    ERR_UNKNOWN,
    /**
     * Device stalled an unexpected request.
     */
    ERR_STALLEDPKT;

    public static DeviceStatus fromValue(byte value) {
        return values()[value];
    }
}
