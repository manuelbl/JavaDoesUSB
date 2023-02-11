//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.USBDeviceRegistry;
import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;
import net.codecrete.usb.macos.gen.iokit.IOKit;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.macos.MacosUSBException.throwException;

/**
 * MacOS implementation of USB device registry.
 */
public class MacosUSBDeviceRegistry extends USBDeviceRegistry {

    private final SegmentAllocator GLOBAL_ALLOCATOR = SegmentAllocator.nativeAllocator(SegmentScope.global());

    private final MemorySegment KEY_ID_VENDOR = CoreFoundationHelper.createCFStringRef("idVendor", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_ID_PRODUCT = CoreFoundationHelper.createCFStringRef("idProduct", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_VENDOR = CoreFoundationHelper.createCFStringRef("kUSBVendorString", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_PRODUCT = CoreFoundationHelper.createCFStringRef("kUSBProductString", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_SERIAL_NUM = CoreFoundationHelper.createCFStringRef("kUSBSerialNumberString", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_DEVICE_CLASS = CoreFoundationHelper.createCFStringRef("bDeviceClass", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_DEVICE_SUBCLASS = CoreFoundationHelper.createCFStringRef("bDeviceSubClass", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_DEVICE_PROTOCOL = CoreFoundationHelper.createCFStringRef("bDeviceProtocol", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_USB_BCD = CoreFoundationHelper.createCFStringRef("bcdUSB", GLOBAL_ALLOCATOR);
    private final MemorySegment KEY_DEVICE_BCD = CoreFoundationHelper.createCFStringRef("bcdDevice", GLOBAL_ALLOCATOR);


    /**
     * Monitors the USB devices.
     * <p>
     * This method is the core of the background thread. It runs forever and does not terminate.
     * </p>
     */
    @Override
    protected void monitorDevices() {

        // as the method runs forever, there is no need to clean up one-time allocations
        try (var arena = Arena.openConfined()) {

            try {

                // setup run loop, run loop source and notification port
                var notifyPort = IOKit.IONotificationPortCreate(IOKit.kIOMasterPortDefault$get());
                var runLoopSource = IOKit.IONotificationPortGetRunLoopSource(notifyPort);
                var runLoop = CoreFoundation.CFRunLoopGetCurrent();
                CoreFoundation.CFRunLoopAddSource(runLoop, runLoopSource, IOKit.kCFRunLoopDefaultMode$get());

                // setup notification for connected devices
                var onDeviceConnectedMH = MethodHandles.lookup().findVirtual(MacosUSBDeviceRegistry.class,
                        "onDevicesConnected", MethodType.methodType(void.class, MemorySegment.class, int.class));
                int deviceConnectedIter = setupNotification(arena, notifyPort, IOKit.kIOFirstMatchNotification(),
                        onDeviceConnectedMH);

                // iterate current devices in order to arm the notifications (and build initial device list)
                var deviceList = new ArrayList<USBDevice>();
                iterateDevices(deviceConnectedIter, (device) -> deviceList.add(device));
                setInitialDeviceList(deviceList);

                // setup notification for disconnected devices
                var onDeviceDisconnectedMH = MethodHandles.lookup().findVirtual(MacosUSBDeviceRegistry.class,
                        "onDevicesDisconnected", MethodType.methodType(void.class, MemorySegment.class, int.class));
                int deviceDisconnectedIter = setupNotification(arena, notifyPort, IOKit.kIOTerminatedNotification(),
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
     * @param iterator the iterator
     * @param consumer a consumer that will be called for each device with the entry ID and the service
     */
    private void iterateDevices(int iterator, IOKitDeviceConsumer consumer) {

        try (var arena = Arena.openConfined()) {
            var entryIdHolder = arena.allocate(JAVA_LONG);

            int svc;
            while ((svc = IOKit.IOIteratorNext(iterator)) != 0) {
                try (var cleanup = new ScopeCleanup()) {

                    final int service = svc;
                    cleanup.add(() -> IOKit.IOObjectRelease(service));

                    var device = IoKitHelper.getInterface(service, IoKitHelper.kIOUSBDeviceUserClientTypeID, IoKitHelper.kIOUSBDeviceInterfaceID100);
                    if (device != null)
                        cleanup.add(() -> IoKitUSB.Release(device));

                    // get entry ID (as unique ID)
                    int ret = IOKit.IORegistryEntryGetRegistryEntryID(service, entryIdHolder);
                    if (ret != 0)
                        throwException(ret, "IORegistryEntryGetRegistryEntryID failed");
                    var entryId = entryIdHolder.get(JAVA_LONG, 0);

                    // call consumer to process device
                    consumer.accept(entryId, service, device);
                }
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
                System.err.printf("Info: [JavaDoesUSB] failed to retrieve information about device 0x%04x/0x%04x - " + "ignoring device%n", deviceInfo.vid, deviceInfo.pid);
                e.printStackTrace(System.err);
            }
        });
    }

    private USBDevice createDevice(Long entryID, int service, MemorySegment deviceIntf, VidPid info) {

        if (deviceIntf == null)
            return null;

        try (var arena = Arena.openConfined()) {

            Integer vendorId = IoKitHelper.getPropertyInt(service, KEY_ID_VENDOR, arena);
            Integer productId = IoKitHelper.getPropertyInt(service, KEY_ID_PRODUCT, arena);
            if (vendorId == null || productId == null)
                return null;

            info.vid = vendorId;
            info.pid = productId;

            var device = new MacosUSBDevice(this, deviceIntf, entryID, vendorId, productId);

            String manufacturer = IoKitHelper.getPropertyString(service, KEY_VENDOR, arena);
            String product = IoKitHelper.getPropertyString(service, KEY_PRODUCT, arena);
            String serial = IoKitHelper.getPropertyString(service, KEY_SERIAL_NUM, arena);

            device.setProductStrings(manufacturer, product, serial);

            Integer classCode = IoKitHelper.getPropertyInt(service, KEY_DEVICE_CLASS, arena);
            Integer subclassCode = IoKitHelper.getPropertyInt(service, KEY_DEVICE_SUBCLASS, arena);
            Integer protocolCode = IoKitHelper.getPropertyInt(service, KEY_DEVICE_PROTOCOL, arena);

            device.setClassCodes(classCode != null ? classCode : 0, subclassCode != null ? subclassCode : 0, protocolCode != null ? protocolCode : 0);

            Integer usbVersion = IoKitHelper.getPropertyInt(service, KEY_USB_BCD, arena);
            Integer deviceVersion = IoKitHelper.getPropertyInt(service, KEY_DEVICE_BCD, arena);
            //noinspection DataFlowIssue
            device.setVersions(usbVersion, deviceVersion != null ? deviceVersion : 0);

            return device;
        }
    }

    private int setupNotification(Arena arena, MemorySegment notifyPort, MemorySegment notificationType,
                                  MethodHandle callback) {

        // new matching dictionary for (dis)connected device notifications
        var matchingDict = IOKit.IOServiceMatching(IOKit.kIOUSBDeviceClassName());

        // create callback stub
        var onDeviceCallbackStub = Linker.nativeLinker().upcallStub(callback.bindTo(this),
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT), SegmentScope.global());

        // Set up a notification to be called when a device is first matched / terminated by I/O Kit.
        // This method consumes the matchingDict reference.
        var deviceIterHolder = arena.allocate(JAVA_INT);
        int ret = IOKit.IOServiceAddMatchingNotification(notifyPort, notificationType, matchingDict,
                onDeviceCallbackStub, NULL, deviceIterHolder);
        if (ret != 0)
            throwException(ret, "IOServiceAddMatchingNotification failed");

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
    private void onDevicesConnected(MemorySegment ignoredRefCon, int iterator) {

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
    private void onDevicesDisconnected(@SuppressWarnings("SameParameterValue") MemorySegment ignoredRefCon, int iterator) {

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

    private final ReentrantLock asyncIoLock = new ReentrantLock();
    private final Condition asyncIoReady = asyncIoLock.newCondition();
    private MemorySegment asyncIoRunLoop;

    void addEventSource(MemorySegment source) {
        try {
            asyncIoLock.lock();

            if (asyncIoRunLoop == null) {
                // start background thread
                Thread t = new Thread(() -> asyncIOCompletionTask(source), "USB async IO");
                t.setDaemon(true);
                t.start();

                while (asyncIoRunLoop == null)
                    asyncIoReady.awaitUninterruptibly();

                return;
            }

            CoreFoundation.CFRunLoopAddSource(asyncIoRunLoop, source, IOKit.kCFRunLoopDefaultMode$get());

        } finally {
            asyncIoLock.unlock();
        }
    }

    void removeEventSource(MemorySegment source) {
        CoreFoundation.CFRunLoopRemoveSource(asyncIoRunLoop, source, IOKit.kCFRunLoopDefaultMode$get());
    }

    private void asyncIOCompletionTask(MemorySegment firstSource) {
        try {
            asyncIoLock.lock();
            asyncIoRunLoop = CoreFoundation.CFRunLoopGetCurrent();
            CoreFoundation.CFRunLoopAddSource(asyncIoRunLoop, firstSource, IOKit.kCFRunLoopDefaultMode$get());
            asyncIoReady.signalAll();
        } finally {
            asyncIoLock.unlock();
        }

        // loop forever
        CoreFoundation.CFRunLoopRun();
    }

    @FunctionalInterface
    interface IOKitDeviceConsumer {
        void accept(long entryId, int service, MemorySegment deviceIntf);
    }

    static class VidPid {
        int vid;
        int pid;
    }
}
