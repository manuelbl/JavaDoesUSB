//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBTransferType;
import net.codecrete.usb.linux.gen.errno.errno;
import net.codecrete.usb.linux.gen.poll.poll;
import net.codecrete.usb.linux.gen.poll.pollfd;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_urb;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static net.codecrete.usb.linux.LinuxUSBException.throwException;
import static net.codecrete.usb.linux.LinuxUSBException.throwLastError;
import static net.codecrete.usb.linux.USBDevFS.*;
import static net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs.*;

/**
 * Background task for handling asynchronous transfers.
 * <p>
 * Each USB device must register its file handle with this task.
 * </p>
 * <p>
 * The task keeps track of the submitted transfers by indexing them
 * by URB address (USB request block).
 * </p>
 * <p>
 * URBs are allocated but never freed. To limit the memory usage,
 * URBs are reused. So the maximum number of outstanding transfers
 * determines the number of allocated URBs.
 * </p>
 */
public class LinuxAsyncTask {
    private static LinuxAsyncTask singletonInstance;

    /**
     * Singleton instance of background task.
     *
     * @return background task
     */
    static synchronized LinuxAsyncTask instance() {
        if (singletonInstance == null)
            singletonInstance = new LinuxAsyncTask();
        return singletonInstance;
    }

    private final Arena urbArena = Arena.openShared();
    /// available URBs
    private final List<MemorySegment> availableURBs = new ArrayList<>();
    /// map of URB addresses to transfer (for outstanding transfers)
    private final Map<MemorySegment, LinuxTransfer> transfersByURB = new LinkedHashMap<>();
    /// array of file descriptors using asynchronous completion
    private int[] asyncFds;
    /// file descriptor to notify async IO background thread about an update
    private int asyncIOWakeUpEventFd;

