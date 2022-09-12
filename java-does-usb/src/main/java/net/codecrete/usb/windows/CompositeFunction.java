//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.MemoryAddress;

/**
 * Describes the function of an interface of a composite USB device.
 * <p>
 * A composite USB device can have multiple functions, e.g. a mass
 * storage function and a virtual serial port function. Each function
 * will appear as a separate device in Window.
 * </p>
 * <p>
 * A function consists of one or more interfaces. Functions with
 * multiple interfaces must have consecutive interface numbers. The
 * interfaces after the first one are called associated interfaces.
 * </p>
 * <p>
 * On Windows, each function has a separate device path. Each device
 * path must be opened and each interface must be opened.
 * Furthermore, the first and the associated interfaces are
 * treated differently.
 * </p>
 */
public class CompositeFunction {
    // TODO: implement associated interfaces

    // TODO: implement devices without interfaces

    private final int firstInterfaceNumber_;
    private final int numInterfaces_;
    private final String devicePath_;
    private MemoryAddress deviceHandle_;
    private MemoryAddress firstInterfaceHandle_;

    /**
     * Creates a new instance with a single interface.
     *
     * @param interfaceNumber the interface number
     * @param devicePath      the device path
     */
    public CompositeFunction(int interfaceNumber, String devicePath) {
        firstInterfaceNumber_ = interfaceNumber;
        numInterfaces_ = 1;
        devicePath_ = devicePath;
    }

    public int firstInterfaceNumber() {
        return firstInterfaceNumber_;
    }

    public int numInterfaces() {
        return numInterfaces_;
    }

    public String devicePath() {
        return devicePath_;
    }

    public MemoryAddress deviceHandle() {
        return deviceHandle_;
    }

    public void setDeviceHandle(MemoryAddress deviceHandle) {
        deviceHandle_ = deviceHandle;
    }

    public MemoryAddress firstInterfaceHandle() {
        return firstInterfaceHandle_;
    }

    public void setFirstInterfaceHandle(MemoryAddress firstInterfaceHandle) {
        firstInterfaceHandle_ = firstInterfaceHandle;
    }
}
