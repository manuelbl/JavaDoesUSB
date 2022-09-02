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
import java.util.function.BiConsumer;

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
            iterateDevices(deviceConnectedIter, false, (entryId, service) -> {
                var device = createDevice(entryId, service);
                if (device != null)
                    deviceList.add(device);
            });
            setInitialDeviceList(deviceList);

            // setup notification for disconnected devices
            var onDeviceDisconnectedMH = MethodHandles.lookup().findVirtual(MacosUSBDeviceRegistry.class,
                    "onDevicesDisconnected", MethodType.methodType(void.class, MemoryAddress.class, int.class));
            int deviceDisconnectedIter = setupNotification(session, notifyPort, IoKit.kIOTerminatedNotification,
                    onDeviceDisconnectedMH);

            // iterate current devices in order to arm the notifications
            onDevicesDisconnected(NULL, deviceDisconnectedIter);

            // loop forever
            CoreFoundation.CFRunLoopRun();

        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the devices resulting from the iterator
     *
     * @param iterator       the iterator
     * @param isDisconnected flag is the iterator is for disconnected devices
     * @param function       a function that will be called for each device with the entry ID and the service
     */
    private void iterateDevices(int iterator, boolean isDisconnected, BiConsumer<Long, Integer> function) {

        int svc;
        while ((svc = IoKit.IOIteratorNext(iterator)) != 0) {
            try (var session = MemorySession.openConfined()) {

                final int service = svc;
                session.addCloseAction(() -> IoKit.IOObjectRelease(service));

                // test if service has user client interface (if not, it is likely a controller)
                if (!isDisconnected) {
                    final var device = IoKitHelper.GetInterface(service, IoKit.kIOUSBDeviceUserClientTypeID,
                            IoKit.kIOUSBDeviceInterfaceID100);
                    if (device == null) continue;
                    IoKit.Release(device);
                }

                // get entry ID (as unique ID)
                var entryIdHolder = session.allocate(JAVA_LONG);
                int ret = IoKit.IORegistryEntryGetRegistryEntryID(service, entryIdHolder);
                if (ret != 0) throw new MacosUSBException("IORegistryEntryGetRegistryEntryID failed", ret);
                var entryId = entryIdHolder.get(JAVA_LONG, 0);

                // call function to process device
                function.accept(entryId, service);
            }
        }
    }

    private USBDevice createDevice(Long entryID, int service) {

        Integer vendorId = IoKitHelper.GetPropertyInt(service, "idVendor");
        Integer productId = IoKitHelper.GetPropertyInt(service, "idProduct");
        String manufacturer = IoKitHelper.GetPropertyString(service, "kUSBVendorString");
        String product = IoKitHelper.GetPropertyString(service, "kUSBProductString");
        String serial = IoKitHelper.GetPropertyString(service, "kUSBSerialNumberString");
        Integer classCode = IoKitHelper.GetPropertyInt(service, "bDeviceClass");
        Integer subclassCode = IoKitHelper.GetPropertyInt(service, "bDeviceSubClass");
        Integer protocolCode = IoKitHelper.GetPropertyInt(service, "bDeviceProtocol");

        if (vendorId == null || productId == null || classCode == null || subclassCode == null || protocolCode == null)
            return null;

        return new MacosUSBDevice(entryID, vendorId, productId, manufacturer, product, serial, classCode,
                subclassCode, protocolCode);
    }

    private int setupNotification(MemorySession session, MemoryAddress notifyPort, MemorySegment notificationType,
                                  MethodHandle callback) throws NoSuchMethodException, IllegalAccessException {

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
        iterateDevices(iterator, false, (entryId, service) -> {
            var device = createDevice(entryId, service);
            if (device != null)
                addDevice(device);
        });
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
        iterateDevices(iterator, true, (entryId, service) -> removeDevice(entryId));
    }
}