    /**
     * Background task for handling asynchronous IO completions.
     * <p>
     * It polls on all registered file descriptors. If a file descriptor is
     * ready, the URB is "reaped".
     * </p>
     * <p>
     * Using an additional {@code eventfd} file descriptor, this background task
     * can be woken up to refresh the list of polled file descriptors.
     * </p>
     */
    private void asyncCompletionTask() {

        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            var asyncPolls = pollfd.allocateArray(100, arena);
            var urbPointerHolder = arena.allocate(ADDRESS);
            var eventfdValueHolder = arena.allocate(JAVA_LONG);

            while (true) {

                // get current file descriptor array
                int[] fds;
                synchronized (this) {
                    fds = asyncFds;
                }

                // prepare pollfd struct array
                var n = fds.length;
                for (int i = 0; i < n; i++) {
                    pollfd.fd$set(asyncPolls, i, fds[i]);
                    pollfd.events$set(asyncPolls, i, (short) poll.POLLOUT());
                    pollfd.revents$set(asyncPolls, i, (short) 0);
                }

                // entry n is the wake-up event file descriptor
                pollfd.fd$set(asyncPolls, n, asyncIOWakeUpEventFd);
                pollfd.events$set(asyncPolls, n, (short) poll.POLLIN());
                pollfd.revents$set(asyncPolls, n, (short) 0);

                // poll for event
                int res = poll.poll(asyncPolls, n + 1, -1);
                if (res < 0)
                    throwException("internal error (poll)");

                // acquire lock
                synchronized (this) {

                    if ((pollfd.revents$get(asyncPolls, n) & poll.POLLIN()) != 0) {
                        // wakeup to refresh list of file descriptors
                        res = IO.eventfd_read(asyncIOWakeUpEventFd, eventfdValueHolder, errnoState);
                        if (res < 0)
                            throwLastError(errnoState, "internal error (eventfd_read)");
                        continue;
                    }

                    // check for events
                    for (int i = 0; i < n + 1; i++) {
                        var revent = pollfd.revents$get(asyncPolls, i);
                        if (revent == 0)
                            continue;

                        if ((revent & poll.POLLERR()) != 0) {
                            // most likely the device has been disconnected;
                            // remove from polled FD list to prevent further problems
                            int fd = pollfd.fd$get(asyncPolls, i);
                            removeFdFromAsyncIOCompletion(fd);
                            continue;
                        }

                        // reap URB
                        int fd = pollfd.fd$get(asyncPolls, i);
                        reapURBs(fd, urbPointerHolder, errnoState);
                    }
                }
            }
        }
    }

    /**
     * Reap all pending URBs and handle the completed transfers.
     *
     * @param fd               file descriptor
     * @param urbPointerHolder native memory to receive the URB pointer
     * @param errnoState       native memory to receive the errno
     */
    private void reapURBs(int fd, MemorySegment urbPointerHolder, MemorySegment errnoState) {
        while (true) {
            int res = IO.ioctl(fd, REAPURBNDELAY, urbPointerHolder, errnoState);
            if (res < 0) {
                var err = Linux.getErrno(errnoState);
                if (err == errno.EAGAIN())
                    return; // no more pending URBs
                if (err == errno.ENODEV())
                    return; // ignore, device might have been closed
                throwException(err, "internal error (reap URB)");
            }

            // call completion handler
            var urb = urbPointerHolder.get(ADDRESS, 0);
            var transfer = getTransferResult(urb);
            transfer.completion.completed(transfer);
        }
    }

    /**
     * Notifies background process about changed FD list
     */
    private void notifyAsyncIOTask() {
        // start background process if needed
        if (asyncIOWakeUpEventFd == 0) {
            startAsyncIOTask();
            return;
        }

        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            if (IO.eventfd_write(asyncIOWakeUpEventFd, 1, errnoState) < 0)
                throwLastError(errnoState, "internal error (eventfd_write)");
        }
    }

    /**
     * Register a device for asynchronous IO completion handling
     *
     * @param device USB device
     */
    synchronized void addForAsyncIOCompletion(LinuxUSBDevice device) {
        int n = asyncFds != null ? asyncFds.length : 0;
        int[] fds = new int[n + 1];
        if (n > 0)
            System.arraycopy(asyncFds, 0, fds, 0, n);
        fds[n] = device.fileDescriptor();

        // activate new array
        asyncFds = fds;
        notifyAsyncIOTask();
    }

    /**
     * Unregisters a device from asynchronous IO completion handling.
     *
     * @param device USB device
     */
    synchronized void removeFromAsyncIOCompletion(LinuxUSBDevice device) {
        removeFdFromAsyncIOCompletion(device.fileDescriptor());
        notifyAsyncIOTask();
    }

    private synchronized void removeFdFromAsyncIOCompletion(int fd) {
        // copy file descriptor (except the device's) into new array
        int n = asyncFds.length;
        if (n == 0)
            return;

        int[] fds = new int[n - 1];
        int tgt = 0;
        for (int asyncFd : asyncFds) {
            if (asyncFd != fd) {
                if (tgt == n)
                    return;
                fds[tgt] = asyncFd;
                tgt += 1;
            }
        }

        // make new array to active one
        asyncFds = fds;
    }

    synchronized void submitTransfer(LinuxUSBDevice device, int endpointAddress, USBTransferType transferType, LinuxTransfer transfer) {

        addURB(transfer);
        var urb = transfer.urb;

        usbdevfs_urb.type$set(urb, (byte) urbTransferType(transferType));
        usbdevfs_urb.endpoint$set(urb, (byte) endpointAddress);
        usbdevfs_urb.buffer$set(urb, transfer.data);
        usbdevfs_urb.buffer_length$set(urb, transfer.dataSize);
        usbdevfs_urb.usercontext$set(urb, MemorySegment.ofAddress(device.fileDescriptor()));

        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            if (IO.ioctl(device.fileDescriptor(), SUBMITURB, urb, errnoState) < 0) {
                String action = endpointAddress >= 128 ? "reading from" : "writing to";
                String endpoint = endpointAddress == 0 ? "control endpoint" : String.format("endpoint %d", endpointAddress);
                throwLastError(errnoState, "failed %s %s", action, endpoint);
            }
        }
    }

    private static int urbTransferType(USBTransferType transferType) {
        return switch (transferType) {
            case BULK -> USBDEVFS_URB_TYPE_BULK();
            case INTERRUPT -> USBDEVFS_URB_TYPE_INTERRUPT();
            case CONTROL -> USBDEVFS_URB_TYPE_CONTROL();
            case ISOCHRONOUS -> USBDEVFS_URB_TYPE_ISO();
        };
    }

    private void addURB(LinuxTransfer transfer) {
        MemorySegment urb;
        int size = availableURBs.size();
        if (size > 0) {
            urb = availableURBs.remove(size - 1);
        } else {
            urb = usbdevfs_urb.allocate(urbArena);
        }

        transfer.urb = urb;
        transfersByURB.put(urb, transfer);
    }

    private synchronized LinuxTransfer getTransferResult(MemorySegment urb) {
        var transfer = transfersByURB.remove(urb);
        if (transfer == null)
            throwException("internal error (unknown URB)");

        transfer.resultCode = -usbdevfs_urb.status$get(transfer.urb);
        transfer.resultSize = usbdevfs_urb.actual_length$get(transfer.urb);

        availableURBs.add(transfer.urb);
        transfer.urb = null;
        return transfer;
    }

    synchronized void abortTransfers(LinuxUSBDevice device, byte endpointAddress) {
        int fd = device.fileDescriptor();
        try (var arena = Arena.openConfined()) {

            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());

            // iterate all URBs and discard the ones for the specified endpoint
            for (var urb : transfersByURB.keySet()) {
                if (fd != (int) usbdevfs_urb.usercontext$get(urb).address())
                    continue;
                if (endpointAddress != usbdevfs_urb.endpoint$get(urb))
                    continue;

                if (IO.ioctl(fd, DISCARDURB, urb, errnoState) < 0) {
                    // ignore EINVAL; it occurs if the URB has completed at the same time
                    if (Linux.getErrno(errnoState) != errno.EINVAL())
                        throwLastError(errnoState, "failed to abort transfer");
                }
            }
        }
    }

    private void startAsyncIOTask() {
        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            asyncIOWakeUpEventFd = IO.eventfd(0, 0, errnoState);
            if (asyncIOWakeUpEventFd == -1) {
                asyncIOWakeUpEventFd = 0;
                throwLastError(errnoState, "internal error (eventfd)");
            }
        }

        // start background thread for handling IO completion
        Thread t = new Thread(this::asyncCompletionTask, "USB async IO");
        t.setDaemon(true);
        t.start();
    }
}
