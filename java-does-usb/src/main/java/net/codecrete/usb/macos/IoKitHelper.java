//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.macos.CoreFoundation.CFUUIDBytes;
import static net.codecrete.usb.macos.CoreFoundation.CFUUID_bytes$Offset;

/**
 * Helper functions for the IOKit framework.
 */
public class IoKitHelper {
    private static final long NUMBER_TYPE_ID = CoreFoundation.CFNumberGetTypeID();
    private static final long STRING_TYPE_ID = CoreFoundation.CFStringGetTypeID();

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
    public static MemoryAddress getInterface(int service, Addressable pluginType, MemoryAddress interfaceId) {
        try (var session = MemorySession.openConfined()) {
            // MemorySegment for holding IOCFPlugInInterface**
            var plugHolder = session.allocate(ADDRESS, NULL);
            // MemorySegment for holding score
            var score = session.allocate(JAVA_INT, 0);
            int ret = IoKit.IOCreatePlugInInterfaceForService(service, pluginType, IoKit.kIOCFPlugInInterfaceID, plugHolder, score);
            if (ret != 0)
                return null;
            var plug = (MemoryAddress) plugHolder.get(ADDRESS, 0);

            // MemorySegment for holding xxxInterface**
            var intfHolder = session.allocate(ADDRESS, NULL);
            // UUID bytes
            var refiid = MemorySegment.ofAddress(interfaceId.addOffset(CFUUID_bytes$Offset), CFUUIDBytes.byteSize(), session);
            ret = IoKit.QueryInterface(plug, refiid, intfHolder);
            IoKit.Release(plug);
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
     * @param service the service
     * @param key the property key
     * @return the property value, or {@code null} if the service doesn't have the property
     */
    public static Integer getPropertyInt(int service, String key) {
        var value = IoKit.IORegistryEntryCreateCFProperty(service, key, NULL, 0);
        if (value == NULL)
            return null;

        Integer result = null;
        var type = CoreFoundation.CFGetTypeID(value);
        if (type == NUMBER_TYPE_ID) {

            try (var session = MemorySession.openConfined()) {
                var numberValue = session.allocate(JAVA_INT, 0);
                if (CoreFoundation.CFNumberGetValue(value, CoreFoundation.kCFNumberSInt32Type, numberValue))
                    result = numberValue.get(JAVA_INT, 0);
            }
        }

        CoreFoundation.CFRelease(value);
        return result;
    }

    /**
     * Gets a property of the specified IO registry service.
     * <p>
     * The property must be of string type.
     * </p>
     * @param service the service
     * @param key the property key
     * @return the property value, or {@code null} if the service doesn't have the property
     */
    public static String getPropertyString(int service, String key) {
        var value = IoKit.IORegistryEntryCreateCFProperty(service, key, NULL, 0);
        if (value == NULL)
            return null;

        String result = null;
        var type = CoreFoundation.CFGetTypeID(value);
        if (type == STRING_TYPE_ID)
            result = CoreFoundationHelper.stringFromCFStringRef(value);

        CoreFoundation.CFRelease(value);
        return result;
    }

    public static int getRefCount(MemoryAddress object) {
        try (var session = MemorySession.openConfined()) {
            var objSegment = MemorySegment.ofAddress(object, 16, session);
            var dataAddr = objSegment.get(ADDRESS, 8);
            var dataSegment = MemorySegment.ofAddress(dataAddr, 12, session);
            return dataSegment.get(JAVA_INT, 8);
        }
    }
}
