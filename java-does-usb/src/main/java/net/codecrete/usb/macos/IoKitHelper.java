//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;
import net.codecrete.usb.macos.gen.iokit.IOKit;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Constants and helper functions for the IOKit framework.
 */
public class IoKitHelper {
    public static final MemorySegment kIOUSBDeviceUserClientTypeID = UUID.CreateCFUUID(
            new byte[]{(byte) 0x9d, (byte) 0xc7, (byte) 0xb7, (byte) 0x80, (byte) 0x9e, (byte) 0xc0, (byte) 0x11,
                    (byte) 0xD4, (byte) 0xa5, (byte) 0x4f, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x05,
                    (byte) 0x28, (byte) 0x61});
    public static final MemorySegment kIOUSBInterfaceUserClientTypeID = UUID.CreateCFUUID(
            new byte[]{(byte) 0x2d, (byte) 0x97, (byte) 0x86, (byte) 0xc6, (byte) 0x9e, (byte) 0xf3, (byte) 0x11,
                    (byte) 0xD4, (byte) 0xad, (byte) 0x51, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x05,
                    (byte) 0x28, (byte) 0x61});
    public static final MemorySegment kIOUSBDeviceInterfaceID100 = UUID.CreateCFUUID(
            new byte[]{(byte) 0x5c, (byte) 0x81, (byte) 0x87, (byte) 0xd0, (byte) 0x9e, (byte) 0xf3, (byte) 0x11,
                    (byte) 0xD4, (byte) 0x8b, (byte) 0x45, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x05,
                    (byte) 0x28, (byte) 0x61});
    public static final MemorySegment kIOUSBInterfaceInterfaceID100 = UUID.CreateCFUUID(
            new byte[]{(byte) 0x73, (byte) 0xc9, (byte) 0x7a, (byte) 0xe8, (byte) 0x9e, (byte) 0xf3, (byte) 0x11,
                    (byte) 0xD4, (byte) 0xb1, (byte) 0xd0, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x05,
                    (byte) 0x28, (byte) 0x61});
    public static final MemorySegment kIOCFPlugInInterfaceID = UUID.CreateCFUUID(
            new byte[]{(byte) 0xC2, (byte) 0x44, (byte) 0xE8, (byte) 0x58, (byte) 0x10, (byte) 0x9C, (byte) 0x11,
                    (byte) 0xD4, (byte) 0x91, (byte) 0xD4, (byte) 0x00, (byte) 0x50, (byte) 0xE4, (byte) 0xC6,
                    (byte) 0x42, (byte) 0x6F});

    /**
     * Get an interface of the specified service.
     * <p>
     * This method first request the specified plugin interfaces and then
     * queries for the specified interface.
     * </p>
     *
     * @param service     the service
     * @param pluginType  the plugin interface type
     * @param interfaceId the interface ID
     * @return the interface, or <code>null</code> if the plugin type or interface is not available
     */
    public static MemorySegment getInterface(int service, MemorySegment pluginType, MemorySegment interfaceId) {
        try (var arena = Arena.openConfined()) {
            // MemorySegment for holding IOCFPlugInInterface**
            var plugHolder = arena.allocate(ADDRESS, NULL);
            // MemorySegment for holding score
            var score = arena.allocate(JAVA_INT, 0);
            int ret = IOKit.IOCreatePlugInInterfaceForService(service, pluginType, kIOCFPlugInInterfaceID,
                    plugHolder, score);
            if (ret != 0)
                return null;
            var plug = plugHolder.get(ADDRESS, 0);

            // UUID bytes
            var refiid = CoreFoundation.CFUUIDGetUUIDBytes(arena, interfaceId);
            // MemorySegment for holding xxxInterface**
            var intfHolder = arena.allocate(ADDRESS, NULL);
            ret = IoKitUSB.QueryInterface(plug, refiid, intfHolder);
            IoKitUSB.Release(plug);
            if (ret != 0)
                return null;
            return intfHolder.get(ADDRESS, 0);
        }
    }

    /**
     * Gets a property of the specified IO registry service.
     * <p>
     * The property must be of numeric type.
     * </p>
     *
     * @param service the service
     * @param key     the property key
     * @param arena   the arena for allocating memory
     * @return the property value, or {@code null} if the service doesn't have the property
     */
    public static Integer getPropertyInt(int service, MemorySegment key, Arena arena) {

        var value = IOKit.IORegistryEntryCreateCFProperty(service, key, NULL, 0);
        if (value.address() == 0)
            return null;

        Integer result = null;
        var type = CoreFoundation.CFGetTypeID(value);
        if (type == CoreFoundation.CFNumberGetTypeID()) {
            var numberValue = arena.allocate(JAVA_INT, 0);
            if (CoreFoundation.CFNumberGetValue(value, CoreFoundation.kCFNumberSInt32Type(), numberValue) != 0)
                result = numberValue.get(JAVA_INT, 0);
        }

        CoreFoundation.CFRelease(value);

        return result;
    }

    /**
     * Gets a property of the specified IO registry service.
     * <p>
     * The property must be of string type.
     * </p>
     *
     * @param service the service
     * @param key     the property key
     * @param arena   the arena for allocating memory
     * @return the property value, or {@code null} if the service doesn't have the property
     */
    public static String getPropertyString(int service, MemorySegment key, Arena arena) {

        var value = IOKit.IORegistryEntryCreateCFProperty(service, key, NULL, 0);
        if (value.address() == 0)
            return null;

        String result = null;
        var type = CoreFoundation.CFGetTypeID(value);
        if (type == CoreFoundation.CFStringGetTypeID())
            result = CoreFoundationHelper.stringFromCFStringRef(value, arena);

        CoreFoundation.CFRelease(value);

        return result;
    }

    // debugging aid
    public static int getRefCount(MemorySegment self) {
        var object = MemorySegment.ofAddress(self.address(), 16, SegmentScope.global());
        var dataAddr = object.get(ADDRESS, ADDRESS.byteSize());
        var data = MemorySegment.ofAddress(dataAddr.address(), 12, SegmentScope.global());
        return data.get(JAVA_INT, 8);
    }
}
