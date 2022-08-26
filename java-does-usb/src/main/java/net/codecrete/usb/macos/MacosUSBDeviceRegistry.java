//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBDeviceInfo;
import net.codecrete.usb.common.USBDeviceRegistry;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySession;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

public class MacosUSBDeviceRegistry implements USBDeviceRegistry {

    private Consumer<USBDeviceInfo> onDeviceConnectedHandler;
    private Consumer<USBDeviceInfo> onDeviceDisconnectedHandler;
    private boolean isMonitorThreadStarted;

    public List<USBDeviceInfo> getAllDevices() {

        try (var session = MemorySession.openConfined()) {

            final int entry = IoKit.IORegistryGetRootEntry(IoKit.kIOMasterPortDefault);
            if (entry == 0)
                throw new RuntimeException("IORegistryGetRootEntry failed");
            session.addCloseAction(() -> IoKit.IOObjectRelease(entry));

            var iterHolder = session.allocate(JAVA_INT);
            int ret = IoKit.IORegistryCreateIterator(0, IoKit.kIOUSBPlane, IoKit.kIORegistryIterateRecursively, iterHolder);
            final var iter = iterHolder.get(JAVA_INT, 0);
            if (ret != 0)
                throw new RuntimeException("IORegistryCreateIterator failed");
            session.addCloseAction(() -> IoKit.IOObjectRelease(iter));

            return iterateDevices(iter, false);
        }
    }

    /**
     * Return all devices of the iterator in a list
     */
    private List<USBDeviceInfo> iterateDevices(int iter, boolean isDisconnected) {

        List<USBDeviceInfo> result = new ArrayList<>();

        int svc;
        while ((svc = IoKit.IOIteratorNext(iter)) != 0) {
            try (var session = MemorySession.openConfined()) {

                final int service = svc;
                session.addCloseAction(() -> IoKit.IOObjectRelease(service));

                // Test if service has user client interface (if not, it is likely a hub)
                if (!isDisconnected) {
                    final MemoryAddress device = IoKitHelper.GetInterface(service, IoKit.kIOUSBDeviceUserClientTypeID, IoKit.kIOUSBDeviceInterfaceID100);
                    if (device == null)
                        continue;
                    IoKit.Release(device);
                }

                // Get registry path
                String path;
                var pathSegment = session.allocateArray(JAVA_BYTE, 512);
                int ret = IoKit.IORegistryEntryGetPath(service, IoKit.kIOServicePlane, pathSegment);
                if (ret == 0) {
                    path = pathSegment.getUtf8String(0);
                } else if (isDisconnected) {
                    path = "<disconnected>";
                } else {
                    continue;
                }

                // Get entry ID
                var entryIdHolder = session.allocate(JAVA_LONG);
                ret = IoKit.IORegistryEntryGetRegistryEntryID(service, entryIdHolder);
                if (ret != 0)
                    throw new MacosUSBException("IORegistryEntryGetRegistryEntryID failed", ret);
                var entryId = entryIdHolder.get(JAVA_LONG, 0);

                var deviceInfo = createDeviceInfo(entryId, service);

                if (deviceInfo != null)
                    result.add(deviceInfo);
            }
        }

        return result;
    }

    private synchronized void startDeviceMonitor() {
        if (isMonitorThreadStarted)
            return;

        isMonitorThreadStarted = true;
        Thread t = new Thread(this::monitorDevices, "USB device monitor");
        t.start();
    }

