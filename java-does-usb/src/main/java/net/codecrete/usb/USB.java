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
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Provides access to USB devices.
 */
public class USB {

    private static USBDeviceRegistry createInstance() {
        var osName = System.getProperty("os.name");
        var osArch = System.getProperty("os.arch");

        USBDeviceRegistry impl;
        if (osName.equals("Mac OS X") && (osArch.equals("x86_64") || osArch.equals("aarch64"))) {
            impl = new MacosUSBDeviceRegistry();
        } else if (osName.startsWith("Windows") && osArch.equals("amd64")) {
            impl = new WindowsUSBDeviceRegistry();
        } else if (osName.equals("Linux") && (osArch.equals("amd64") || osArch.equals("aarch64"))) {
            impl = new LinuxUSBDeviceRegistry();
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Java Does USB has no implementation for architecture %s/%s",
                    osName, osArch));
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
     * @param predicate device predicate
     * @return list of USB devices
     */
    public static List<USBDevice> getDevices(USBDevicePredicate predicate) {
        return instance().getAllDevices().stream().filter(predicate::matches).toList();
    }

    /**
     * Gets the first connected USB device matching the specified predicate.
     *
     * @param predicate device predicate
     * @return optional USB device
     */
    public static Optional<USBDevice> getDevice(USBDevicePredicate predicate) {
        return instance().getAllDevices().stream().filter(predicate::matches).findFirst();
    }

    /**
     * Gets the first connected USB device with the specified vendor and product ID.
     *
     * @param vendorId vendor ID
     * @param productId product ID
     * @return optional USB device
     */
    public static Optional<USBDevice> getDevice(int vendorId, int productId) {
        return getDevice(device -> device.vendorId() == vendorId && device.productId() == productId);
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
