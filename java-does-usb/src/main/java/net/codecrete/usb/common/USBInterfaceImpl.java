//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBAlternateInterface;
import net.codecrete.usb.USBInterface;

import java.util.Collections;
import java.util.List;

public class USBInterfaceImpl implements USBInterface {

    private int number;
    private USBAlternateInterface alternate;
    private List<USBAlternateInterface> alternates;

    public USBInterfaceImpl(int number, List<USBAlternateInterface> alternates) {
        this.number = number;
        this.alternates = alternates;
        this.alternate = alternates.get(0);
    }

    @Override
    public int getNumber() {
        return 0;
    }

    @Override
    public boolean isClaimed() {
        return false;
    }

    @Override
    public USBAlternateInterface getAlternate() {
        return alternate;
    }

    @Override
    public List<USBAlternateInterface> getAlternates() {
        return Collections.unmodifiableList(alternates);
    }

    void addAlternate(USBAlternateInterface alt) {
        alternates.add(alt);
    }
}
