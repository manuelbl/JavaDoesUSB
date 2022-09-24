//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.MemoryAddress;

/**
 * Handles for WinUSB devices and interfaces
 */
class InterfaceHandle {
    /**
     * The number of this interface.
     */
    int interfaceNumber;
    /**
     * The number of the first interface in the same composite function.
     */
    int firstInterfaceNumber;
    /**
     * The device path.
     * <p>
     * This is only set for the first interface in a composite function.
     * </p>
     */
    String devicePath;
    /**
     * The file handle of the device.
     * <p>
     * This is only used for the first interface in a composite function.
     * </p>
     */
    MemoryAddress deviceHandle;
    /**
     * The WinUSB handle of the interface.
     */
    MemoryAddress interfaceHandle;
    /**
     * Count indicating how many interface depend on the device being open.
     */
    int deviceOpenCount;
}
