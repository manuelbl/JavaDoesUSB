//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.common.AsyncIOCompletion;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.linux.gen.errno.errno;
import net.codecrete.usb.linux.gen.poll.poll;
import net.codecrete.usb.linux.gen.poll.pollfd;
import net.codecrete.usb.linux.gen.udev.udev;
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
import static net.codecrete.usb.linux.gen.usbdevice_fs.usbdevice_fs.USBDEVFS_URB_TYPE_BULK;

/**
 * Linux implementation of USB device registry.
 */
public class LinuxUSBDeviceRegistry extends USBDeviceRegistry {

    private final SegmentAllocator GLOBAL_ALLOCATOR = SegmentAllocator.nativeAllocator(SegmentScope.global());
    private final MemorySegment SUBSYSTEM_USB = GLOBAL_ALLOCATOR.allocateUtf8String("usb");
    private final MemorySegment MONITOR_NAME = GLOBAL_ALLOCATOR.allocateUtf8String("udev");
    private final MemorySegment DEVTYPE_USB_DEVICE = GLOBAL_ALLOCATOR.allocateUtf8String("usb_device");

    private final MemorySegment ATTR_ID_VENDOR = GLOBAL_ALLOCATOR.allocateUtf8String("idVendor");
    private final MemorySegment ATTR_ID_PRODUCT = GLOBAL_ALLOCATOR.allocateUtf8String("idProduct");
    private final MemorySegment ATTR_MANUFACTURER = GLOBAL_ALLOCATOR.allocateUtf8String("manufacturer");
    private final MemorySegment ATTR_PRODUCT = GLOBAL_ALLOCATOR.allocateUtf8String("product");
    private final MemorySegment ATTR_SERIAL = GLOBAL_ALLOCATOR.allocateUtf8String("serial");

    /// available URBs
    private final List<MemorySegment> availableURBs = new ArrayList<>();
    /// map of URB address to completion handler (for outstanding requests)
    private final Map<Long, AsyncIOCompletion> completionHandlerByURB = new HashMap<>();
    /// array of file descriptors using asynchronous completion
    private int[] asyncFds;
    /// file descriptor to notify async IO background thread about an update
    private int asyncIOUpdateEventFd;

    @Override
    protected void monitorDevices() {

        int fd;
        MemorySegment monitor;

        try {
            // setup udev monitor
            var udevInstance = udev.udev_new();
            if (udevInstance.address() == 0)
                throwException("internal error (udev_new)");

            monitor = udev.udev_monitor_new_from_netlink(udevInstance, MONITOR_NAME);
            if (monitor.address() == 0)
                throwException("internal error (udev_monitor_new_from_netlink)");

            if (udev.udev_monitor_filter_add_match_subsystem_devtype(monitor, SUBSYSTEM_USB, DEVTYPE_USB_DEVICE) < 0)
                throwException("internal error (udev_monitor_filter_add_match_subsystem_devtype)");

            if (udev.udev_monitor_enable_receiving(monitor) < 0)
                throwException("internal error (udev_monitor_enable_receiving)");

            fd = udev.udev_monitor_get_fd(monitor);
            if (fd < 0)
                throwException("internal error (udev_monitor_get_fd)");

            // create initial list of devices
            var deviceList = enumeratePresentDevices(udevInstance);
            setInitialDeviceList(deviceList);

        } catch (Throwable e) {
            enumerationFailed(e);
            return;
        }

        // monitor device changes
        //noinspection InfiniteLoopStatement
        while (true) {
            try (var arena = Arena.openConfined(); var cleanup = new ScopeCleanup()) {

                // wait for next change
                waitForFileDescriptor(fd, arena);

                // retrieve change
                var udevDevice = udev.udev_monitor_receive_device(monitor);
                if (udevDevice == null)
                    continue; // shouldn't happen

                cleanup.add(() -> udev.udev_device_unref(udevDevice));

                // get details
                var action = getDeviceAction(udevDevice);

                if ("add".equals(action)) {
                    onDeviceConnected(udevDevice);
                } else if ("remove".equals(action)) {
                    onDeviceDisconnected(udevDevice);
                }
            }
        }
    }

