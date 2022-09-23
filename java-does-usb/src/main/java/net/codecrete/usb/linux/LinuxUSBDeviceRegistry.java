//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.linux.gen.poll.poll;
import net.codecrete.usb.linux.gen.poll.pollfd;
import net.codecrete.usb.linux.gen.udev.udev;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;

/**
 * Linux implementation of USB device registry.
 */
public class LinuxUSBDeviceRegistry extends USBDeviceRegistry {

    private static final MemorySegment SUBSYSTEM_USB = MemorySession.global().allocateUtf8String("usb");
    private static final MemorySegment MONITOR_NAME = MemorySession.global().allocateUtf8String("udev");
    private static final MemorySegment DEVTYPE_USB_DEVICE = MemorySession.global().allocateUtf8String("usb_device");

    @Override
    protected void monitorDevices() {

        int fd;
        MemoryAddress monitor;

        try {
            // setup udev monitor
            var udevInstance = udev.udev_new();
            if (udevInstance == NULL)
                throw new USBException("internal error (udev_new)");

            monitor = udev.udev_monitor_new_from_netlink(udevInstance, MONITOR_NAME);
            if (monitor == NULL)
                throw new USBException("internal error (udev_monitor_new_from_netlink)");

            if (udev.udev_monitor_filter_add_match_subsystem_devtype(monitor, SUBSYSTEM_USB, DEVTYPE_USB_DEVICE) < 0)
                throw new USBException("internal error (udev_monitor_filter_add_match_subsystem_devtype)");

            if (udev.udev_monitor_enable_receiving(monitor) < 0)
                throw new USBException("internal error (udev_monitor_enable_receiving)");

            fd = udev.udev_monitor_get_fd(monitor);
            if (fd < 0)
                throw new USBException("internal error (udev_monitor_get_fd)");

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
            try (var session = MemorySession.openConfined()) {

                // wait for next change
                waitForFileDescriptor(fd, session);

                // retrieve change
                var udevDevice = udev.udev_monitor_receive_device(monitor);
                if (udevDevice == null)
                    continue; // shouldn't happen

                session.addCloseAction(() -> udev.udev_device_unref(udevDevice));

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

    private List<USBDevice> enumeratePresentDevices(Addressable udevInstance) {
        List<USBDevice> result = new ArrayList<>();
        try (var outerSession = MemorySession.openConfined()) {

            // create device enumerator
            var enumerate = udev.udev_enumerate_new(udevInstance);
            if (enumerate == NULL)
                throw new USBException("internal error (udev_enumerate_new)");

            outerSession.addCloseAction(() -> udev.udev_enumerate_unref(enumerate));

            if (udev.udev_enumerate_add_match_subsystem(enumerate, SUBSYSTEM_USB) < 0)
                throw new USBException("internal error (udev_enumerate_add_match_subsystem)");

            if (udev.udev_enumerate_scan_devices(enumerate) < 0)
                throw new USBException("internal error (udev_enumerate_scan_devices)");

            // enumerate devices
            for (var entry = udev.udev_enumerate_get_list_entry(enumerate); entry != NULL; entry =
                    udev.udev_list_entry_get_next(entry)) {

                try (var session = MemorySession.openConfined()) {

                    var path = udev.udev_list_entry_get_name(entry);
                    if (path == NULL)
                        continue;

                    // get device handle
                    var dev = udev.udev_device_new_from_syspath(udevInstance, path);
                    if (dev == NULL)
                        continue;

                    // ensure the device is released
                    session.addCloseAction(() -> udev.udev_device_unref(dev));

                    // get device details
                    var device = getDeviceDetails(dev);
                    if (device != null)
                        result.add(device);
                }
            }
        }

        return result;
    }

    private void onDeviceConnected(MemoryAddress udevDevice) {

        var device = getDeviceDetails(udevDevice);
        if (device != null)
            addDevice(device);
    }

    private void onDeviceDisconnected(MemoryAddress udevDevice) {

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
    private static USBDevice getDeviceDetails(MemoryAddress udevDevice) {

        int vendorId = 0;
        int productId = 0;

        try {
            // retrieve device attributes
            String idVendor = getDeviceAttribute(udevDevice, "idVendor");
            if (idVendor == null)
                return null;

            String idProduct = getDeviceAttribute(udevDevice, "idProduct");
            if (idProduct == null)
                return null;

            // get device path
            var devPath = getDeviceName(udevDevice);
            if (devPath == null)
                return null;

            vendorId = Integer.parseInt(idVendor, 16);
            productId = Integer.parseInt(idProduct, 16);

            // create device instance
            return new LinuxUSBDevice(devPath, vendorId, productId, getDeviceAttribute(udevDevice, "manufacturer"),
                    getDeviceAttribute(udevDevice, "product"), getDeviceAttribute(udevDevice, "serial"));

        } catch (Throwable e) {
            System.err.printf("Info: [JavaDoesUSB] failed to retrieve information about device 0x%04x/0x%04x - " +
                    "ignoring device%n", vendorId, productId);
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static String getDeviceAttribute(Addressable udevDevice, String attribute) {
        try (var session = MemorySession.openConfined()) {
            var sysattr = session.allocateUtf8String(attribute);
            var valueAddr = udev.udev_device_get_sysattr_value(udevDevice, sysattr);
            if (valueAddr == NULL)
                return null;

            var value = MemorySegment.ofAddress(valueAddr, 2000, session);
            return value.getUtf8String(0);
        }
    }

    private static String getDeviceName(Addressable udevDevice) {
        return Linux.createStringFromAddress(udev.udev_device_get_devnode(udevDevice));
    }

    private static String getDeviceAction(Addressable udevDevice) {
        return Linux.createStringFromAddress(udev.udev_device_get_action(udevDevice));
    }

    /**
     * Waits until the specified file descriptor becomes ready for reading.
     *
     * @param fd      the file descriptor
     * @param session a memory session for allocating memory
     */
    private static void waitForFileDescriptor(int fd, MemorySession session) {
        var fds = session.allocate(pollfd.$LAYOUT());
        pollfd.fd$set(fds, fd);
        pollfd.events$set(fds, (short) poll.POLLIN());
        int res = poll.poll(fds, 1, -1);
        if (res < 0)
            throw new USBException("internal error (poll)");
    }
}
