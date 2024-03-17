//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import net.codecrete.usb.common.UsbDeviceRegistry;
import net.codecrete.usb.linux.LinuxUsbDeviceRegistry;
import net.codecrete.usb.macos.MacosUsbDeviceRegistry;
import net.codecrete.usb.windows.WindowsUsbDeviceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Provides access to USB devices.
 */
public class Usb {

    private static UsbDeviceRegistry createInstance() {
        var osName = System.getProperty("os.name");
        var osArch = System.getProperty("os.arch");

        UsbDeviceRegistry impl;
        if (osName.equals("Mac OS X") && (osArch.equals("x86_64") || osArch.equals("aarch64"))) {
            impl = new MacosUsbDeviceRegistry();
        } else if (osName.startsWith("Windows") && osArch.equals("amd64")) {
            impl = new WindowsUsbDeviceRegistry();
        } else if (osName.equals("Linux") && (osArch.equals("amd64") || osArch.equals("aarch64"))) {
            impl = new LinuxUsbDeviceRegistry();
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Java Does USB has no implementation for architecture %s/%s",
                    osName, osArch));
        }
        return impl;
    }

    private static UsbDeviceRegistry singletonInstance = null;

    private static synchronized UsbDeviceRegistry instance() {
        if (singletonInstance == null) {
            singletonInstance = createInstance();
            singletonInstance.start();
        }
        return singletonInstance;
    }

    // Private, so no instance can be created
    private Usb() {
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
    public static @NotNull @Unmodifiable Collection<UsbDevice> getDevices() {
        return Collections.unmodifiableCollection(instance().getAllDevices());
    }

    /**
     * Gets a list of connected USB devices matching the specified predicate.
     *
     * @param predicate device predicate
     * @return list of USB devices
     */
    public static @NotNull @Unmodifiable List<UsbDevice> findDevices(@NotNull UsbDevicePredicate predicate) {
        return instance().getAllDevices().stream().filter(predicate::matches).toList();
    }

    /**
     * Gets the first connected USB device matching the specified predicate.
     *
     * @param predicate device predicate
     * @return optional USB device
     */
    public static Optional<UsbDevice> findDevice(@NotNull UsbDevicePredicate predicate) {
        return instance().getAllDevices().stream().filter(predicate::matches).findFirst();
    }

    /**
     * Gets the first connected USB device with the specified vendor and product ID.
     *
     * @param vendorId vendor ID
     * @param productId product ID
     * @return optional USB device
     */
    public static Optional<UsbDevice> findDevice(int vendorId, int productId) {
        return findDevice(device -> device.getVendorId() == vendorId && device.getProductId() == productId);
    }

    /**
     * Sets the handler to be called when a USB device is connected.
     * <p>
     * The handler is called from a background thread. 
     * </p>
     * <p>
     * The handler should not execute any time-consuming operations but rather return quickly.
     * While the handler is being executed, maintaining the list of connected devices is paused,
     * methods of this class (such as {@link #getDevices()}) will possibly work with an outdated list
     * of connected devices and handlers for connect and disconnect events will not be called.
     * </p>
     *
     * @param handler handler function, or {@code null} to remove a previous handler
     */
    public static void setOnDeviceConnected(@Nullable Consumer<UsbDevice> handler) {
        instance().setOnDeviceConnected(handler);
    }

    /**
     * Sets the handler to be called when a USB device is disconnected.
     * <p>
     * The handler is called from a background thread. 
     * </p>
     * <p>
     * When the handler is called, the {@link UsbDevice} instance has already been closed.
     * Descriptive information (such as vendor and product ID, serial number, interfaces, endpoints)
     * can still be accessed.
     * </p>
     * <p>
     * If the application was communicating with the device when it was disconnected, it will also receive
     * an error for those operations. Due to the concurrency of the USB stack, there is no particular order
     * for the disconnect event and the transmission errors.
     * </p>
     * <p>
     * The handler should not execute any time-consuming operations but rather return quickly.
     * While the handler is being executed, maintaining the list of connected devices is paused,
     * methods of this class (such as {@link #getDevices()}) will possibly work with an outdated list
     * of connected devices and handlers for connect and disconnect events will not be called.
     * </p>
     *
     * @param handler handler function, or {@code null} to remove a previous handler
     */
    public static void setOnDeviceDisconnected(@Nullable Consumer<UsbDevice> handler) {
        instance().setOnDeviceDisconnected(handler);
    }
}
