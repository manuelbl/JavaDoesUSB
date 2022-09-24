//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

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
 */
public class CompositeFunction {
    private final int firstInterfaceNumber_;
    private final int numInterfaces_;
    private final int classCode_;
    private final int subclassCode_;
    private final int protocolCode_;

    /**
     * Creates a new instance.
     *
     * @param firstInterfaceNumber the number of the first interface
     * @param numInterfaces        the number of interfaces
     * @param classCode            the function class
     * @param subclassCode         the function subclass
     * @param protocolCode         the function protocol
     */
    public CompositeFunction(int firstInterfaceNumber, int numInterfaces, int classCode, int subclassCode, int protocolCode) {
        firstInterfaceNumber_ = firstInterfaceNumber;
        numInterfaces_ = numInterfaces;
        classCode_ = classCode;
        subclassCode_ = subclassCode;
        protocolCode_ = protocolCode;
    }

    public int firstInterfaceNumber() {
        return firstInterfaceNumber_;
    }

    public int numInterfaces() {
        return numInterfaces_;
    }

    public int classCode() {
        return classCode_;
    }

    public int subclassCode() {
        return subclassCode_;
    }

    public int protocolCode() {
        return protocolCode_;
    }
}
