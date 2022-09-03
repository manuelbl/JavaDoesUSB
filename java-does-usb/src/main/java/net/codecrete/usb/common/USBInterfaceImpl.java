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

    private int number_;
    private USBAlternateInterface alternate_;
    private List<USBAlternateInterface> alternates_;

    public USBInterfaceImpl(int number, List<USBAlternateInterface> alternates) {
        number_ = number;
        alternates_ = alternates;
        alternate_ = alternates.get(0);
    }

    @Override
    public int number() {
        return 0;
    }

    @Override
    public boolean isClaimed() {
        return false;
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
