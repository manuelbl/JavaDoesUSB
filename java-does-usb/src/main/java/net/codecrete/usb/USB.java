//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.linux.LinuxUSBDeviceRegistry;
import net.codecrete.usb.macos.MacosUSBDeviceRegistry;
import net.codecrete.usb.windows.WindowsUSBDeviceRegistry;

import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

/**
 * Provides access to USB devices.
 */
public class USB {

    private static USBDeviceRegistry createInstance() {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        USBDeviceRegistry impl;
        if (osName.equals("Mac OS X") && (osArch.equals("x86_64") || osArch.equals("aarch64"))) {
            impl = new MacosUSBDeviceRegistry();
        } else if (osName.startsWith("Windows") && osArch.equals("amd64")) {
            impl = new WindowsUSBDeviceRegistry();
        } else if (osName.equals("Linux") && (osArch.equals("amd64") || osArch.equals("aarch64"))) {
            impl = new LinuxUSBDeviceRegistry();
        } else {
            throw new UnsupportedOperationException(String.format("JavaCanDoUsb is not implemented for architecture " + "%s/%s", osName, osArch));
        }
        return impl;
    }

    private static USBDeviceRegistry singletonInstance = null;

    private static synchronized USBDeviceRegistry instance() {
        if (singletonInstance == null) {
            singletonInstance = createInstance();
            singletonInstance.start();
        }
        return singletonInstance;
    }

    // Private, so no instance can be created
    private USB() {
    }

    /**
     * Gets a list of all connected USB devices.
     *
     * <p>
     * Depending on the operating system, the list might or might not include
     * USB hubs and USB host controllers.
     * </p>
     *
     * @return list of USB devices
     */
    public static List<USBDevice> getAllDevices() {
        return instance().getAllDevices();
    }

    /**
     * Gets a list of connected USB devices matching the specified predicate.
     *
     * @param predicate device predicate/filter
     * @return list of USB devices
     */
    public static List<USBDevice> getDevices(USBDevicePredicate predicate) {
        return instance().getAllDevices().stream().filter(predicate::matches).collect(toList());
    }

    /**
     * Gets a list of connected USB devices matching any of the specified predicates/filters.
     *
     * @param predicates list of device predicates/filters
     * @return list of USB devices
     */
    public static List<USBDevice> getDevices(List<USBDevicePredicate> predicates) {
        return instance().getAllDevices().stream().filter(dev -> USBDevicePredicate.matchesAny(dev, predicates)).collect(toList());
    }

    /**
     * Gets the first connected USB device matching the specified predicate.
     *
     * @param predicate device predicate/filter
     * @return USB device, or {@code null} if no device matches
     */
    public static USBDevice getDevice(USBDevicePredicate predicate) {
        return instance().getAllDevices().stream().filter(predicate::matches).findFirst().orElse(null);
    }

    /**
     * Gets the first connected USB device matching any of the specified predicates.
     *
     * @param predicates list of device predicates/filters
     * @return USB device, or {@code null} if no device matches
     */
    public static USBDevice getDevice(List<USBDevicePredicate> predicates) {
        return instance().getAllDevices().stream().filter(dev -> USBDevicePredicate.matchesAny(dev, predicates)).findFirst().orElse(null);
    }

    /**
     * Sets the handler to be called when a USB device is connected.
     *
     * @param handler handler function, or {@code null} to remove a previous handler
     */
    public static void setOnDeviceConnected(Consumer<USBDevice> handler) {
        instance().setOnDeviceConnected(handler);
    }

    /**
     * Sets the handler to be called when a USB device is disconnected.
     * <p>
     * When the handler is called, the {@link USBDevice} instance has already been closed.
     * Descriptive information (such as vendor and product ID, serial number, interfaces, endpoints)
     * can still be accessed.
     * </p>
     *
     * @param handler handler function, or {@code null} to remove a previous handler
     */
    public static void setOnDeviceDisconnected(Consumer<USBDevice> handler) {
        instance().setOnDeviceDisconnected(handler);
    }
}
