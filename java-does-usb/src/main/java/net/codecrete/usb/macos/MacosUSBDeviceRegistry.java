//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.common.USBDeviceRegistry;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

/**
 * MacOS implementation of USB device registry.
 */
public class MacosUSBDeviceRegistry extends USBDeviceRegistry {

    /**
     * Monitors the USB devices.
     * <p>
     * This method is the core of the background thread. It runs forever and does not terminate.
     * </p>
     */
    @Override
    protected void monitorDevices() {

        // as the method runs forever, there is no need to clean up one-time allocations
        try (var session = MemorySession.openConfined()) {

            try {

                // setup run loop, run loop source and notification port
                var notifyPort = IoKit.IONotificationPortCreate(IoKit.kIOMasterPortDefault);
                var runLoopSource = IoKit.IONotificationPortGetRunLoopSource(notifyPort);
                var runLoop = CoreFoundation.CFRunLoopGetCurrent();
                CoreFoundation.CFRunLoopAddSource(runLoop, runLoopSource, IoKit.kCFRunLoopDefaultMode);

                // setup notification for connected devices
                var onDeviceConnectedMH = MethodHandles.lookup().findVirtual(MacosUSBDeviceRegistry.class,
                        "onDevicesConnected", MethodType.methodType(void.class, MemoryAddress.class, int.class));
                int deviceConnectedIter = setupNotification(session, notifyPort, IoKit.kIOFirstMatchNotification,
                        onDeviceConnectedMH);

                // iterate current devices in order to arm the notifications (and build initial device list)
                var deviceList = new ArrayList<USBDevice>();
                iterateDevices(deviceConnectedIter, (device) -> deviceList.add(device));
                setInitialDeviceList(deviceList);

                // setup notification for disconnected devices
                var onDeviceDisconnectedMH = MethodHandles.lookup().findVirtual(MacosUSBDeviceRegistry.class,
                        "onDevicesDisconnected", MethodType.methodType(void.class, MemoryAddress.class, int.class));
                int deviceDisconnectedIter = setupNotification(session, notifyPort, IoKit.kIOTerminatedNotification,
                        onDeviceDisconnectedMH);

                // iterate current devices in order to arm the notifications
                onDevicesDisconnected(NULL, deviceDisconnectedIter);

            } catch (Throwable e) {
                enumerationFailed(e);
                return;
            }

            // loop forever
            CoreFoundation.CFRunLoopRun();
        }
    }

    /**
     * Process the devices resulting from the iterator
     *
     * @param iterator       the iterator
     * @param consumer       a consumer that will be called for each device with the entry ID and the service
     */
    private void iterateDevices(int iterator, IOKitDeviceConsumer consumer) {

        int svc;
        while ((svc = IoKit.IOIteratorNext(iterator)) != 0) {
            try (var session = MemorySession.openConfined()) {

                final int service = svc;
                session.addCloseAction(() -> IoKit.IOObjectRelease(service));

                var device = IoKitHelper.getInterface(service, IoKit.kIOUSBDeviceUserClientTypeID,
                        IoKit.kIOUSBDeviceInterfaceID100);

                if (device != null)
                    session.addCloseAction(() -> IoKit.Release(device));

                // get entry ID (as unique ID)
                var entryIdHolder = session.allocate(JAVA_LONG);
                int ret = IoKit.IORegistryEntryGetRegistryEntryID(service, entryIdHolder);
                if (ret != 0)
                    throw new MacosUSBException("IORegistryEntryGetRegistryEntryID failed", ret);
                var entryId = entryIdHolder.get(JAVA_LONG, 0);

                // call consumer to process device
                consumer.accept(entryId, service, device);
            }
        }
    }

