//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a device configuration.
 */
public class Configuration {
    private final List<CompositeFunction> functionList;
    private final List<USBInterface> interfaceList;
    private final int configuration;
    private final int configurationAttributes;
    private final int configurationMaxPower;

    public Configuration(int configValue, int attributes, int maxPower) {
        configuration = configValue;
        configurationAttributes = attributes;
        configurationMaxPower = maxPower;
        functionList = new ArrayList<>();
        interfaceList = new ArrayList<>();
    }

    public int configValue() {
        return configuration;
    }

    public int attributes() {
        return configurationAttributes;
    }

    public int maxPower() {
        return configurationMaxPower;
    }

    public List<USBInterface> interfaces() {
        return interfaceList;
    }

    public List<CompositeFunction> functions() {
        return functionList;
    }

    public void addInterface(USBInterface intf) {
        interfaceList.add(intf);
    }

    public USBInterfaceImpl findInterfaceByNumber(int number) {
        return (USBInterfaceImpl) interfaceList.stream().filter((intf) -> intf.number() == number).findFirst().orElse(null);
    }

    public void addFunction(CompositeFunction function) {
        functionList.add(function);
    }

    public CompositeFunction findFunction(int interfaceNumber) {
        return functionList.stream().filter((f) -> interfaceNumber >= f.firstInterfaceNumber() && interfaceNumber < f.firstInterfaceNumber() + f.numInterfaces()).findFirst().orElse(null);
    }
}
