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
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static net.codecrete.usb.common.ForeignMemory.dereference;

/**
 * Constants and helper functions for the IOKit framework.
 */
class IoKitHelper {

    private IoKitHelper() {
    }

    static final MemorySegment kIOUSBDeviceUserClientTypeID = UUID.createCFUUID(new byte[]{(byte) 0x9d,
            (byte) 0xc7, (byte) 0xb7, (byte) 0x80, (byte) 0x9e, (byte) 0xc0, (byte) 0x11, (byte) 0xD4, (byte) 0xa5,
            (byte) 0x4f, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61});
    static final MemorySegment kIOUSBInterfaceUserClientTypeID = UUID.createCFUUID(new byte[]{(byte) 0x2d,
            (byte) 0x97, (byte) 0x86, (byte) 0xc6, (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4, (byte) 0xad,
            (byte) 0x51, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61});
    static final MemorySegment kIOUSBDeviceInterfaceID187 = UUID.createCFUUID(new byte[]{(byte) 0x3c,
            (byte) 0x9e, (byte) 0xe1, (byte) 0xeb, (byte) 0x24, (byte) 0x02, (byte) 0x11, (byte) 0xb2, (byte) 0x8e,
            (byte) 0x7e, (byte) 0x00, (byte) 0x0a, (byte) 0x27, (byte) 0x80, (byte) 0x1e, (byte) 0x86});
    static final MemorySegment kIOUSBInterfaceInterfaceID190 = UUID.createCFUUID(new byte[]{(byte) 0x8f,
            (byte) 0xdb, (byte) 0x84, (byte) 0x55, (byte) 0x74, (byte) 0xa6, (byte) 0x11, (byte) 0xD6, (byte) 0x97,
            (byte) 0xb1, (byte) 0x00, (byte) 0x30, (byte) 0x65, (byte) 0xd3, (byte) 0x60, (byte) 0x8e});
    static final MemorySegment kIOCFPlugInInterfaceID = UUID.createCFUUID(new byte[]{(byte) 0xC2, (byte) 0x44,
            (byte) 0xE8, (byte) 0x58, (byte) 0x10, (byte) 0x9C, (byte) 0x11, (byte) 0xD4, (byte) 0x91, (byte) 0xD4,
            (byte) 0x00, (byte) 0x50, (byte) 0xE4, (byte) 0xC6, (byte) 0x42, (byte) 0x6F});

    /**
     * Layout of COM object.
     * <p>
     * Parts of I/O Kit use a plug-in architecture following the Component Object Model (COM).
     * This layout is the basis for calling object methods (through the vtable) and
     * accessing the reference count (for debugging purposes).
     * </p>
     */
    static final StructLayout COM_OBJECT =
            MemoryLayout.structLayout(
                    ADDRESS.withTargetLayout(
                            MemoryLayout.structLayout(
                                // up to 100 function pointers
                                MemoryLayout.sequenceLayout(100, ADDRESS)
                            )
                    ).withName("vtable"),
                    ADDRESS.withTargetLayout(
                            MemoryLayout.structLayout(
                                    ADDRESS.withName("unknown"),
                                    JAVA_INT.withName("refCount")
                            )
                    ).withName("data")
            );

    /**
     * Var handle for accessing the <i>vtable</i>.
     * <p>
     * The <i>vtable</i> is an array of function pointers.
     * </p>
     */
    static final VarHandle vtable$VH = COM_OBJECT.varHandle(PathElement.groupElement("vtable"));

    /**
     * Var handle for accessing the reference count.
     */
    static final VarHandle refCount$VH = COM_OBJECT.varHandle(
            PathElement.groupElement("data"),
            PathElement.dereferenceElement(),
            PathElement.groupElement("refCount")
    );

    /**
     * Get the <i>vtable</i> of the specified object instance.
     * @param self object instance
     * @return <i>vtable</i>
     */
    static MemorySegment getVtable(MemorySegment self) {
        return (MemorySegment) vtable$VH.get(self);
    }

    /**
     * Gets an object instance implementing the specified service.
     * <p>
     * This method first requests the specified plugin interface and then
     * queries for the specified interface.
     * </p>
     *
     * @param service     the service
     * @param pluginType  the plugin interface type
     * @param interfaceId the interface ID
     * @return object instance implementing the interface, or {@code null} if the plugin type or interface is not available
     */
    static MemorySegment getInterface(int service, MemorySegment pluginType, MemorySegment interfaceId) {
        try (var arena = Arena.ofConfined()) {
            // MemorySegment for holding IOCFPlugInInterface**
            var plugHolder = arena.allocate(ADDRESS, NULL);
            // MemorySegment for holding score
            var score = arena.allocate(JAVA_INT, 0);
            var ret = IOKit.IOCreatePlugInInterfaceForService(service, pluginType, kIOCFPlugInInterfaceID, plugHolder
                    , score);
            if (ret != 0)
                return null;
            var plug = dereference(plugHolder, COM_OBJECT);

            // UUID bytes
            var refiid = CoreFoundation.CFUUIDGetUUIDBytes(arena, interfaceId);
            // MemorySegment for holding xxxInterface**
            var intfHolder = arena.allocate(ADDRESS, NULL);
            ret = IoKitUSB.QueryInterface(plug, refiid, intfHolder);
            IoKitUSB.Release(plug);
            if (ret != 0)
                return null;
            return dereference(intfHolder, COM_OBJECT);
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
    static Integer getPropertyInt(int service, MemorySegment key, Arena arena) {

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
    static String getPropertyString(int service, MemorySegment key, Arena arena) {

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
    static int getRefCount(MemorySegment self) {
        return (int) refCount$VH.get(self);
    }
}
