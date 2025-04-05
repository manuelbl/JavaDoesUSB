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

    /**
     * Gets the number of the first interface contained in this function.
     * @return the interface number
     */
    public int firstInterfaceNumber() {
        return firstIntfNumber;
    }

    /**
     * Gets the number of interfaces contained in this function.
     * @return the number of interfaces
     */
    public int numInterfaces() {
        return interfaceCount;
    }

    /**
     * Indicates if this function contains the specified interface.
     * @param interfaceNumber the interface number
     * @return {@code true} if it is contained, {@code false} otherwise
     */
    public boolean containsInterface(int interfaceNumber) {
        return interfaceNumber >= firstIntfNumber && interfaceNumber < firstIntfNumber + interfaceCount;
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
