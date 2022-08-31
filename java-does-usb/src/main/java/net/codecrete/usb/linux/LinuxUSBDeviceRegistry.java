//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.USBException;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.linux.gen.select.fd_set;
import net.codecrete.usb.linux.gen.select.select;
import net.codecrete.usb.linux.gen.udev.udev;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Linux implementation of USB device registry.
 */
public class LinuxUSBDeviceRegistry extends USBDeviceRegistry {

    private static final MemorySegment SUBSYSTEM_USB = MemorySession.global().allocateUtf8String("usb");
    private static final MemorySegment MONITOR_NAME = MemorySession.global().allocateUtf8String("udev");
    private static final MemorySegment DEVTYPE_USB_DEVICE = MemorySession.global().allocateUtf8String("usb_device");

    @Override
    protected void monitorDevices() {

        // setup udev monitor
        var udevInstance = udev.udev_new();
        if (udevInstance == NULL)
            throw new USBException("internal error (udev_new)");

        var monitor = udev.udev_monitor_new_from_netlink(udevInstance, MONITOR_NAME);
        if (monitor == NULL)
            throw new USBException("internal error (udev_monitor_new_from_netlink)");

        if (udev.udev_monitor_filter_add_match_subsystem_devtype(monitor, SUBSYSTEM_USB, DEVTYPE_USB_DEVICE) < 0)
            throw new USBException("internal error (udev_monitor_filter_add_match_subsystem_devtype)");

        if (udev.udev_monitor_enable_receiving(monitor) < 0)
            throw new USBException("internal error (udev_monitor_enable_receiving)");

        int fd = udev.udev_monitor_get_fd(monitor);
        if (fd < 0)
            throw new USBException("internal error (udev_monitor_get_fd)");

        // create initial list of devices
        enumeratePresentDevices(udevInstance);

        // monitor device changes
        while (true) {
            try (var session = MemorySession.openConfined()) {

                // wait for next change
                waitForFileDescriptor(fd, session);

                // retrieve change
                var dev = udev.udev_monitor_receive_device(monitor);
                if (dev == null)
                    continue; // shouldn't happen

                session.addCloseAction(() -> udev.udev_device_unref(dev));

                // get details
                var action = getDeviceAction(dev);

                if ("add".equals(action)) {
                    onDeviceConnected(dev);
                } else if ("remove".equals(action)) {
                    onDeviceDisconnected(dev);
                }
            }
        }
    }

    private void enumeratePresentDevices(Addressable udevInstance) {
        List<USBDeviceInfo> result = new ArrayList<>();
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
            for (var entry = udev.udev_enumerate_get_list_entry(enumerate);
                 entry != NULL;
                 entry = udev.udev_list_entry_get_next(entry)) {

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
                    var deviceInfo = getDeviceDetails(dev);
                    if (deviceInfo != null)
                        result.add(deviceInfo);
                }
            }
        }

        setInitialDeviceList(result);
    }

    private void onDeviceConnected(MemoryAddress device) {

        var deviceInfo = getDeviceDetails(device);
        if (deviceInfo == null)
            return;

        addDevice(deviceInfo);
    }

    private void onDeviceDisconnected(MemoryAddress device) {

        var devPath = getDeviceName(device);
        if (devPath == null)
            return;

        removeDevice(devPath);
    }

    /**
     * Retrieves the device details and returns a {@code USBDeviceInfo} instance.
     * <p>
     * If the device is missing one of vendor ID, product ID or device path,
     * {@code null} is returned.
     * </p>
     *
     * @param device the device
     * @return the device info
     */
    private static USBDeviceInfo getDeviceDetails(MemoryAddress device) {

        // retrieve device attributes
        String idVendor = getDeviceAttribute(device, "idVendor");
        if (idVendor == null)
            return null;

        String idProduct = getDeviceAttribute(device, "idProduct");
        if (idProduct == null)
            return null;

        // get device path
        var devPath = getDeviceName(device);
        if (devPath == null)
            return null;

        int vendorId = Integer.parseInt(idVendor, 16);
        int productId = Integer.parseInt(idProduct, 16);

        // create device info instance
        return new LinuxUSBDeviceInfo(devPath, vendorId, productId,
                getDeviceAttribute(device, "manufacturer"),
                getDeviceAttribute(device, "product"),
                getDeviceAttribute(device, "serial"),
                0, 0, 0);
    }

    private static String getDeviceAttribute(Addressable device, String attribute) {
        try (var session = MemorySession.openConfined()) {
            var sysattr = session.allocateUtf8String(attribute);
            var valueAddr = udev.udev_device_get_sysattr_value(device, sysattr);
            if (valueAddr == NULL)
                return null;

            var value = MemorySegment.ofAddress(valueAddr, 2000, session);
            return value.getUtf8String(0);
        }
    }

    private static String getDeviceName(Addressable device) {
        return Linux.createStringFromAddress(udev.udev_device_get_devnode(device));
    }

    private static String getDeviceAction(Addressable device) {
        return Linux.createStringFromAddress(udev.udev_device_get_action(device));
    }

    /**
     * Waits until the specified file descriptor becomes ready for reading.
     *
     * @param fd      the file descriptor
     * @param session a memory session for allocating memory
     */
    private static void waitForFileDescriptor(int fd, MemorySession session) {
        // fd_set is a bit array (constructed from 64-bit integers)
        var fds = session.allocate(fd_set.$LAYOUT());
        fds.set(JAVA_LONG, fd / JAVA_LONG.bitSize(), 1L << (fd % JAVA_LONG.bitSize()));

        int res = select.select(fd + 1, fds, NULL, NULL, NULL);
        if (res <= 0)
            throw new USBException("internal error (select)");
    }
}
