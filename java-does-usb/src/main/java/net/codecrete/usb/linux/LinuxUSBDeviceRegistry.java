//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.linux.gen.udev.udev;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;

/**
 * Linux implementation of USB device registry.
 * <p>
 * This singleton class maintains a list of connected USB devices.
 * It starts a background thread monitoring the USB devices being
 * connected and disconnected.
 * </p>
 * <p>
 * The background thread also enumerates the already present devices
 * and builds the initial device list.
 * </p>
 */
public class LinuxUSBDeviceRegistry extends USBDeviceRegistry {

    private static final MemorySegment SUBSYSTEM_USB = MemorySession.global().allocateUtf8String("usb");

    /**
     * Creates a new instance.
     */
    public LinuxUSBDeviceRegistry() {
        enumeratePresentDevices();
    }

    @Override
    public List<USBDeviceInfo> getAllDevices() {
        return devices;
    }

    private void enumeratePresentDevices() {
        List<USBDeviceInfo> result = new ArrayList<>();
        try (var outerSession = MemorySession.openConfined()) {

            var udevInstance = udev.udev_new();

            // create device enumerator
            var enumerate = udev.udev_enumerate_new(udevInstance);
            outerSession.addCloseAction(() -> udev.udev_enumerate_unref(enumerate));
            udev.udev_enumerate_add_match_subsystem(enumerate, SUBSYSTEM_USB);
            udev.udev_enumerate_scan_devices(enumerate);

            // enumerate devices
            for (var entry= udev.udev_enumerate_get_list_entry(enumerate);
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

                    session.addCloseAction(() -> udev.udev_device_unref(dev));

                    // retrieve device attributes
                    String idVendor = getDeviceAttribute(dev, "idVendor");
                    if (idVendor == null)
                        continue;

                    String idProduct = getDeviceAttribute(dev, "idProduct");
                    if (idProduct == null)
                        continue;

                    // get device path
                    var devPath = getDeviceName(dev);
                    if (devPath == null)
                        continue;

                    int vendorId = Integer.parseInt(idVendor, 16);
                    int productId = Integer.parseInt(idProduct, 16);

                    // add device to result list
                    result.add(new LinuxUSBDeviceInfo(devPath, vendorId, productId,
                            getDeviceAttribute(dev, "manufacturer"),
                            getDeviceAttribute(dev, "product"),
                            getDeviceAttribute(dev, "serial"),
                            0, 0, 0));
                }
            }
        }

        devices = result;
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
        try (var session = MemorySession.openConfined()) {
            var devNameAddr = udev.udev_device_get_devnode(device);
            if (devNameAddr == NULL)
                return null;

            var ret = MemorySegment.ofAddress(devNameAddr, 2000, session);
            return ret.getUtf8String(0);
        }
    }
}
