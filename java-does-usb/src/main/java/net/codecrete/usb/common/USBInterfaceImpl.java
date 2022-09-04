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

    private final int number_;
    private final USBAlternateInterface alternate_;
    private final List<USBAlternateInterface> alternates_;

    private boolean isClaimed_;

    public USBInterfaceImpl(int number, List<USBAlternateInterface> alternates) {
        number_ = number;
        alternates_ = alternates;
        alternate_ = alternates.get(0);
    }

    @Override
    public int number() {
        return number_;
    }

    @Override
    public boolean isClaimed() {
        return isClaimed_;
    }

    public void setClaimed(boolean claimed) {
        isClaimed_ = claimed;
    }

    @Override
    public USBAlternateInterface alternate() {
        return alternate_;
    }

    @Override
    public List<USBAlternateInterface> alternates() {
        return Collections.unmodifiableList(alternates_);
    }

    void addAlternate(USBAlternateInterface alt) {
        alternates_.add(alt);
    }
}
