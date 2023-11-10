//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a predicate (boolean-valued function) of one argument, evaluated for a given USB device.
 * <p>
 * This is a functional interface whose functional method is {@link #matches(UsbDevice)}.
 * </p>
 */
@FunctionalInterface
public interface UsbDevicePredicate {
    /**
     * Evaluates this predicate on the given USB device.
     *
     * @param device the USB device
     * @return {@code true} of the devices matches the predicate, otherwise {@code false}
     */
    boolean matches(@NotNull UsbDevice device);

    /**
     * Test if the USB devices matches any of the filter conditions.
     *
     * @param device  the USB device
     * @param predicates a list of filter predicates
     * @return {@code true} if it matches, {@code false} otherwise
     */
    static boolean matchesAny(@NotNull UsbDevice device, @NotNull List<UsbDevicePredicate> predicates) {
        return predicates.stream().anyMatch(predicate -> predicate.matches(device));
    }
}
