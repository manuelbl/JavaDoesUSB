//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.linux;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.common.USBDeviceInfoImpl;

public class LinuxUSBDeviceInfo extends USBDeviceInfoImpl {

    LinuxUSBDeviceInfo(
            Object id, int vendorId, int productId,
            String manufacturer, String product, String serial,
            int classCode, int subclassCode, int protocolCode) {

        super(id, vendorId, productId,
                manufacturer, product, serial,
                classCode, subclassCode, protocolCode);
    }

    @Override
    public USBDevice open() {
        return new LinuxUSBDevice(id, this);
    }
}
