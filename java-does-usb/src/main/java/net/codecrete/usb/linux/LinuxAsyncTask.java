//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.UsbTransferType;
import net.codecrete.usb.linux.gen.epoll.epoll_event;
import net.codecrete.usb.linux.gen.errno.errno;
import net.codecrete.usb.linux.gen.usbdevice_fs.usbdevfs_urb;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static net.codecrete.usb.common.ForeignMemory.dereference;
import static net.codecrete.usb.linux.EPoll.epoll_create1;
import static net.codecrete.usb.linux.EPoll.epoll_wait;
import static net.codecrete.usb.linux.Linux.allocateErrorState;
import static net.codecrete.usb.linux.LinuxUsbException.throwException;
import static net.codecrete.usb.linux.LinuxUsbException.throwLastError;
import static net.codecrete.usb.linux.UsbDevFS.DISCARDURB;
import static net.codecrete.usb.linux.UsbDevFS.REAPURBNDELAY;
import static net.codecrete.usb.linux.UsbDevFS.SUBMITURB;
import static net.codecrete.usb.linux.gen.epoll.epoll.EPOLLOUT;
import static net.codecrete.usb.linux.gen.epoll.epoll.EPOLLWAKEUP;
import static net.codecrete.usb.linux.gen.errno.errno.EINTR;
import static net.codecrete.usb.linux.gen.errno.errno.ENODEV;
import static net.codecrete.usb.linux.gen.fcntl.fcntl.FD_CLOEXEC;
import static net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs.USBDEVFS_URB_TYPE_BULK;
import static net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs.USBDEVFS_URB_TYPE_CONTROL;
import static net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs.USBDEVFS_URB_TYPE_INTERRUPT;
import static net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs.USBDEVFS_URB_TYPE_ISO;

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
@SuppressWarnings("java:S6548")
class LinuxAsyncTask {
    /**
     * Singleton instance of background task.
     */
    static final LinuxAsyncTask INSTANCE = new LinuxAsyncTask();

    private static final int NUM_EVENTS = 5;

    private final Arena urbArena = Arena.ofAuto();
    /// available URBs
    private final List<MemorySegment> availableURBs = new ArrayList<>();
    /// map of URB addresses to transfer (for outstanding transfers)
    private final Map<MemorySegment, LinuxTransfer> transfersByURB = new LinkedHashMap<>();
    /// file descriptor of epoll
    private int epollFd = -1;

