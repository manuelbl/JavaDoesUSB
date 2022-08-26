//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.common.USBDeviceRegistry;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * USB implementation for Linux
 */
public class LinuxUSBDeviceRegistry implements USBDeviceRegistry {
    @Override
    public List<USBDeviceInfo> getAllDevices() {

        List<USBDeviceInfo> result = new ArrayList<>();
        try (var session = MemorySession.openConfined()) {
            // create enumerator
            var eHolder = session.allocate(ADDRESS);
            Systemd.sd_device_enumerator_new(eHolder);
            var e = eHolder.get(ADDRESS, 0);
            session.addCloseAction(() -> Systemd.sd_device_enumerator_unref(e));

            // restrict to USB devices
            Systemd.sd_device_enumerator_add_match_subsystem(e, "usb", 1);

            // iterate devices
            for (var dev = Systemd.sd_device_enumerator_get_device_first(e);
                 dev != NULL;
                 dev = Systemd.sd_device_enumerator_get_device_next(e)
            ) {
                // retrieve device attributes
                String idVendor = getDeviceAttribute(dev, "idVendor");
                if (idVendor == null)
                    continue;

                String idProduct = getDeviceAttribute(dev, "idProduct");
                if (idProduct == null)
                    continue;

                String devPath = getDeviceName(dev);
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

        return result;
    }

    private static String getDeviceAttribute(Addressable device, String attribute) {
        try (var session = MemorySession.openConfined()) {
            var sysattr = session.allocateUtf8String(attribute);
            var valueHolder = session.allocate(ADDRESS);
            int res = Systemd.sd_device_get_sysattr_value(device, sysattr, valueHolder);
            if (res != 0)
                return null;

            var valuePointer = valueHolder.get(ADDRESS, 0);
            var value = MemorySegment.ofAddress(valuePointer, 2000, session);
            return value.getUtf8String(0);
        }
    }

    private static String getDeviceName(Addressable device) {
        try (var session = MemorySession.openConfined()) {
            var retHolder = session.allocate(ADDRESS);
            int res = Systemd.sd_device_get_devname(device, retHolder);
            if (res != 0)
                return null;

            var retPointer = retHolder.get(ADDRESS, 0);
            var ret = MemorySegment.ofAddress(retPointer, 2000, session);
            return ret.getUtf8String(0);
        }
    }

    @Override
    public void setOnDeviceConnected(Consumer<USBDeviceInfo> handler) {
    }

    @Override
    public void setOnDeviceDisconnected(Consumer<USBDeviceInfo> handler) {
    }
}
