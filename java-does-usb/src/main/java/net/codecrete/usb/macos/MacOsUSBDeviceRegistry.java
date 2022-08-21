//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.common.USBDeviceRegistry;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySession;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class MacOsUSBDeviceRegistry implements USBDeviceRegistry {

    private static final long NUMBER_TYPE_ID = CoreFoundation.CFNumberGetTypeID();
    private static final long STRING_TYPE_ID = CoreFoundation.CFStringGetTypeID();

    public List<USBDeviceInfo> getAllDevices() {

        List<USBDeviceInfo> result = new ArrayList<>();
        try (var outerSession = MemorySession.openConfined()) {

            final int entry = IoKit.IORegistryGetRootEntry(IoKit.kIOMasterPortDefault);
            if (entry == 0)
                throw new RuntimeException("IORegistryGetRootEntry failed");
            outerSession.addCloseAction(() -> IoKit.IOObjectRelease(entry));

            var iterHolder = outerSession.allocate(JAVA_INT);
            int ret = IoKit.IORegistryCreateIterator(0, IoKit.kIOUSBPlane, IoKit.kIORegistryIterateRecursively, iterHolder);
            final var iter = iterHolder.get(JAVA_INT, 0);
            if (ret != 0)
                throw new RuntimeException("IORegistryCreateIterator failed");
            outerSession.addCloseAction(() -> IoKit.IOObjectRelease(iter));

            int svc;
            while ((svc = IoKit.IOIteratorNext(iter)) != 0) {
                try (var session = MemorySession.openConfined()) {

                    final int service = svc;
                    session.addCloseAction(() -> IoKit.IOObjectRelease(service));

                    // Test if service has user client interface (if not, it is likely a hub)
                    final MemoryAddress device = IoKit.GetInterface(service, IoKit.kIOUSBDeviceUserClientTypeID, IoKit.kIOUSBDeviceInterfaceID100);
                    if (device == null)
                        continue;
                    IoKit.Release(device);

                    // Get registry path
                    var path = session.allocateArray(JAVA_BYTE, 512);
                    ret = IoKit.IORegistryEntryGetPath(service, IoKit.kIOServicePlane, path);
                    if (ret != 0)
                        continue;

                    var deviceInfo = createDeviceInfo(path.getUtf8String(0), service);

                    if (deviceInfo != null)
                        result.add(deviceInfo);
                }
            }
        }

        return result;
    }

    private USBDeviceInfo createDeviceInfo(String path, int service) {
        Integer vendorId = GetPropertyInt(service, "idVendor");
        Integer productId = GetPropertyInt(service, "idProduct");
        String manufacturer = GetPropertyString(service, "kUSBVendorString");
        String product = GetPropertyString(service, "kUSBProductString");
        String serial = GetPropertyString(service, "kUSBSerialNumberString");
        Integer classCode = GetPropertyInt(service, "bDeviceClass");
        Integer subclassCode = GetPropertyInt(service, "bDeviceSubClass");
        Integer protocolCode = GetPropertyInt(service, "bDeviceProtocol");

        if (vendorId == null || productId == null || classCode == null || subclassCode == null || protocolCode == null)
            return null;

        return new MacosUSBDeviceInfo(path, vendorId, productId, manufacturer, product, serial, classCode, subclassCode, protocolCode);
    }

    private static Integer GetPropertyInt(int service, String key) {
        var value = IoKit.IORegistryEntryCreateCFProperty(service, key, NULL, 0);
        if (value == NULL)
            return null;

        Integer result = null;
        var type = CoreFoundation.CFGetTypeID(value);
        if (type == NUMBER_TYPE_ID) {

            try (var session = MemorySession.openConfined()) {
                var numberValue = session.allocate(JAVA_INT, 0);
                if (CoreFoundation.CFNumberGetValue(value, CoreFoundation.kCFNumberSInt32Type, numberValue))
                    result = numberValue.get(JAVA_INT, 0);
            }
        }

        CoreFoundation.CFRelease(value);
        return result;
    }

    private static String GetPropertyString(int service, String key) {
        var value = IoKit.IORegistryEntryCreateCFProperty(service, key, NULL, 0);
        if (value == NULL)
            return null;

        String result = null;
        var type = CoreFoundation.CFGetTypeID(value);
        if (type == STRING_TYPE_ID)
            result = CoreFoundation.cfStringToJavaString(value);

        CoreFoundation.CFRelease(value);
        return result;
    }
}
