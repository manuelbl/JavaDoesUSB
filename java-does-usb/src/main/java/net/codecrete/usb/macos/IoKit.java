//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.Foreign;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.*;
import static net.codecrete.usb.macos.CoreFoundation.*;

public class IoKit {

    public static final int kIOMasterPortDefault;
    public static final Addressable kIOUSBPlane;
    public static final Addressable kIOServicePlane;
    public static final int kIORegistryIterateRecursively = 1;
    public static final MemoryAddress kIOUSBDeviceUserClientTypeID;
    public static final MemoryAddress kIOUSBInterfaceUserClientTypeID;
    public static final MemoryAddress kIOUSBDeviceInterfaceID100;
    public static final MemoryAddress kIOUSBInterfaceInterfaceID100;
    public static final MemoryAddress kIOCFPlugInInterfaceID;

    public static final GroupLayout IOCFPlugInInterface$Struct;
    private static final VarHandle IUnknown_QueryInterface;
    private static final VarHandle IUnknown_AddRef;
    private static final VarHandle IUnknown_Release;

    private static final MethodHandle QueryInterface$Func;
    private static final MethodHandle AddRef$Func;
    private static final MethodHandle Release$Func;

    private static final MethodHandle IORegistryGetRootEntry$Func;
    private static final MethodHandle IOObjectRelease$Func;
    private static final MethodHandle IORegistryCreateIterator$Func;
    private static final MethodHandle IOIteratorNext$Func;
    private static final MethodHandle IORegistryEntryCreateCFProperty$Func;
    private static final MethodHandle IORegistryEntryGetPath$Func;
    private static final MethodHandle IOCreatePlugInInterfaceForService$Func;
    private static final MethodHandle IORegistryEntryFromPath$Func;

    static {
        var linker = Linker.nativeLinker();
        var ioKitSession = MemorySession.openShared();
        var ioKitLookup = SymbolLookup.libraryLookup("IOKit.framework/IOKit", ioKitSession);

        try (var session = MemorySession.openConfined()) {
            var kIOMasterPortDefaultAddress = ioKitLookup.lookup("kIOMasterPortDefault").get().address();
            kIOMasterPortDefault = MemorySegment.ofAddress(kIOMasterPortDefaultAddress, 4, session).get(JAVA_INT, 0);
        }

        kIOUSBPlane = ioKitSession.allocateUtf8String("IOUSB");
        kIOServicePlane = ioKitSession.allocateUtf8String("IOService");

        IOCFPlugInInterface$Struct = MemoryLayout.structLayout(
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

        IUnknown_QueryInterface = IOCFPlugInInterface$Struct.varHandle(groupElement("QueryInterface"));
        IUnknown_AddRef = IOCFPlugInInterface$Struct.varHandle(groupElement("AddRef"));
        IUnknown_Release = IOCFPlugInInterface$Struct.varHandle(groupElement("Release"));

        // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv);
        QueryInterface$Func = linker.downcallHandle(
                FunctionDescriptor.of(JAVA_INT, ADDRESS, CFUUIDBytes, ADDRESS)
        );

        // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer);
        AddRef$Func = linker.downcallHandle(
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );

        // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer);
        Release$Func = linker.downcallHandle(
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );


        // io_registry_entry_t IORegistryGetRootEntry(mach_port_t mainPort);
        IORegistryGetRootEntry$Func = linker.downcallHandle(
                ioKitLookup.lookup("IORegistryGetRootEntry").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT)
        );

        // kern_return_t IOObjectRelease(io_object_t object);
        IOObjectRelease$Func = linker.downcallHandle(
                ioKitLookup.lookup("IOObjectRelease").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT)
        );

