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
import java.util.stream.Collectors;

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
            throw new UnsupportedOperationException(String.format("JavaCanDoUsb is not implemented for architecture " +
                    "%s/%s", osName, osArch));
        }
        return impl;
    }

    private static USBDeviceRegistry _instance = null;

    private static synchronized USBDeviceRegistry instance() {
        if (_instance == null) {
            _instance = createInstance();
            _instance.start();
        }
        return _instance;
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
     * Gets a list of connected USB devices matching the specified filter.
     *
     * @param filter device filter
     * @return list of USB devices
     */
    public static List<USBDevice> getDevices(USBDeviceFilter filter) {
        return instance().getAllDevices().stream().filter(filter::matches).collect(Collectors.toList());
    }

    /**
     * Gets a list of connected USB devices matching any of the specified filters.
     *
     * @param filters list of device filters
     * @return list of USB devices
     */
    public static List<USBDevice> getDevices(List<USBDeviceFilter> filters) {
        return instance().getAllDevices().stream().filter(dev -> USBDeviceFilter.matchesAny(dev, filters))
                .collect(Collectors.toList());
    }

    /**
     * Gets the first connected USB device matching the specified filter.
     *
     * @param filter device filter
     * @return USB device, or {@code null} if no device matches
     */
    public static USBDevice getDevice(USBDeviceFilter filter) {
        return instance().getAllDevices().stream().filter(filter::matches).findFirst().orElse(null);
    }

    /**
     * Gets the first connected USB device matching any of the specified filters.
     *
     * @param filters list of device filters
     * @return USB device, or {@code null} if no device matches
     */
    public static USBDevice getDevice(List<USBDeviceFilter> filters) {
        return instance().getAllDevices().stream().filter(dev -> USBDeviceFilter.matchesAny(dev, filters))
                .findFirst().orElse(null);
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
     *
     * @param handler handler function, or {@code null} to remove a previous handler
     */
    public static void setOnDeviceDisconnected(Consumer<USBDevice> handler) {
        instance().setOnDeviceDisconnected(handler);
    }
}
