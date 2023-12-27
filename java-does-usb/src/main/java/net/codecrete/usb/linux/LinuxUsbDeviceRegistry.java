//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.UsbDevice;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.UsbDeviceRegistry;
import net.codecrete.usb.linux.gen.epoll.epoll_event;
import net.codecrete.usb.linux.gen.udev.udev;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.Logger.Level.INFO;
import static net.codecrete.usb.linux.EPoll.epoll_create;
import static net.codecrete.usb.linux.EPoll.epoll_wait;
import static net.codecrete.usb.linux.Linux.allocateErrorState;
import static net.codecrete.usb.linux.LinuxUsbException.throwException;
import static net.codecrete.usb.linux.LinuxUsbException.throwLastError;
import static net.codecrete.usb.linux.gen.epoll.epoll.*;

/**
 * Linux implementation of USB device registry.
 */
public class LinuxUsbDeviceRegistry extends UsbDeviceRegistry {

    private static final System.Logger LOG = System.getLogger(LinuxUsbDeviceRegistry.class.getName());

    private static final MemorySegment SUBSYSTEM_USB;
    private static final MemorySegment MONITOR_NAME;
    private static final MemorySegment DEVTYPE_USB_DEVICE;

    private static final MemorySegment ATTR_ID_VENDOR;
    private static final MemorySegment ATTR_ID_PRODUCT;
    private static final MemorySegment ATTR_MANUFACTURER;
    private static final MemorySegment ATTR_PRODUCT;
    private static final MemorySegment ATTR_SERIAL;

    static {
        @SuppressWarnings("resource")
        var global = Arena.global();

        SUBSYSTEM_USB = global.allocateUtf8String("usb");
        MONITOR_NAME = global.allocateUtf8String("udev");
        DEVTYPE_USB_DEVICE = global.allocateUtf8String("usb_device");

        ATTR_ID_VENDOR = global.allocateUtf8String("idVendor");
        ATTR_ID_PRODUCT = global.allocateUtf8String("idProduct");
        ATTR_MANUFACTURER = global.allocateUtf8String("manufacturer");
        ATTR_PRODUCT = global.allocateUtf8String("product");
        ATTR_SERIAL = global.allocateUtf8String("serial");
    }

    @SuppressWarnings({"java:S1181", "java:S2189"})
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

        try (var arena = Arena.ofConfined()) {
            // create epoll
            var errorState = allocateErrorState(arena);
            var epfd = epoll_create(1, errorState);
            if (epfd < 0)
                throwLastError(errorState, "internal error (epoll_create)");
            EPoll.addFileDescriptor(epfd, EPOLLIN(), fd);

            // allocate event (as output for epoll_wait)
            var event = arena.allocate(epoll_event.$LAYOUT());

            // monitor device changes
            //noinspection InfiniteLoopStatement
            while (true) {
                try (var cleanup = new ScopeCleanup()) {

                    // wait for next change
                    epoll_wait(epfd, event, 1, -1, errorState);

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
    }

    @SuppressWarnings("java:S135")
    private List<UsbDevice> enumeratePresentDevices(MemorySegment udevInstance) {
        List<UsbDevice> result = new ArrayList<>();
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
     * Retrieves the device details and returns a {@code UsbDevice} instance.
     * <p>
     * If the device is missing one of vendor ID, product ID or device path,
     * {@code null} is returned.
     * </p>
     *
     * @param udevDevice the device (udev_device*)
     * @return the device instance
     */
    @SuppressWarnings("java:S106")
    private UsbDevice getDeviceDetails(MemorySegment udevDevice) {

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
            var device = new LinuxUsbDevice(devPath, vendorId, productId);

            device.setProductStrings(getDeviceAttribute(udevDevice, ATTR_MANUFACTURER), getDeviceAttribute(udevDevice
                    , ATTR_PRODUCT), getDeviceAttribute(udevDevice, ATTR_SERIAL));

            return device;

        } catch (Exception e) {
            LOG.log(INFO, String.format("failed to retrieve information about device 0x%04x/0x%04x - ignoring device", vendorId, productId), e);
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
}
