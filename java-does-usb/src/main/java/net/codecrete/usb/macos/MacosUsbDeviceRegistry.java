//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.UsbDevice;
import net.codecrete.usb.common.ScopeCleanup;
import net.codecrete.usb.common.UsbDeviceRegistry;
import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;
import net.codecrete.usb.macos.gen.iokit.IOKit;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static net.codecrete.usb.macos.CoreFoundationHelper.createCFStringRef;
import static net.codecrete.usb.macos.MacosUsbException.throwException;

/**
 * MacOS implementation of USB device registry.
 */
@SuppressWarnings("java:S116")
public class MacosUsbDeviceRegistry extends UsbDeviceRegistry {

    private static final System.Logger LOG = System.getLogger(MacosUsbDeviceRegistry.class.getName());

    private static final MemorySegment KEY_ID_VENDOR;
    private static final MemorySegment KEY_ID_PRODUCT;
    private static final MemorySegment KEY_VENDOR;
    private static final MemorySegment KEY_PRODUCT;
    private static final MemorySegment KEY_SERIAL_NUM;
    private static final MemorySegment KEY_DEVICE_CLASS;
    private static final MemorySegment KEY_DEVICE_SUBCLASS;
    private static final MemorySegment KEY_DEVICE_PROTOCOL;
    private static final MemorySegment KEY_USB_BCD;
    private static final MemorySegment KEY_DEVICE_BCD;

    static {
        SegmentAllocator global = Arena.global();
        KEY_ID_VENDOR = createCFStringRef("idVendor", global);
        KEY_ID_PRODUCT = createCFStringRef("idProduct", global);
        KEY_VENDOR = createCFStringRef("kUSBVendorString", global);
        KEY_PRODUCT = createCFStringRef("kUSBProductString", global);
        KEY_SERIAL_NUM = createCFStringRef("kUSBSerialNumberString", global);
        KEY_DEVICE_CLASS = createCFStringRef("bDeviceClass", global);
        KEY_DEVICE_SUBCLASS = createCFStringRef("bDeviceSubClass", global);
        KEY_DEVICE_PROTOCOL = createCFStringRef("bDeviceProtocol", global);
        KEY_USB_BCD = createCFStringRef("bcdUSB", global);
        KEY_DEVICE_BCD = createCFStringRef("bcdDevice", global);
    }

