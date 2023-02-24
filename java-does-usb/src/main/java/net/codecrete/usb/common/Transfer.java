//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.lang.foreign.MemorySegment;

/**
 * Asynchronous USB endpoint transfer.
 */
public class Transfer {
    /**
     * Buffer with data to transfer (in or out)
     */
    public MemorySegment data;
    /**
     * Length of data to transfer (in bytes)
     */
    public int dataSize;
    /**
     * Result code (operating system specific).
     * <p>
     * 0 represents success, all other values represent an error.
     * </p>
     */
    public int resultCode;
    /**
     * Length of transferred data (in bytes)
     */
    public int resultSize;
    /**
     * Completion handler to call when the transfer is complete.
     */
    public TransferCompletion completionHandler;
}
