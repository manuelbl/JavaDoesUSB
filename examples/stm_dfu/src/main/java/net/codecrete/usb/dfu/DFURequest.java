//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

/**
 * DFU request.
 * <p>
 * See USB Device Class Specification for Device Firmware Upgrade, version 1.1.
 * </p>
 */
public enum DFURequest {
    DETACH,
    DOWNLOAD,
    UPLOAD,
    GET_STATUS,
    CLEAR_STATUS,
    GET_STATE,
    ABORT;

    public int value() {
        return ordinal();
    }
}