    /**
     * Background task for handling asynchronous IO completions.
     * <p>
     * It polls on all registered file descriptors. If a file descriptor is
     * ready, the URB is "reaped".
     * </p>
     */
    @SuppressWarnings({"java:S2189", "java:S135", "java:S3776"})
    private void asyncCompletionTask() {

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            var urbPointerHolder = arena.allocate(ADDRESS);
            var events = arena.allocate(epoll_event.layout(), NUM_EVENTS);

            while (true) {

                // wait for file descriptor to be ready
                var res = epoll_wait(epollFd, events, NUM_EVENTS, -1, errorState);
                if (res < 0) {
                    var err = Linux.getErrno(errorState);
                    if (err == EINTR())
                        continue; // continue on interrupt
                    throwException(err, "internal error (epoll_wait)");
                }

                // for all ready file descriptors, reap URBs
                for (int i = 0; i < res; i++) {
                    var fd = (int) EPoll.EVENT_ARRAY_DATA_FD$VH.get(events, 0, i);
                    reapURBs(fd, urbPointerHolder, errorState);
                }
            }
        }
    }

    /**
     * Reap all pending URBs and handle the completed transfers.
     *
     * @param fd               file descriptor
     * @param urbPointerHolder native memory to receive the URB pointer
     * @param errorState       native memory to receive the errno
     */
    private synchronized void reapURBs(int fd, MemorySegment urbPointerHolder, MemorySegment errorState) {

        while (true) {
            var res = IO.ioctl(fd, REAPURBNDELAY, urbPointerHolder, errorState);
            if (res < 0) {
                var err = Linux.getErrno(errorState);
                if (err == errno.EAGAIN())
                    return; // no more pending URBs
                if (err == errno.ENODEV()) {
                    // device might have been unplugged
                    EPoll.removeFileDescriptor(epollFd, fd);
                    return;
                }
                throwException(err, "internal error (reap URB)");
            }

            // call completion handler
            var urb = dereference(urbPointerHolder);
            var transfer = getTransferWithResult(urb);
            transfer.completion().completed(transfer);
        }
    }

    /**
     * Register a device for asynchronous IO completion handling
     *
     * @param device USB device
     */
    synchronized void addForAsyncIOCompletion(LinuxUsbDevice device) {
        // start background process if needed
        if (epollFd < 0)
            startAsyncIOTask();

        EPoll.addFileDescriptor(epollFd, EPOLLOUT() | EPOLLWAKEUP(), device.fileDescriptor());
    }

    /**
     * Unregisters a device from asynchronous IO completion handling.
     *
     * @param device USB device
     */
    synchronized void removeFromAsyncIOCompletion(LinuxUsbDevice device) {
        int fd = device.fileDescriptor();

        // remove file descriptor from epoll
        EPoll.removeFileDescriptor(epollFd, fd);

        // reap outstanding URBs
        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            var urbPointerHolder = arena.allocate(ADDRESS);
            reapURBs(fd, urbPointerHolder, errorState);
        }

        // reclaim stale URBs
        transfersByURB.entrySet().removeIf(e -> {
            var urb = e.getKey();
            var isMatch = usbdevfs_urb.usercontext(urb).address() == fd;
            if (isMatch) {
                var transfer = e.getValue();
                transfer.urb = null;
                transfer.setResultCode(ENODEV());
                transfer.setResultSize(0);
                transfer.completion().completed(transfer);
                availableURBs.add(urb);
            }
            return isMatch;
        });
    }

    synchronized void submitTransfer(LinuxUsbDevice device, int endpointAddress, UsbTransferType transferType, LinuxTransfer transfer) {

        linkToUrb(transfer);
        var urb = transfer.urb;

        usbdevfs_urb.type(urb, (byte) urbTransferType(transferType));
        usbdevfs_urb.endpoint(urb, (byte) endpointAddress);
        usbdevfs_urb.buffer(urb, transfer.data());
        usbdevfs_urb.buffer_length(urb, transfer.dataSize());
        usbdevfs_urb.usercontext(urb, MemorySegment.ofAddress(device.fileDescriptor()));

        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            if (IO.ioctl(device.fileDescriptor(), SUBMITURB, urb, errorState) < 0) {
                var action = endpointAddress >= 128 ? "reading from" : "writing to";
                var endpoint = endpointAddress == 0 ? "control endpoint" : String.format("endpoint %d", endpointAddress);
                throwLastError(errorState, "error occurred while %s %s", action, endpoint);
            }
        }
    }

    private static int urbTransferType(UsbTransferType transferType) {
        return switch (transferType) {
            case BULK -> USBDEVFS_URB_TYPE_BULK();
            case INTERRUPT -> USBDEVFS_URB_TYPE_INTERRUPT();
            case CONTROL -> USBDEVFS_URB_TYPE_CONTROL();
            case ISOCHRONOUS -> USBDEVFS_URB_TYPE_ISO();
        };
    }

    /**
     * Links the specified transfer instance to a URB.
     * <p>
     * The transfer is assigned an URB instance, and a list
     * of associations from URB to transfer is maintained.
     * </p>
     * @param transfer the transfer to assign a URB.
     */
    private void linkToUrb(LinuxTransfer transfer) {
        MemorySegment urb;
        var size = availableURBs.size();
        if (size > 0) {
            urb = availableURBs.remove(size - 1);
        } else {
            urb = usbdevfs_urb.allocate(urbArena);
        }

        transfer.urb = urb;
        transfersByURB.put(urb, transfer);
    }

    /**
     * Gets the transfer associated with the specified URB and adds the result.
     * <p>
     * The URB is returned into the list of URBs available for further transfers.
     * </p>
     *
     * @param urb URB instance
     * @return transfer associated with the URB
     */
    @SuppressWarnings("java:S2259")
    private synchronized LinuxTransfer getTransferWithResult(MemorySegment urb) {
        var transfer = transfersByURB.remove(urb);
        if (transfer == null)
            throwException("internal error (unknown URB)");

        transfer.setResultCode(-usbdevfs_urb.status(transfer.urb));
        transfer.setResultSize(usbdevfs_urb.actual_length(transfer.urb));

        availableURBs.add(transfer.urb);
        transfer.urb = null;

        return transfer;
    }

    @SuppressWarnings("java:S1066")
    synchronized void abortTransfers(LinuxUsbDevice device, byte endpointAddress) {
        var fd = device.fileDescriptor();
        try (var arena = Arena.ofConfined()) {

            var errorState = allocateErrorState(arena);

            // iterate all URBs and discard the ones for the specified endpoint
            transfersByURB.keySet().stream()
                    .filter(urb ->
                            usbdevfs_urb.usercontext(urb).address() == fd
                                    && usbdevfs_urb.endpoint(urb) == endpointAddress)
                    .forEach(urb -> {
                                if (IO.ioctl(fd, DISCARDURB, urb, errorState) < 0) {
                                    // ignore EINVAL; it occurs if the URB has completed at the same time
                                    if (Linux.getErrno(errorState) != errno.EINVAL())
                                        throwLastError(errorState, "error occurred while aborting transfer");
                                }
                            }
                    );
        }
    }

    private void startAsyncIOTask() {
        try (var arena = Arena.ofConfined()) {
            var errorState = allocateErrorState(arena);
            epollFd = epoll_create1(FD_CLOEXEC(), errorState);
            if (epollFd < 0)
                throwLastError(errorState, "internal error (epoll_create)");
        }

        // start background thread for handling IO completion
        var thread = new Thread(this::asyncCompletionTask, "USB async IO");
        thread.setDaemon(true);
        thread.start();
    }
}