    private List<USBDevice> enumeratePresentDevices(MemorySegment udevInstance) {
        List<USBDevice> result = new ArrayList<>();
        try (var outerCleanup = new ScopeCleanup()) {

            // create device enumerator
            var enumerate = udev.udev_enumerate_new(udevInstance);
            if (enumerate.address() == 0)
                throwException("internal error (udev_enumerate_new)");

            outerCleanup.add(() -> udev.udev_enumerate_unref(enumerate));

            if (udev.udev_enumerate_add_match_subsystem(enumerate, SUBSYSTEM_USB) < 0)
                throwException("internal error (udev_enumerate_add_match_subsystem)");

            if (udev.udev_enumerate_scan_devices(enumerate) < 0)
                throwException("internal error (udev_enumerate_scan_devices)");

            // enumerate devices
            for (var entry = udev.udev_enumerate_get_list_entry(enumerate); entry.address() != 0; entry =
                    udev.udev_list_entry_get_next(entry)) {

                try (var cleanup = new ScopeCleanup()) {

                    var path = udev.udev_list_entry_get_name(entry);
                    if (path.address() == 0)
                        continue;

                    // get device handle
                    var dev = udev.udev_device_new_from_syspath(udevInstance, path);
                    if (dev.address() == 0)
                        continue;

                    // ensure the device is released
                    cleanup.add(() -> udev.udev_device_unref(dev));

                    // get device details
                    var device = getDeviceDetails(dev);
                    if (device != null)
                        result.add(device);
                }
            }
        }

        return result;
    }

    private void onDeviceConnected(MemorySegment udevDevice) {

        var device = getDeviceDetails(udevDevice);
        if (device != null)
            addDevice(device);
    }

    private void onDeviceDisconnected(MemorySegment udevDevice) {

        var devPath = getDeviceName(udevDevice);
        if (devPath == null)
            return;

        closeAndRemoveDevice(devPath);
    }

