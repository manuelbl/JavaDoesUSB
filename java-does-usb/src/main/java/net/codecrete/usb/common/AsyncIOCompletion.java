//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

/**
 * Lambda function called when an asynchronous IO operation has completed.
 */
@FunctionalInterface
public interface AsyncIOCompletion {

    /**
     * Called when the asynchronous IO has completed.
     * <p>
     * A result code of 0 indicates a successful operation. Other codes
     * indicate a failure. The code is operating system specific.
     * </p>
     *
     * @param result the native result code of the operation.
     * @param size the size of the transferred data (in bytes)
     */
    void completed(int result, int size);
}
