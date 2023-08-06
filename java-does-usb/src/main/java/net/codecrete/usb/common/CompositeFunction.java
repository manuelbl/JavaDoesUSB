//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

/**
 * Describes a function of a composite USB device.
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
    private final int firstIntfNumber;
    private final int interfaceCount;
    private final int functionCode;
    private final int functionSubclass;
    private final int functionProtocol;

    /**
     * Creates a new instance.
     *
     * @param firstInterfaceNumber the number of the first interface
     * @param numInterfaces        the number of interfaces
     * @param classCode            the function class
     * @param subclassCode         the function subclass
     * @param protocolCode         the function protocol
     */
    public CompositeFunction(int firstInterfaceNumber, int numInterfaces, int classCode, int subclassCode,
                             int protocolCode) {
        firstIntfNumber = firstInterfaceNumber;
        interfaceCount = numInterfaces;
        functionCode = classCode;
        functionSubclass = subclassCode;
        functionProtocol = protocolCode;
    }

    public int firstInterfaceNumber() {
        return firstIntfNumber;
    }

    public int numInterfaces() {
        return interfaceCount;
    }

    public int classCode() {
        return functionCode;
    }

    public int subclassCode() {
        return functionSubclass;
    }

    public int protocolCode() {
        return functionProtocol;
    }
}