        // kern_return_t IORegistryEntryCreateIterator(io_registry_entry_t entry, const io_name_t plane, IOOptionBits options, io_iterator_t *iterator);
        IORegistryCreateIterator$Func = linker.downcallHandle(
                ioKitLookup.lookup("IORegistryCreateIterator").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS)
        );

        // io_object_t IOIteratorNext(io_iterator_t iterator);
        IOIteratorNext$Func = linker.downcallHandle(
                ioKitLookup.lookup("IOIteratorNext").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT)
        );

        // kern_return_t IORegistryEntryGetPath(io_registry_entry_t entry, const io_name_t plane, io_string_t path);
        IORegistryEntryGetPath$Func = linker.downcallHandle(
                ioKitLookup.lookup("IORegistryEntryGetPath").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS)
        );

        // CFTypeRef IORegistryEntryCreateCFProperty(io_registry_entry_t entry, CFStringRef key, CFAllocatorRef allocator, IOOptionBits options);
        IORegistryEntryCreateCFProperty$Func = linker.downcallHandle(
                ioKitLookup.lookup("IORegistryEntryCreateCFProperty").get(),
                FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)
        );

        // kern_return_t IOCreatePlugInInterfaceForService(io_service_t service, CFUUIDRef pluginType, CFUUIDRef interfaceType, IOCFPlugInInterface ***theInterface, SInt32 *theScore);
        IOCreatePlugInInterfaceForService$Func = linker.downcallHandle(
                ioKitLookup.lookup("IOCreatePlugInInterfaceForService").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
        );

        // io_registry_entry_t IORegistryEntryFromPath(mach_port_t mainPort, const io_string_t path);
        IORegistryEntryFromPath$Func = linker.downcallHandle(
                ioKitLookup.lookup("IORegistryEntryFromPath").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS)
        );

        kIOUSBDeviceUserClientTypeID = CoreFoundation.CFUUIDCreateFromUUIDBytes(
                NULL,
                new byte[]{
                        (byte) 0x9d, (byte) 0xc7, (byte) 0xb7, (byte) 0x80,
                        (byte) 0x9e, (byte) 0xc0, (byte) 0x11, (byte) 0xD4,
                        (byte) 0xa5, (byte) 0x4f, (byte) 0x00, (byte) 0x0a,
                        (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
                });

        kIOUSBInterfaceUserClientTypeID = CoreFoundation.CFUUIDCreateFromUUIDBytes(
                NULL,
                new byte[]{
                        (byte) 0x2d, (byte) 0x97, (byte) 0x86, (byte) 0xc6,
                        (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4,
                        (byte) 0xad, (byte) 0x51, (byte) 0x00, (byte) 0x0a,
                        (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
                });

        kIOUSBDeviceInterfaceID100 = CoreFoundation.CFUUIDCreateFromUUIDBytes(
                NULL,
                new byte[]{
                        (byte) 0x5c, (byte) 0x81, (byte) 0x87, (byte) 0xd0,
                        (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4,
                        (byte) 0x8b, (byte) 0x45, (byte) 0x00, (byte) 0x0a,
                        (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
                });

        kIOUSBInterfaceInterfaceID100 = CoreFoundation.CFUUIDCreateFromUUIDBytes(
                NULL,
                new byte[]{
                        (byte) 0x73, (byte) 0xc9, (byte) 0x7a, (byte) 0xe8,
                        (byte) 0x9e, (byte) 0xf3, (byte) 0x11, (byte) 0xD4,
                        (byte) 0xb1, (byte) 0xd0, (byte) 0x00, (byte) 0x0a,
                        (byte) 0x27, (byte) 0x05, (byte) 0x28, (byte) 0x61
                });

        kIOCFPlugInInterfaceID = CoreFoundation.CFUUIDCreateFromUUIDBytes(
                NULL,
                new byte[]{
                        (byte) 0xC2, (byte) 0x44, (byte) 0xE8, (byte) 0x58,
                        (byte) 0x10, (byte) 0x9C, (byte) 0x11, (byte) 0xD4,
                        (byte) 0x91, (byte) 0xD4, (byte) 0x00, (byte) 0x50,
                        (byte) 0xE4, (byte) 0xC6, (byte) 0x42, (byte) 0x6F
                });
    }

    // io_registry_entry_t IORegistryGetRootEntry(mach_port_t mainPort);
    public static int IORegistryGetRootEntry(int mainPort) {
        try {
            return (int) IORegistryGetRootEntry$Func.invokeExact(mainPort);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IOObjectRelease(io_object_t object);
    public static int IOObjectRelease(int object) {
        try {
            return (int) IOObjectRelease$Func.invokeExact(object);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IORegistryEntryCreateIterator(io_registry_entry_t entry, const io_name_t plane, IOOptionBits options, io_iterator_t *iterator);
    public static int IORegistryCreateIterator(int entry, Addressable plane, int options, Addressable iterator) {
        try {
            return (int) IORegistryCreateIterator$Func.invokeExact(entry, plane, options, iterator);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // io_object_t IOIteratorNext(io_iterator_t iterator);
    public static int IOIteratorNext(int iter) {
        try {
            return (int) IOIteratorNext$Func.invokeExact(iter);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // CFTypeRef IORegistryEntryCreateCFProperty(io_registry_entry_t entry, CFStringRef key, CFAllocatorRef allocator, IOOptionBits options);
    public static MemoryAddress IORegistryEntryCreateCFProperty(int entry, String key, Addressable allocator, int options) {
        try {
            var keyStr = CoreFoundation.javaStringToCfString(key);
            var propValue = (MemoryAddress) IORegistryEntryCreateCFProperty$Func.invokeExact(entry, (Addressable) keyStr, allocator, options);
            CoreFoundation.CFRelease(keyStr);
            return propValue;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IORegistryEntryGetPath(io_registry_entry_t entry, const io_name_t plane, io_string_t path);
    public static int IORegistryEntryGetPath(int entry, Addressable plane, Addressable path) {
        try {
            return (int) IORegistryEntryGetPath$Func.invokeExact(entry, plane, path);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // kern_return_t IOCreatePlugInInterfaceForService(io_service_t service, CFUUIDRef pluginType, CFUUIDRef interfaceType, IOCFPlugInInterface ***theInterface, SInt32 *theScore);
    public static int IOCreatePlugInInterfaceForService(int service, Addressable pluginType, Addressable interfaceType, Addressable intf, Addressable score) {
        try {
            return (int) IOCreatePlugInInterfaceForService$Func.invokeExact(service, pluginType, interfaceType, intf, score);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // io_registry_entry_t IORegistryEntryFromPath(mach_port_t mainPort, const io_string_t path);
    public static int IORegistryEntryFromPath(int mainPort, String path) {
        try (var session = MemorySession.openConfined()) {
            var pathCStr = session.allocateUtf8String(path);
            return (int) IORegistryEntryFromPath$Func.invokeExact(mainPort, (Addressable) pathCStr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Addressable IOCFPlugInInterface$VirtFunc(MemoryAddress thisPointer, VarHandle varHandle, MemorySession session) {
        var seg = MemorySegment.ofAddress(thisPointer, ADDRESS.byteSize(), session);
        var thisValue = seg.get(ADDRESS, 0);
        var obj = MemorySegment.ofAddress(thisValue, IOCFPlugInInterface$Struct.byteSize(), session);
        return (MemoryAddress) varHandle.get(obj);
    }

    // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv);
    public static int QueryInterface(MemoryAddress thisPointer, MemorySegment iid, Addressable ppv) {
        try (var session = MemorySession.openShared()) {
            var funcPtr = IOCFPlugInInterface$VirtFunc(thisPointer, IUnknown_QueryInterface, session);
            return (int) QueryInterface$Func.invokeExact(funcPtr, (Addressable) thisPointer, iid, ppv);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer);
    public static int AddRef(MemoryAddress thisPointer) {
        try (var session = MemorySession.openShared()) {
            var funcPtr = IOCFPlugInInterface$VirtFunc(thisPointer, IUnknown_AddRef, session);
            return (int) AddRef$Func.invokeExact(funcPtr, (Addressable) thisPointer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer);
    public static int Release(MemoryAddress thisPointer) {
        try (var session = MemorySession.openShared()) {
            var funcPtr = IOCFPlugInInterface$VirtFunc(thisPointer, IUnknown_Release, session);
            return (int) Release$Func.invokeExact(funcPtr, (Addressable) thisPointer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

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
    public static MemoryAddress GetInterface(int service, Addressable pluginType, MemoryAddress interfaceId) {
        try (var session = MemorySession.openConfined()) {
            // MemorySegment for holding IOCFPlugInInterface**
            var plugPointer = session.allocate(ADDRESS, NULL);
            // MemorySegment for holding score
            var score = session.allocate(JAVA_INT, 0);
            int ret = IOCreatePlugInInterfaceForService(service, pluginType, kIOCFPlugInInterfaceID, plugPointer, score);
            if (ret != 0)
                return null;

            var plug = Foreign.derefAddress(plugPointer.address(), session);
            // MemorySegment for holding XXXInterface**
            var intf = session.allocate(ADDRESS, NULL);
            // UUID bytes
            var refiid = MemorySegment.ofAddress(interfaceId.addOffset(CFUUID_bytes$Offset), CFUUID.byteSize(), session);
            ret = QueryInterface(plug, refiid, intf);
            Release(plug);

            if (ret != 0)
                return null;
            return Foreign.derefAddress(intf.address(), session);
        }

    }
}
