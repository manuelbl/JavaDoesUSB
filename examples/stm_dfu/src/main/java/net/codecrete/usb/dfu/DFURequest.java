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
 * See ST Microelectronics, application note AN3156.
 * </p>
 */
public enum DFURequest {
    /**
     * Requests the device to leave DFU mode and enter the application.
     * <p>
     * The Detach request is not meaningful in the case of the bootloader. The bootloader starts
     * with a system reset depending on the boot mode configuration settings, which means that
     * no other application is running at that time.
     * </p>
     */
    DETACH,
    /**
     * Requests data transfer from Host to the device in order to load them
     * into device internal flash memory. Includes also erase commands.
     */
    DOWNLOAD,
    /**
     * Requests data transfer from device to Host in order to load content
     * of device internal flash memory into a Host file.
     */
    UPLOAD,
    /**
     * Requests device to send status report to the Host (including status
     * resulting from the last request execution and the state the device
     * enters immediately after this request).
     */
    GET_STATUS,
    /**
     * Requests device to clear error status and move to next step.
     */
    CLEAR_STATUS,
    /**
     * Requests the device to send only the state it enters immediately
     * after this request.
     */
    GET_STATE,
    /**
     * Requests device to exit the current state/operation and enter idle
     * state immediately
     */
    ABORT
}
