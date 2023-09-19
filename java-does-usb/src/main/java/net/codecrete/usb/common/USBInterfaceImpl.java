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

    private final int interfaceNumber;
    private USBAlternateInterface currentAlternate;
    private final List<USBAlternateInterface> alternateInterfaces;

    private boolean claimed;

    public USBInterfaceImpl(int number, List<USBAlternateInterface> alternates) {
        interfaceNumber = number;
        alternateInterfaces = alternates;
        currentAlternate = alternates.get(0);
    }

    @Override
    public int number() {
        return interfaceNumber;
    }

    @Override
    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    @Override
    public USBAlternateInterface alternate() {
        return currentAlternate;
    }

    @Override
    public USBAlternateInterface getAlternate(int alternateNumber) {
        return alternateInterfaces.stream().filter(alt -> alt.number() == alternateNumber).findFirst().orElse(null);
    }

    @Override
    public List<USBAlternateInterface> alternates() {
        return Collections.unmodifiableList(alternateInterfaces);
    }

    void addAlternate(USBAlternateInterface alt) {
        alternateInterfaces.add(alt);
    }

    public void setAlternate(USBAlternateInterface alternate) {
        currentAlternate = alternate;
    }
}
