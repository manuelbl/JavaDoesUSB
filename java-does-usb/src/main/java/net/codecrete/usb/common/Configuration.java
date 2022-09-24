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
    private final List<CompositeFunction> functions_;
    private final List<USBInterface> interfaces_;
    private final int configValue_;
    private final int attributes_;
    private final int maxPower_;

    public Configuration(int configValue, int attributes, int maxPower) {
        configValue_ = configValue;
        attributes_ = attributes;
        maxPower_ = maxPower;
        functions_ = new ArrayList<>();
        interfaces_ = new ArrayList<>();
    }

    public int configValue() {
        return configValue_;
    }

    public int attributes() {
        return attributes_;
    }

    public int maxPower() {
        return maxPower_;
    }

    public List<USBInterface> interfaces() {
        return interfaces_;
    }

    public List<CompositeFunction> functions() { return functions_; }

    public void addInterface(USBInterface intf) {
        interfaces_.add(intf);
    }

    public USBInterfaceImpl findInterfaceByNumber(int number) {
        return (USBInterfaceImpl) interfaces_.stream().filter((intf) -> intf.number() == number).findFirst().orElse(null);
    }

    public void addFunction(CompositeFunction function) {
        functions_.add(function);
    }

    public CompositeFunction findFunction(int interfaceNumber) {
        return functions_.stream()
                .filter((f) -> interfaceNumber >= f.firstInterfaceNumber()
                        && interfaceNumber < f.firstInterfaceNumber() + f.numInterfaces())
                .findFirst().orElse(null);
    }
}
