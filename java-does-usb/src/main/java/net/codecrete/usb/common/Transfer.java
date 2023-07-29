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
    private MemorySegment data;
    private int dataSize;
    private int resultCode;
    private int resultSize;
    private TransferCompletion completion;

    /**
     * Gets the with data to transfer (in or out).
     *
     * @return buffer
     */
    public MemorySegment data() {
        return data;
    }

    /**
     * Sets buffer with data to transfer (in or out)
     *
     * @param data buffer
     */
    public void setData(MemorySegment data) {
        this.data = data;
    }

    /**
     * Gets length of data to transfer.
     *
     * @return length (in bytes)
     */
    public int dataSize() {
        return dataSize;
    }

    /**
     * Sets length of data to transfer.
     *
     * @param dataSize length (in bytes)
     */
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * Gets result code (operating system specific).
     * <p>
     * 0 represents success, all other values represent an error.
     * </p>
     *
     * @return result code
     */
    public int resultCode() {
        return resultCode;
    }

    /**
     * Sets result code (operating system specific).
     * <p>
     * 0 represents success, all other values represent an error.
     * </p>
     *
     * @param resultCode result code
     */
    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    /**
     * Gets length of transferred data.
     *
     * @return length (in bytes)
     */
    public int resultSize() {
        return resultSize;
    }

    /**
     * Sets length of transferred data.
     *
     * @param resultSize length (in bytes)
     */
    public void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

    /**
     * Gets completion handler to call when the transfer is complete.
     *
     * @return completion handler
     */
    public TransferCompletion completion() {
        return completion;
    }

    /**
     * Sets completion handler to call when the transfer is complete.
     *
     * @param completion completion handler
     */
    public void setCompletion(TransferCompletion completion) {
        this.completion = completion;
    }
}
