//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

/**
 * Functional interface used to notify when an asynchronous USB transfer operation has completed.
 */
@FunctionalInterface
public interface TransferCompletion {

    /**
     * Called when the asynchronous transfer has completed.
     * <p>
     * When this function has been called, {@link Transfer#resultSize()} and
     * {@link Transfer#resultSize()} have been filled in.
     * </p>
     * <p>
     * Since there is a single background task calling all completion functions,
     * this function should only execute little work and return quickly.
     * </p>
     *
     * @param transfer completed transfer request
     */
    void completed(Transfer transfer);
}