    /**
     * Monitors the USB devices.
     * <p>
     * This method is the core of the background thread. It runs forever and does not terminate.
     * </p>
     */
    @Override
    protected void monitorDevices() {

        // as the method runs forever, there is no need to clean up one-time allocations
        try (var arena = Arena.ofConfined()) {

            try {

                // setup run loop, run loop source and notification port
                var notifyPort = IOKit.IONotificationPortCreate(IOKit.kIOMasterPortDefault$get());
                var runLoopSource = IOKit.IONotificationPortGetRunLoopSource(notifyPort);
                var runLoop = CoreFoundation.CFRunLoopGetCurrent();
                CoreFoundation.CFRunLoopAddSource(runLoop, runLoopSource, IOKit.kCFRunLoopDefaultMode$get());

                // setup notification for connected devices
                var onDeviceConnectedMH = MethodHandles.lookup().findVirtual(MacosUsbDeviceRegistry.class,
                        "onDevicesConnected", MethodType.methodType(void.class, MemorySegment.class, int.class));
                var deviceConnectedIter = setupNotification(arena, notifyPort, IOKit.kIOFirstMatchNotification(),
                        onDeviceConnectedMH);

                // iterate current devices in order to arm the notifications (and build initial device list)
                var deviceList = new ArrayList<UsbDevice>();
                iterateDevices(deviceConnectedIter, device -> deviceList.add(device)); // NOSONAR
                setInitialDeviceList(deviceList);

                // setup notification for disconnected devices
                var onDeviceDisconnectedMH = MethodHandles.lookup().findVirtual(MacosUsbDeviceRegistry.class,
                        "onDevicesDisconnected", MethodType.methodType(void.class, MemorySegment.class, int.class));
                var deviceDisconnectedIter = setupNotification(arena, notifyPort, IOKit.kIOTerminatedNotification(),
                        onDeviceDisconnectedMH);

                // iterate current devices in order to arm the notifications
                onDevicesDisconnected(NULL, deviceDisconnectedIter);

            } catch (Exception e) {
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

        try (var arena = Arena.ofConfined()) {
            var entryIdHolder = arena.allocate(JAVA_LONG);

            int svc;
            while ((svc = IOKit.IOIteratorNext(iterator)) != 0) {
                try (var cleanup = new ScopeCleanup()) {

                    final var service = svc;
                    cleanup.add(() -> IOKit.IOObjectRelease(service));

                    var device = IoKitHelper.getInterface(service, IoKitHelper.kIOUSBDeviceUserClientTypeID,
                            IoKitHelper.kIOUSBDeviceInterfaceID187);
                    if (device != null)
                        cleanup.add(() -> IoKitUsb.Release(device));

                    // get entry ID (as unique ID)
                    var ret = IOKit.IORegistryEntryGetRegistryEntryID(service, entryIdHolder);
                    if (ret != 0)
                        throwException(ret, "internal error (IORegistryEntryGetRegistryEntryID)");
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
     * This method tries to create a {@link UsbDevice} instance.
     * If it fails, an information is printed, but the consumer is not called.
     * </p>
     *
     * @param iterator the iterator
     * @param consumer the consumer
     */
    @SuppressWarnings("java:S106")
    private void iterateDevices(int iterator, Consumer<UsbDevice> consumer) {
        iterateDevices(iterator, (entryId, service, deviceIntf) -> {

            var deviceInfo = new VidPid();
            try {
                var device = createDevice(entryId, service, deviceIntf, deviceInfo);
                if (device != null)
                    consumer.accept(device);

            } catch (Exception e) {
                LOG.log(INFO, String.format("failed to retrieve information about device 0x%04x/0x%04x - ignoring device",
                        deviceInfo.vid, deviceInfo.pid), e);
            }
        });
    }

    private UsbDevice createDevice(Long entryID, int service, MemorySegment deviceIntf, VidPid info) {

        if (deviceIntf == null)
            return null;

        try (var arena = Arena.ofConfined()) {

            var vendorId = IoKitHelper.getPropertyInt(service, KEY_ID_VENDOR, arena);
            var productId = IoKitHelper.getPropertyInt(service, KEY_ID_PRODUCT, arena);
            if (vendorId == null || productId == null)
                return null;

            info.vid = vendorId;
            info.pid = productId;

            var device = new MacosUsbDevice(deviceIntf, entryID, vendorId, productId);

            var manufacturer = IoKitHelper.getPropertyString(service, KEY_VENDOR, arena);
            var product = IoKitHelper.getPropertyString(service, KEY_PRODUCT, arena);
            var serial = IoKitHelper.getPropertyString(service, KEY_SERIAL_NUM, arena);

            device.setProductStrings(manufacturer, product, serial);

            var classCode = IoKitHelper.getPropertyInt(service, KEY_DEVICE_CLASS, arena);
            var subclassCode = IoKitHelper.getPropertyInt(service, KEY_DEVICE_SUBCLASS, arena);
            var protocolCode = IoKitHelper.getPropertyInt(service, KEY_DEVICE_PROTOCOL, arena);

            device.setClassCodes(classCode != null ? classCode : 0, subclassCode != null ? subclassCode : 0,
                    protocolCode != null ? protocolCode : 0);

            var usbVersion = IoKitHelper.getPropertyInt(service, KEY_USB_BCD, arena);
            var deviceVersion = IoKitHelper.getPropertyInt(service, KEY_DEVICE_BCD, arena);
            //noinspection DataFlowIssue
            device.setVersions(usbVersion, deviceVersion != null ? deviceVersion : 0);

            return device;
        }
    }

    private int setupNotification(Arena arena, MemorySegment notifyPort, MemorySegment notificationType,
                                  MethodHandle callback) {

        // new matching dictionary for (dis)connected device notifications (NOSONAR)
        var matchingDict = IOKit.IOServiceMatching(IOKit.kIOUSBDeviceClassName());

        // create callback stub
        var onDeviceCallbackStub = Linker.nativeLinker().upcallStub(callback.bindTo(this),
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT), Arena.global());

        // Set up a notification to be called when a device is first matched / terminated by I/O Kit.
        // This method consumes the matchingDict reference.
        var deviceIterHolder = arena.allocate(JAVA_INT);
        var ret = IOKit.IOServiceAddMatchingNotification(notifyPort, notificationType, matchingDict,
                onDeviceCallbackStub, NULL, deviceIterHolder);
        if (ret != 0)
            throwException(ret, "internal error (IOServiceAddMatchingNotification)");

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
    @SuppressWarnings("java:S1172")
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
    @SuppressWarnings({"SameParameterValue", "java:S1172", "java:S106"})
    private void onDevicesDisconnected(MemorySegment ignoredRefCon, int iterator) {

        // process device iterator for disconnected devices
        iterateDevices(iterator, (entryId, service, deviceIntf) -> {
            var device = findDevice(entryId);
            if (device == null)
                return;

            try {
                ((MacosUsbDevice) device).closeFully();
            } catch (Exception e) {
                LOG.log(INFO, "failed to close USB device - ignoring exception", e);
            }

            removeDevice(entryId);
        });
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
