//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.MemorySegment;

/**
 * Handles for WinUSB devices and interfaces
 */
class InterfaceHandle {
    InterfaceHandle(int interfaceNumber, int firstInterfaceNumber) {
        this.interfaceNumber = interfaceNumber;
        this.firstInterfaceNumber = firstInterfaceNumber;
    }

    /**
     * The number of this interface.
     */
    final int interfaceNumber;
    /**
     * The number of the first interface in the same composite function.
     */
    final int firstInterfaceNumber;
    /**
     * The file handle of the device.
     * <p>
     * This is only used for the first interface in a composite function.
     * </p>
     */
    MemorySegment deviceHandle;
    /**
     * The WinUSB handle of the interface.
     */
    @SuppressWarnings("java:S1700")
    MemorySegment winusbHandle;
    /**
     * Count indicating how many interface depend on the device being open.
     */
    int deviceOpenCount;
}
