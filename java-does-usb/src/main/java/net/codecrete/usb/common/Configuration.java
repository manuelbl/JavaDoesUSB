//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a device configuration.
 */
public class Configuration {
    private final List<CompositeFunction> functionList;
    private final List<UsbInterface> interfaceList;
    private final int configurationValue;
    private final int configurationAttributes;
    private final int configurationMaxPower;

    public Configuration(int configValue, int attributes, int maxPower) {
        configurationValue = configValue;
        configurationAttributes = attributes;
        configurationMaxPower = maxPower;
        functionList = new ArrayList<>();
        interfaceList = new ArrayList<>();
    }

    public int configValue() {
        return configurationValue;
    }

    public int attributes() {
        return configurationAttributes;
    }

    public int maxPower() {
        return configurationMaxPower;
    }

    public List<UsbInterface> interfaces() {
        return interfaceList;
    }

    public List<CompositeFunction> functions() {
        return functionList;
    }

    public void addInterface(UsbInterface intf) {
        interfaceList.add(intf);
    }

    public UsbInterfaceImpl findInterfaceByNumber(int number) {
        return (UsbInterfaceImpl) interfaceList.stream().filter(intf -> intf.getNumber() == number).findFirst().orElse(null);
    }

    public void addFunction(CompositeFunction function) {
        functionList.add(function);
    }

    public CompositeFunction findFunction(int interfaceNumber) {
        return functionList.stream().filter(f -> interfaceNumber >= f.firstInterfaceNumber() && interfaceNumber < f.firstInterfaceNumber() + f.numInterfaces()).findFirst().orElse(null);
    }
}