    /**
     * Retrieves the device details and returns a {@code USBDevice} instance.
     * <p>
     * If the device is missing one of vendor ID, product ID or device path,
     * {@code null} is returned.
     * </p>
     *
     * @param udevDevice the device (udev_device*)
     * @return the device instance
     */
    private USBDevice getDeviceDetails(MemorySegment udevDevice) {

        int vendorId = 0;
        int productId = 0;

        try {
            // retrieve device attributes
            String idVendor = getDeviceAttribute(udevDevice, ATTR_ID_VENDOR);
            if (idVendor == null)
                return null;

            String idProduct = getDeviceAttribute(udevDevice, ATTR_ID_PRODUCT);
            if (idProduct == null)
                return null;

            // get device path
            var devPath = getDeviceName(udevDevice);
            if (devPath == null)
                return null;

            vendorId = Integer.parseInt(idVendor, 16);
            productId = Integer.parseInt(idProduct, 16);

            // create device instance
            var device = new LinuxUSBDevice(this, devPath, vendorId, productId);

            device.setProductStrings(getDeviceAttribute(udevDevice, ATTR_MANUFACTURER), getDeviceAttribute(udevDevice
                    , ATTR_PRODUCT), getDeviceAttribute(udevDevice, ATTR_SERIAL));

            return device;

        } catch (Throwable e) {
            System.err.printf("Info: [JavaDoesUSB] failed to retrieve information about device 0x%04x/0x%04x - " +
                    "ignoring device%n", vendorId, productId);
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static String getDeviceAttribute(MemorySegment udevDevice, MemorySegment attribute) {
        var value = udev.udev_device_get_sysattr_value(udevDevice, attribute);
        if (value.address() == 0)
            return null;

        return value.getUtf8String(0);
    }

    private static String getDeviceName(MemorySegment udevDevice) {
        return udev.udev_device_get_devnode(udevDevice).getUtf8String(0);
    }

    private static String getDeviceAction(MemorySegment udevDevice) {
        return udev.udev_device_get_action(udevDevice).getUtf8String(0);
    }

    /**
     * Waits until the specified file descriptor becomes ready for reading.
     *
     * @param fd    the file descriptor
     * @param arena an arena for allocating memory
     */
    private static void waitForFileDescriptor(int fd, Arena arena) {
        var fds = arena.allocate(pollfd.$LAYOUT());
        pollfd.fd$set(fds, fd);
        pollfd.events$set(fds, (short) poll.POLLIN());
        int res = poll.poll(fds, 1, -1);
        if (res < 0)
            throwException("internal error (poll)");
    }

    /**
     * Background task for handling asynchronous IO completions.
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
                    pollfd.events$set(asyncPolls, i, (short) (poll.POLLIN() | poll.POLLOUT()));
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
                        // TODO
                        System.err.println("Error in asynchronous request");
                        continue;
                    }

                    if (i != n) {
                        // reap URB
                        int fd = pollfd.fd$get(asyncPolls, i);
                        reapURB(fd, urbPointerHolder, errnoState);

                    } else {
                        res = IO.eventfd_read(asyncIOUpdateEventFd, eventfdValueHolder, errnoState);
                        if (res < 0)
                            throwLastError(errnoState, "internal error (eventfd_read)");
                    }
                }

            }
        }
    }

    private void reapURB(int fd, MemorySegment urbPointerHolder, MemorySegment errnoState) {
        int res;
        res = IO.ioctl(fd, REAPURB, urbPointerHolder, errnoState);
        if (res < 0) {
            var err = Linux.getErrno(errnoState);
            if (err == errno.EBADF())
                return; // ignore, device might have been closed
            throwException(err, "internal error (reap URB)");
        }

        var addr = urbPointerHolder.get(ADDRESS, 0);
        var urb = usbdevfs_urb.ofAddress(addr, SegmentScope.global());
        int status = usbdevfs_urb.status$get(urb);
        int length = usbdevfs_urb.actual_length$get(urb);

        // call completion handler
        synchronized (this) {
            var completionHandler = completionHandlerByURB.get(urb.address());
            completionHandler.completed(status, length);
            recycleURB(urb);
        }
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
    synchronized void registerCompletionHandling(LinuxUSBDevice device) {
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
    synchronized void unregisterCompletionHandling(LinuxUSBDevice device) {
        // copy file descriptor (except the device's) into new array
        int n = asyncFds.length;
        if (n == 0)
            return;

        int fd = device.fileDescriptor();
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


        // activate new array
        asyncFds = fds;
        notifyAsyncIOTask();
    }

    synchronized void submitBulkTransfer(LinuxUSBDevice device, int endpointAddress, MemorySegment buffer,
                                         int bufferLength, AsyncIOCompletion completion) {
        var urb = getURB(completion);

        usbdevfs_urb.type$set(urb, (byte) USBDEVFS_URB_TYPE_BULK());
        usbdevfs_urb.endpoint$set(urb, (byte) endpointAddress);
        usbdevfs_urb.buffer$set(urb, buffer);
        usbdevfs_urb.buffer_length$set(urb, bufferLength);
        usbdevfs_urb.usercontext$set(urb, MemorySegment.ofAddress(device.fileDescriptor()));

        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());
            if (IO.ioctl(device.fileDescriptor(), SUBMITURB, urb, errnoState) < 0)
                throwLastError(errnoState, "failed to submit bulk transfer request");
        }
    }

    private MemorySegment getURB(AsyncIOCompletion completionHandler) {
        MemorySegment urb;
        int size = availableURBs.size();
        if (size > 0) {
            urb = availableURBs.remove(size - 1);
        } else {
            urb = usbdevfs_urb.allocate(GLOBAL_ALLOCATOR);
        }

        completionHandlerByURB.put(urb.address(), completionHandler);

        return urb;
    }

    synchronized void recycleURB(MemorySegment urb) {
        completionHandlerByURB.remove(urb.address());
        availableURBs.add(urb);
    }

    synchronized void abortTransfers(LinuxUSBDevice device, byte endpointAddress) {
        int fd = device.fileDescriptor();
        try (var arena = Arena.openConfined()) {
            var errnoState = arena.allocate(Linux.ERRNO_STATE.layout());

            for (var urbAddress : completionHandlerByURB.keySet()) {
                var urb = usbdevfs_urb.ofAddress(MemorySegment.ofAddress(urbAddress), SegmentScope.global());
                if (fd != (int) usbdevfs_urb.usercontext$get(urb).address())
                    continue;
                if (endpointAddress != usbdevfs_urb.endpoint$get(urb))
                    continue;

                if (IO.ioctl(fd, DISCARDURB, urb, errnoState) < 0)
                    throwLastError(errnoState, "failed to cancel transfer request");
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