    /**
     * Calls the consumer for all devices produced by the iterator.
     * <p>
     * This method tries to create a {@link USBDevice} instance.
     * If it fails, an information is printed, but the consumer is not called.
     * </p>
     *
     * @param iterator the iterator
     * @param consumer the consumer
     */
    private void iterateDevices(int iterator, Consumer<USBDevice> consumer) {
        iterateDevices(iterator, (entryId, service, deviceIntf) -> {

            var deviceInfo = new VidPid();
            try {
                var device = createDevice(entryId, service, deviceIntf, deviceInfo);
                if (device != null)
                    consumer.accept(device);

            } catch (Throwable e) {
                System.err.printf(
                        "Info: [JavaDoesUSB] failed to retrieve information about device 0x%04x/0x%04x - ignoring device%n",
                        deviceInfo.vid, deviceInfo.pid);
                e.printStackTrace(System.err);
            }
        });
    }

    private USBDevice createDevice(Long entryID, int service, MemoryAddress deviceIntf, VidPid info) {

        if (deviceIntf == null)
            return null;

        Integer vendorId = IoKitHelper.getPropertyInt(service, "idVendor");
        Integer productId = IoKitHelper.getPropertyInt(service, "idProduct");
        if (vendorId == null || productId == null)
            return null;

        info.vid = vendorId;
        info.pid = productId;

        String manufacturer = IoKitHelper.getPropertyString(service, "kUSBVendorString");
        String product = IoKitHelper.getPropertyString(service, "kUSBProductString");
        String serial = IoKitHelper.getPropertyString(service, "kUSBSerialNumberString");

        var device = new MacosUSBDevice(deviceIntf, entryID, vendorId, productId, manufacturer, product, serial);

        Integer classCode = IoKitHelper.getPropertyInt(service, "bDeviceClass");
        Integer subclassCode = IoKitHelper.getPropertyInt(service, "bDeviceSubClass");
        Integer protocolCode = IoKitHelper.getPropertyInt(service, "bDeviceProtocol");

        device.setClassCodes(classCode != null ? classCode : 0,
                subclassCode != null ? subclassCode : 0,
                protocolCode != null ? protocolCode : 0);

        return device;
    }

    private int setupNotification(MemorySession session, MemoryAddress notifyPort, MemorySegment notificationType,
                                  MethodHandle callback) {

        // new matching dictionary for (dis)connected device notifications
        MemoryAddress matchingDict = IoKit.IOServiceMatching(IoKit.kIOUSBDeviceClassName);

        // create callback stub
        var onDeviceCallbackStub = Linker.nativeLinker().upcallStub(callback.bindTo(this),
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT), session);

        // Set up a notification to be called when a device is first matched / terminated by I/O Kit.
        // This method consumes the matchingDict reference.
        var deviceIterHolder = session.allocate(JAVA_INT);
        int ret = IoKit.IOServiceAddMatchingNotification(notifyPort, notificationType, matchingDict,
                onDeviceCallbackStub, NULL, deviceIterHolder);
        if (ret != 0) throw new MacosUSBException("IOServiceAddMatchingNotification failed", ret);

        return deviceIterHolder.get(JAVA_INT, 0);
    }

    /**
     * Callback function for monitoring connected USB devices.
     * <p>
     * This method is used in an upcall from native code.
     * </p>
     *
     * @param ignoredRefCon ignored parameter
     * @param iterator      device iterator
     */
    private void onDevicesConnected(MemoryAddress ignoredRefCon, int iterator) {

        // process device iterator for connected devices
        iterateDevices(iterator, this::addDevice);
    }

    /**
     * Callback function for monitoring disconnected USB devices.
     * <p>
     * This method is used in an upcall from native code.
     * </p>
     *
     * @param ignoredRefCon ignored parameter
     * @param iterator      device iterator
     */
    private void onDevicesDisconnected(MemoryAddress ignoredRefCon, int iterator) {

        // process device iterator for disconnected devices
        iterateDevices(iterator, (entryId, service, deviceIntf) -> {
            var device = findDevice(entryId);
            if (device == null)
                return;

            try {
                ((MacosUSBDevice) device).closeFully();
            } catch (Throwable e) {
                System.err.println("Info: [JavaDoesUSB] failed to close USB device - ignoring exception");
                e.printStackTrace(System.err);
            }

            removeDevice(entryId);
        });
    }

    @FunctionalInterface
    interface IOKitDeviceConsumer {
        void accept(long entryId, int service, MemoryAddress deviceIntf);
    }

    static class VidPid {
        int vid;
        int pid;
    }
}
