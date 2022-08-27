//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.macos.CoreFoundation.CFUUIDBytes;
import static net.codecrete.usb.macos.CoreFoundationHelper.createCFStringRef;

/**
 * Constants and functions for the IOKit framework.
 * <p>
 * In the future, jextract will hopefully be able to generate code for
 * macOS frameworks in order to generate most of this.
 * </p>
 */
public class IoKit {

    private static final Linker linker = Linker.nativeLinker();
    private static final MemorySession ioKitSession = MemorySession.openShared();
    private static final SymbolLookup ioKitLookup = SymbolLookup.libraryLookup("IOKit.framework/IOKit", ioKitSession);

    public static final Addressable kIOUSBPlane = ioKitSession.allocateUtf8String("IOUSB");
    public static final Addressable kIOServicePlane = ioKitSession.allocateUtf8String("IOService");

    public static final int kIOMasterPortDefault;
    public static final int kIORegistryIterateRecursively = 1;

    public static final MemorySegment kIOUSBDeviceClassName = ioKitSession.allocateUtf8String("IOUSBDevice");
    public static final MemorySegment kIOFirstMatchNotification = ioKitSession.allocateUtf8String("IOServiceFirstMatch");
    public static final MemorySegment kIOTerminatedNotification = ioKitSession.allocateUtf8String("IOServiceTerminate");

    public static final MemoryAddress kCFRunLoopDefaultMode = createCFStringRef("kCFRunLoopDefaultMode");

    public static final GroupLayout IOCFPlugInInterface$Struct = MemoryLayout.structLayout(
            ADDRESS.withName("_reserved"), // void *_reserved;
            ADDRESS.withName("QueryInterface"), // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv);
            ADDRESS.withName("AddRef"), // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer);
            ADDRESS.withName("Release"), // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer);
            JAVA_SHORT.withName("version"), // UInt16 version;
            JAVA_SHORT.withName("revision"), // UInt16 revision;
            ADDRESS.withName("Probe"), // IOReturn (*Probe)(void *thisPointer, CFDictionaryRef propertyTable, io_service_t service, SInt32 * order);
            ADDRESS.withName("Start"), // IOReturn (*Start)(void *thisPointer, CFDictionaryRef propertyTable, io_service_t service);
            ADDRESS.withName("Stop") // IOReturn (*Stop)(void *thisPointer);
    );

    /**
     * Looks up the address of the specified virtual function in the {@code IOCFPlugInInterface} object
     * @param thisPointer pointer to pointer to object
     * @param varHandle variable handle to virtual function
     * @param session memory session
     * @return function address
     */
    private static Addressable IOCFPlugInInterfaceFunctionAddress(MemoryAddress thisPointer, VarHandle varHandle, MemorySession session) {
        var seg = MemorySegment.ofAddress(thisPointer, ADDRESS.byteSize(), session);
        var thisValue = seg.get(ADDRESS, 0);
        var obj = MemorySegment.ofAddress(thisValue, IOCFPlugInInterface$Struct.byteSize(), session);
        return (MemoryAddress) varHandle.get(obj);
    }

    // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv);
    private static final VarHandle IUnknown_QueryInterface = IOCFPlugInInterface$Struct.varHandle(groupElement("QueryInterface"));
    private static final MethodHandle QueryInterface$Func = linker.downcallHandle(
            FunctionDescriptor.of(JAVA_INT, ADDRESS, CFUUIDBytes, ADDRESS)
    );
    public static int QueryInterface(MemoryAddress thisPointer, MemorySegment iid, Addressable ppv) {
        try (var session = MemorySession.openShared()) {
            var funcPtr = IOCFPlugInInterfaceFunctionAddress(thisPointer, IUnknown_QueryInterface, session);
            return (int) QueryInterface$Func.invokeExact(funcPtr, (Addressable) thisPointer, iid, ppv);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer);
    private static final VarHandle IUnknown_AddRef = IOCFPlugInInterface$Struct.varHandle(groupElement("AddRef"));
    private static final MethodHandle AddRef$Func = linker.downcallHandle(
            FunctionDescriptor.of(JAVA_INT, ADDRESS)
    );
    public static int AddRef(MemoryAddress thisPointer) {
        try (var session = MemorySession.openShared()) {
            var funcPtr = IOCFPlugInInterfaceFunctionAddress(thisPointer, IUnknown_AddRef, session);
            return (int) AddRef$Func.invokeExact(funcPtr, (Addressable) thisPointer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer);
    private static final VarHandle IUnknown_Release = IOCFPlugInInterface$Struct.varHandle(groupElement("Release"));
    private static final MethodHandle Release$Func = linker.downcallHandle(
            FunctionDescriptor.of(JAVA_INT, ADDRESS)
    );
    public static int Release(MemoryAddress thisPointer) {
        try (var session = MemorySession.openShared()) {
            var funcPtr = IOCFPlugInInterfaceFunctionAddress(thisPointer, IUnknown_Release, session);
            return (int) Release$Func.invokeExact(funcPtr, (Addressable) thisPointer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // io_registry_entry_t IORegistryGetRootEntry(mach_port_t mainPort);
    private static final MethodHandle IORegistryGetRootEntry$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryGetRootEntry").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT)
    );
    public static int IORegistryGetRootEntry(int mainPort) {
        try {
            return (int) IORegistryGetRootEntry$Func.invokeExact(mainPort);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IOObjectRelease(io_object_t object);
    private static final MethodHandle IOObjectRelease$Func = linker.downcallHandle(
            ioKitLookup.lookup("IOObjectRelease").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT)
    );
    public static int IOObjectRelease(int object) {
        try {
            return (int) IOObjectRelease$Func.invokeExact(object);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IORegistryEntryCreateIterator(io_registry_entry_t entry, const io_name_t plane, IOOptionBits options, io_iterator_t *iterator);
    private static final MethodHandle IORegistryCreateIterator$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryCreateIterator").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS)
    );
    public static int IORegistryCreateIterator(int entry, Addressable plane, int options, Addressable iterator) {
        try {
            return (int) IORegistryCreateIterator$Func.invokeExact(entry, plane, options, iterator);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // io_object_t IOIteratorNext(io_iterator_t iterator);
    private static final MethodHandle IOIteratorNext$Func = linker.downcallHandle(
            ioKitLookup.lookup("IOIteratorNext").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT)
    );
    public static int IOIteratorNext(int iter) {
        try {
            return (int) IOIteratorNext$Func.invokeExact(iter);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IORegistryEntryGetPath(io_registry_entry_t entry, const io_name_t plane, io_string_t path);
    private static final MethodHandle IORegistryEntryGetPath$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryEntryGetPath").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS)
    );
    public static int IORegistryEntryGetPath(int entry, Addressable plane, Addressable path) {
        try {
            return (int) IORegistryEntryGetPath$Func.invokeExact(entry, plane, path);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IOCreatePlugInInterfaceForService(io_service_t service, CFUUIDRef pluginType, CFUUIDRef interfaceType, IOCFPlugInInterface ***theInterface, SInt32 *theScore);
    private static final MethodHandle IOCreatePlugInInterfaceForService$Func = linker.downcallHandle(
            ioKitLookup.lookup("IOCreatePlugInInterfaceForService").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
    );
    public static int IOCreatePlugInInterfaceForService(int service, Addressable pluginType, Addressable interfaceType, Addressable intf, Addressable score) {
        try {
            return (int) IOCreatePlugInInterfaceForService$Func.invokeExact(service, pluginType, interfaceType, intf, score);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // CFTypeRef IORegistryEntryCreateCFProperty(io_registry_entry_t entry, CFStringRef key, CFAllocatorRef allocator, IOOptionBits options);
    private static final MethodHandle IORegistryEntryCreateCFProperty$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryEntryCreateCFProperty").get(),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)
    );
    public static MemoryAddress IORegistryEntryCreateCFProperty(int entry, String key, Addressable allocator, int options) {
        try {
            var keyStr = CoreFoundationHelper.createCFStringRef(key);
            var propValue = (MemoryAddress) IORegistryEntryCreateCFProperty$Func.invokeExact(entry, (Addressable) keyStr, allocator, options);
            CoreFoundation.CFRelease(keyStr);
            return propValue;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // io_registry_entry_t IORegistryEntryFromPath(mach_port_t mainPort, const io_string_t path);
    private static final MethodHandle IORegistryEntryFromPath$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryEntryFromPath").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS)
    );
    public static int IORegistryEntryFromPath(int mainPort, String path) {
        try (var session = MemorySession.openConfined()) {
            var pathCStr = session.allocateUtf8String(path);
            return (int) IORegistryEntryFromPath$Func.invokeExact(mainPort, (Addressable) pathCStr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // IONotificationPortRef IONotificationPortCreate(mach_port_t mainPort);
    private static final MethodHandle IONotificationPortCreate$Func = linker.downcallHandle(
            ioKitLookup.lookup("IONotificationPortCreate").get(),
            FunctionDescriptor.of(ADDRESS, JAVA_INT)
    );
    public static MemoryAddress IONotificationPortCreate(int mainPort) {
        try {
            return (MemoryAddress) IONotificationPortCreate$Func.invokeExact(mainPort);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFRunLoopSourceRef IONotificationPortGetRunLoopSource(IONotificationPortRef notify);
    private static final MethodHandle IONotificationPortGetRunLoopSource$Func = linker.downcallHandle(
            ioKitLookup.lookup("IONotificationPortGetRunLoopSource").get(),
            FunctionDescriptor.of(ADDRESS, ADDRESS)
    );
    public static MemoryAddress IONotificationPortGetRunLoopSource(Addressable notify) {
        try {
            return (MemoryAddress) IONotificationPortGetRunLoopSource$Func.invokeExact(notify);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // io_service_t IOServiceGetMatchingService(mach_port_t mainPort, CFDictionaryRef matching);
    private static final MethodHandle IOServiceGetMatchingService$Func = linker.downcallHandle(
            ioKitLookup.lookup("IOServiceGetMatchingService").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS)
    );
    public static int IOServiceGetMatchingService(int mainPort, Addressable matching) {
        try {
            return (int) IOServiceGetMatchingService$Func.invokeExact(mainPort, matching);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // kern_return_t IORegistryEntryGetRegistryEntryID(io_registry_entry_t entry, uint64_t *entryID);
    private static final MethodHandle IORegistryEntryGetRegistryEntryID$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryEntryGetRegistryEntryID").get(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS)
    );

    public static int IORegistryEntryGetRegistryEntryID(int entry, Addressable entryIdHolder) {
        try {
            return (int) IORegistryEntryGetRegistryEntryID$Func.invokeExact(entry, entryIdHolder);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // kern_return_t IOServiceAddMatchingNotification(IONotificationPortRef notifyPort, const io_name_t notificationType, CFDictionaryRef matching, IOServiceMatchingCallback callback, void *refCon, io_iterator_t *notification);
    private static final MethodHandle IOServiceAddMatchingNotification$Func = linker.downcallHandle(
            ioKitLookup.lookup("IOServiceAddMatchingNotification").get(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
    );
    public static int IOServiceAddMatchingNotification(Addressable notifyPort, Addressable notificationType, Addressable matching, Addressable callback, Addressable refCon, Addressable notificationHolder) {
        try {
            return (int) IOServiceAddMatchingNotification$Func.invokeExact(notifyPort, notificationType, matching, callback, refCon, notificationHolder);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFMutableDictionaryRef IOServiceMatching(const char *name);
    private static final MethodHandle IOServiceMatching$Func = linker.downcallHandle(
            ioKitLookup.lookup("IOServiceMatching").get(),
            FunctionDescriptor.of(ADDRESS, ADDRESS)
    );
    public static MemoryAddress IOServiceMatching(Addressable name) {
        try {
            return (MemoryAddress) IOServiceMatching$Func.invokeExact(name);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFMutableDictionaryRef IORegistryEntryIDMatching(uint64_t entryID);
    private static final MethodHandle IORegistryEntryIDMatching$Func = linker.downcallHandle(
            ioKitLookup.lookup("IORegistryEntryIDMatching").get(),
            FunctionDescriptor.of(ADDRESS, JAVA_LONG)
    );
    public static MemoryAddress IORegistryEntryIDMatching(long entryID) {
        try {
            return (MemoryAddress) IORegistryEntryIDMatching$Func.invokeExact(entryID);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static final MemoryAddress kIOUSBDeviceUserClientTypeID = CoreFoundation.CFUUIDCreateFromUUIDBytes(
            NULL,
            new byte[]{
                    (byte) 0x9d, (byte) 0xc7, (byte) 0xb7, (byte) 0x80,
                    (byte) 0x9e, (byte) 0xc0, (byte) 0x11, (byte) 0xD4,
                    (byte) 0xa5, (byte) 0x4f, (byte) 0x00, (byte) 0x0a,
                    (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
            });

    public static final MemoryAddress kIOUSBInterfaceUserClientTypeID = CoreFoundation.CFUUIDCreateFromUUIDBytes(
            NULL,
            new byte[]{
                    (byte) 0x2d, (byte) 0x97, (byte) 0x86, (byte) 0xc6,
                    (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4,
                    (byte) 0xad, (byte) 0x51, (byte) 0x00, (byte) 0x0a,
                    (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
            });

    public static final MemoryAddress kIOUSBDeviceInterfaceID100 = CoreFoundation.CFUUIDCreateFromUUIDBytes(
            NULL,
            new byte[]{
                    (byte) 0x5c, (byte) 0x81, (byte) 0x87, (byte) 0xd0,
                    (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4,
                    (byte) 0x8b, (byte) 0x45, (byte) 0x00, (byte) 0x0a,
                    (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
            });

    public static final MemoryAddress kIOUSBInterfaceInterfaceID100 = CoreFoundation.CFUUIDCreateFromUUIDBytes(
            NULL,
            new byte[]{
                    (byte) 0x73, (byte) 0xc9, (byte) 0x7a, (byte) 0xe8,
                    (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4,
                    (byte) 0xb1, (byte) 0xd0, (byte) 0x00, (byte) 0x0a,
                    (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
            });

    public static final MemoryAddress kIOCFPlugInInterfaceID = CoreFoundation.CFUUIDCreateFromUUIDBytes(
            NULL,
            new byte[]{
                    (byte) 0xC2, (byte) 0x44, (byte) 0xE8, (byte) 0x58,
                    (byte) 0x10, (byte) 0x9C, (byte) 0x11, (byte) 0xD4,
                    (byte) 0x91, (byte) 0xD4, (byte) 0x00, (byte) 0x50,
                    (byte) 0xE4, (byte) 0xC6, (byte) 0x42, (byte) 0x6F
            });

    static {
        try (var session = MemorySession.openConfined()) {
            var kIOMasterPortDefaultAddress = ioKitLookup.lookup("kIOMasterPortDefault").get().address();
            kIOMasterPortDefault = MemorySegment.ofAddress(kIOMasterPortDefaultAddress, 4, session).get(JAVA_INT, 0);
        }
    }
}
