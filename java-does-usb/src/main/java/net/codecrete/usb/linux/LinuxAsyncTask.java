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
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Each USB device must register its file handler with this task.
 * </p>
 * <p>
 * The task keeps track of the submitted transfers by remembering the
 * URB address (USB request block) in order to match it to the
 * completion.
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

    private final SegmentAllocator GLOBAL_ALLOCATOR = SegmentAllocator.nativeAllocator(SegmentScope.global());
    /// available URBs
    private final List<MemorySegment> availableURBs = new ArrayList<>();
    /// map of URB addresses to transfer (for outstanding transfers)
    private final Map<Long, LinuxTransfer> transfersByURB = new HashMap<>();
    /// array of file descriptors using asynchronous completion
    private int[] asyncFds;
    /// file descriptor to notify async IO background thread about an update
    private int asyncIOUpdateEventFd;

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

                pollfd.fd$set(asyncPolls, n, asyncIOUpdateEventFd);
                pollfd.events$set(asyncPolls, n, (short) poll.POLLIN());
                pollfd.revents$set(asyncPolls, n, (short) 0);

                // poll for event
                int res = poll.poll(asyncPolls, n + 1, -1);
                if (res < 0)
                    throwException("internal error (poll)");

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

                    if (i != n) {
                        // reap URB
                        int fd = pollfd.fd$get(asyncPolls, i);
                        reapURB(fd, urbPointerHolder, errnoState);

                    } else {
                        // wakeup to refresh list of file descriptors
                        res = IO.eventfd_read(asyncIOUpdateEventFd, eventfdValueHolder, errnoState);
                        if (res < 0)
                            throwLastError(errnoState, "internal error (eventfd_read)");
                    }
                }
            }
        }
    }

    /**
     * Reap URB and handle the completed transfer.
     *
     * @param fd               file descriptor
     * @param urbPointerHolder native memory to receive the URB pointer
     * @param errnoState       native memory to receive the errno
     */
    private void reapURB(int fd, MemorySegment urbPointerHolder, MemorySegment errnoState) {
        int res;
        res = IO.ioctl(fd, REAPURBNDELAY, urbPointerHolder, errnoState);
        if (res < 0) {
            var err = Linux.getErrno(errnoState);
            if (err == errno.EAGAIN())
                return; // retry
            if (err == errno.EBADF())
                return; // ignore, device might have been closed
            throwException(err, "internal error (reap URB)");
        }

        // call completion handler
        var urbAddr = urbPointerHolder.get(JAVA_LONG, 0);
        var transfer = getTransferResult(urbAddr);
        transfer.completion.completed(transfer);
    }

    /**
     * Notifies background process about changed FD list
     */
    private void notifyAsyncIOTask() {
        // start background process if needed
        if (asyncIOUpdateEventFd == 0) {
            startAsyncIOHandler();
            return;
        }

        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            if (IO.eventfd_write(asyncIOUpdateEventFd, 1, errnoState) < 0)
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
            urb = usbdevfs_urb.allocate(GLOBAL_ALLOCATOR);
        }

        transfer.urb = urb;
        transfersByURB.put(urb.address(), transfer);
    }

    private synchronized LinuxTransfer getTransferResult(long urbAddr) {
        var transfer = transfersByURB.remove(urbAddr);
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

            for (var urbAddress : transfersByURB.keySet()) {
                var urb = usbdevfs_urb.ofAddress(MemorySegment.ofAddress(urbAddress), SegmentScope.global());
                if (fd != (int) usbdevfs_urb.usercontext$get(urb).address())
                    continue;
                if (endpointAddress != usbdevfs_urb.endpoint$get(urb))
                    continue;

                if (IO.ioctl(fd, DISCARDURB, urb, errnoState) < 0)
                    throwLastError(errnoState, "failed to cancel transfer");
            }
        }
    }

    private void startAsyncIOHandler() {
        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            asyncIOUpdateEventFd = IO.eventfd(0, 0, errnoState);
            if (asyncIOUpdateEventFd == -1) {
                asyncIOUpdateEventFd = 0;
                throwLastError(errnoState, "internal error (eventfd)");
            }
        }

        // start background thread for handling IO completion
        Thread t = new Thread(this::asyncCompletionTask, "USB async IO");
        t.setDaemon(true);
        t.start();
    }
}