    private void monitorDevices() {

        try (var session = MemorySession.openConfined()) {

            var notifyPort = IoKit.IONotificationPortCreate(IoKit.kIOMasterPortDefault);
            var runLoopSource = IoKit.IONotificationPortGetRunLoopSource(notifyPort);

            var runLoop = CoreFoundation.CFRunLoopGetCurrent();
            CoreFoundation.CFRunLoopAddSource(runLoop, runLoopSource, IoKit.kCFRunLoopDefaultMode);

            var matchingDict = IoKit.IOServiceMatching(IoKit.kIOUSBDeviceClassName);

            // create callback stub (connected devices)
            var onDeviceConnectedMH = MethodHandles.lookup().findVirtual(
                    MacosUSBDeviceRegistry.class,
                    "onDeviceConnected",
                    MethodType.methodType(void.class, MemoryAddress.class, int.class)
            ).bindTo(this);
            var onDeviceConnectedStub = Linker.nativeLinker().upcallStub(
                    onDeviceConnectedMH,
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT),
                    session
            );

            // Set up a notification to be called when a device is first matched by I/O Kit.
            // This method consumes the matchingDict reference.
            var deviceIterHolder = session.allocate(JAVA_INT);
            int ret = IoKit.IOServiceAddMatchingNotification(notifyPort, IoKit.kIOFirstMatchNotification,
                    matchingDict, onDeviceConnectedStub, NULL, deviceIterHolder);
            if (ret != 0)
                throw new MacosUSBException("IOServiceAddMatchingNotification failed", ret);
            var deviceConnectedIter = deviceIterHolder.get(JAVA_INT, 0);

            // iterate current devices in order to arm the notifications
            onDeviceConnected(NULL, deviceConnectedIter);

            // new matching dictionary for disconnected notifications
            matchingDict = IoKit.IOServiceMatching(IoKit.kIOUSBDeviceClassName);

            // create callback stub (connected devices)
            var onDeviceDisconnectedMH = MethodHandles.lookup().findVirtual(
                    MacosUSBDeviceRegistry.class,
                    "onDeviceDisconnected",
                    MethodType.methodType(void.class, MemoryAddress.class, int.class)
            ).bindTo(this);
            var onDeviceDisconnectedStub = Linker.nativeLinker().upcallStub(
                    onDeviceDisconnectedMH,
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT),
                    session
            );

            // Set up a notification to be called when a device is terminated by I/O Kit.
            // This method consumes the matchingDict reference.
            deviceIterHolder.set(JAVA_INT, 0, 0);
            ret = IoKit.IOServiceAddMatchingNotification(notifyPort, IoKit.kIOTerminatedNotification,
                    matchingDict, onDeviceDisconnectedStub, NULL, deviceIterHolder);
            if (ret != 0)
                throw new MacosUSBException("IOServiceAddMatchingNotification (2) failed", ret);
            var deviceDisconnectedIter = deviceIterHolder.get(JAVA_INT, 0);

            // iterate current devices in order to arm the notifications
            onDeviceDisconnected(NULL, deviceDisconnectedIter);

            CoreFoundation.CFRunLoopRun();

        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void onDeviceConnected(MemoryAddress ignoredRefCon, int iterator) {
        var devices = iterateDevices(iterator, false);
        if (onDeviceConnectedHandler == null)
            return;

        for (var device : devices)
            onDeviceConnectedHandler.accept(device);
    }

    private void onDeviceDisconnected(MemoryAddress ignoredRefCon, int iterator) {
        var devices = iterateDevices(iterator, true);
        if (onDeviceDisconnectedHandler == null)
            return;

        for (var device : devices)
            onDeviceDisconnectedHandler.accept(device);
    }

    private USBDeviceInfo createDeviceInfo(long entryID, int service) {
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

        return new MacosUSBDeviceInfo(entryID, vendorId, productId, manufacturer, product, serial, classCode, subclassCode, protocolCode);
    }

    @Override
    public void setOnDeviceConnected(Consumer<USBDeviceInfo> handler) {
        if (handler == null) {
            onDeviceConnectedHandler = null;
            return;
        }

        onDeviceConnectedHandler = handler;
        startDeviceMonitor();
    }

    @Override
    public void setOnDeviceDisconnected(Consumer<USBDeviceInfo> handler) {
        if (handler == null) {
            onDeviceDisconnectedHandler = null;
            return;
        }

        onDeviceDisconnectedHandler = handler;
        startDeviceMonitor();
    }
}
