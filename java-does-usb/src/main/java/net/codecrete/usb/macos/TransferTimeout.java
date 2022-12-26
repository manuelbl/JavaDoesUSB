//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.codecrete.usb.macos.MacosUSBException.throwException;

/**
 * Manages the timeout for a USB transfer.
 * <p>
 * Not used for bulk transfers as IOKit offers suitable functions ({@code WritePipeTO()}, {@code ReadPipeTO()}).
 * </p>
 */
class TransferTimeout {
    /**
     * Schedules a timeout for the specified endpoint.
     * <p>
     * If the timeout expires, the transfer is aborted.
     * </p>
     *
     * @param endpointInfo endpoint information
     * @param timeout      timeout, in milliseconds
     */
    TransferTimeout(MacosUSBDevice.EndpointInfo endpointInfo, int timeout) {
        this.endpointInfo = endpointInfo;
        future = getScheduledExecutorService().schedule(this::abort, timeout, TimeUnit.MILLISECONDS);
    }

    private synchronized void abort() {
        if (completed)
            return;
        int ret = IoKitUSB.AbortPipe(endpointInfo.segment(), endpointInfo.pipeIndex());
        if (ret != 0)
            throwException(ret, "Failed to abort USB transfer after timeout");
    }

    /**
     * Marks the transfer as completed and cancels the timeout.
     */
    synchronized void markCompleted() {
        if (completed)
            return;
        completed = true;
        future.cancel(false);
    }

    static synchronized ScheduledExecutorService getScheduledExecutorService() {
        if (scheduledExecutorService == null)
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
        return scheduledExecutorService;
    }

    private final ScheduledFuture<?> future;
    private final MacosUSBDevice.EndpointInfo endpointInfo;
    private boolean completed;

    private static ScheduledExecutorService scheduledExecutorService;
}
