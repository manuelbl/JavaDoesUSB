//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.UsbAlternateInterface;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbInterface;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UsbInterfaceImpl implements UsbInterface {

    private final int interfaceNumber;
    private UsbAlternateInterface currentAlternate;
    private final List<UsbAlternateInterface> alternateInterfaces;

    private boolean claimed;

    public UsbInterfaceImpl(int number, List<UsbAlternateInterface> alternates) {
        interfaceNumber = number;
        alternateInterfaces = alternates;
        currentAlternate = alternates.getFirst();
        alternateInterfaces.sort(Comparator.comparingInt(UsbAlternateInterface::getNumber));
    }

    @Override
    public int getNumber() {
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
    public @NotNull UsbAlternateInterface getCurrentAlternate() {
        return currentAlternate;
    }

    @Override
    public @NotNull UsbAlternateInterface getAlternate(int alternateNumber) {
        return alternateInterfaces.stream()
                .filter(alt -> alt.getNumber() == alternateNumber).findFirst()
                .orElseThrow(() -> new UsbException(String.format(
                        "Interface %d does not have an alternate interface setting %d",
                        interfaceNumber, alternateNumber)
                ));
    }

    @Override
    public @NotNull List<UsbAlternateInterface> getAlternates() {
        return Collections.unmodifiableList(alternateInterfaces);
    }

    void addAlternate(UsbAlternateInterface alt) {
        alternateInterfaces.add(alt);
    }

    public void setAlternate(UsbAlternateInterface alternate) {
        currentAlternate = alternate;
    }
}
