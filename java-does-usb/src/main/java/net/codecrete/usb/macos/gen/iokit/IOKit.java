// Generated by jextract

package net.codecrete.usb.macos.gen.iokit;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.*;
public class IOKit  {

    /* package-private */ IOKit() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int kIOUSBFindInterfaceDontCare() {
        return (int)65535L;
    }
    public static MemoryLayout kCFRunLoopDefaultMode$LAYOUT() {
        return constants$0.kCFRunLoopDefaultMode$LAYOUT;
    }
    public static VarHandle kCFRunLoopDefaultMode$VH() {
        return constants$0.kCFRunLoopDefaultMode$VH;
    }
    public static MemorySegment kCFRunLoopDefaultMode$SEGMENT() {
        return RuntimeHelper.requireNonNull(constants$0.kCFRunLoopDefaultMode$SEGMENT,"kCFRunLoopDefaultMode");
    }
    public static MemoryAddress kCFRunLoopDefaultMode$get() {
        return (java.lang.foreign.MemoryAddress) constants$0.kCFRunLoopDefaultMode$VH.get(RuntimeHelper.requireNonNull(constants$0.kCFRunLoopDefaultMode$SEGMENT, "kCFRunLoopDefaultMode"));
    }
    public static void kCFRunLoopDefaultMode$set( MemoryAddress x) {
        constants$0.kCFRunLoopDefaultMode$VH.set(RuntimeHelper.requireNonNull(constants$0.kCFRunLoopDefaultMode$SEGMENT, "kCFRunLoopDefaultMode"), x);
    }
    public static MemoryLayout kIOMasterPortDefault$LAYOUT() {
        return constants$0.kIOMasterPortDefault$LAYOUT;
    }
    public static VarHandle kIOMasterPortDefault$VH() {
        return constants$0.kIOMasterPortDefault$VH;
    }
    public static MemorySegment kIOMasterPortDefault$SEGMENT() {
        return RuntimeHelper.requireNonNull(constants$0.kIOMasterPortDefault$SEGMENT,"kIOMasterPortDefault");
    }
    public static int kIOMasterPortDefault$get() {
        return (int) constants$0.kIOMasterPortDefault$VH.get(RuntimeHelper.requireNonNull(constants$0.kIOMasterPortDefault$SEGMENT, "kIOMasterPortDefault"));
    }
    public static void kIOMasterPortDefault$set( int x) {
        constants$0.kIOMasterPortDefault$VH.set(RuntimeHelper.requireNonNull(constants$0.kIOMasterPortDefault$SEGMENT, "kIOMasterPortDefault"), x);
    }
    public static MethodHandle IONotificationPortCreate$MH() {
        return RuntimeHelper.requireNonNull(constants$0.IONotificationPortCreate$MH,"IONotificationPortCreate");
    }
    public static MemoryAddress IONotificationPortCreate ( int mainPort) {
        var mh$ = IONotificationPortCreate$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(mainPort);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IONotificationPortGetRunLoopSource$MH() {
        return RuntimeHelper.requireNonNull(constants$0.IONotificationPortGetRunLoopSource$MH,"IONotificationPortGetRunLoopSource");
    }
    public static MemoryAddress IONotificationPortGetRunLoopSource ( Addressable notify) {
        var mh$ = IONotificationPortGetRunLoopSource$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(notify);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IOObjectRelease$MH() {
        return RuntimeHelper.requireNonNull(constants$0.IOObjectRelease$MH,"IOObjectRelease");
    }
    public static int IOObjectRelease ( int object) {
        var mh$ = IOObjectRelease$MH();
        try {
            return (int)mh$.invokeExact(object);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IOIteratorNext$MH() {
        return RuntimeHelper.requireNonNull(constants$0.IOIteratorNext$MH,"IOIteratorNext");
    }
    public static int IOIteratorNext ( int iterator) {
        var mh$ = IOIteratorNext$MH();
        try {
            return (int)mh$.invokeExact(iterator);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IOServiceAddMatchingNotification$MH() {
        return RuntimeHelper.requireNonNull(constants$1.IOServiceAddMatchingNotification$MH,"IOServiceAddMatchingNotification");
    }
    public static int IOServiceAddMatchingNotification ( Addressable notifyPort,  Addressable notificationType,  Addressable matching,  Addressable callback,  Addressable refCon,  Addressable notification) {
        var mh$ = IOServiceAddMatchingNotification$MH();
        try {
            return (int)mh$.invokeExact(notifyPort, notificationType, matching, callback, refCon, notification);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IORegistryEntryGetRegistryEntryID$MH() {
        return RuntimeHelper.requireNonNull(constants$1.IORegistryEntryGetRegistryEntryID$MH,"IORegistryEntryGetRegistryEntryID");
    }
    public static int IORegistryEntryGetRegistryEntryID ( int entry,  Addressable entryID) {
        var mh$ = IORegistryEntryGetRegistryEntryID$MH();
        try {
            return (int)mh$.invokeExact(entry, entryID);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IORegistryEntryCreateCFProperty$MH() {
        return RuntimeHelper.requireNonNull(constants$1.IORegistryEntryCreateCFProperty$MH,"IORegistryEntryCreateCFProperty");
    }
    public static MemoryAddress IORegistryEntryCreateCFProperty ( int entry,  Addressable key,  Addressable allocator,  int options) {
        var mh$ = IORegistryEntryCreateCFProperty$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(entry, key, allocator, options);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IOServiceMatching$MH() {
        return RuntimeHelper.requireNonNull(constants$1.IOServiceMatching$MH,"IOServiceMatching");
    }
    public static MemoryAddress IOServiceMatching ( Addressable name) {
        var mh$ = IOServiceMatching$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact(name);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static MethodHandle IOCreatePlugInInterfaceForService$MH() {
        return RuntimeHelper.requireNonNull(constants$1.IOCreatePlugInInterfaceForService$MH,"IOCreatePlugInInterfaceForService");
    }
    public static int IOCreatePlugInInterfaceForService ( int service,  Addressable pluginType,  Addressable interfaceType,  Addressable theInterface,  Addressable theScore) {
        var mh$ = IOCreatePlugInInterfaceForService$MH();
        try {
            return (int)mh$.invokeExact(service, pluginType, interfaceType, theInterface, theScore);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static int kIOReturnAborted() {
        return (int)-536870165L;
    }
    public static int kIOUSBTransactionTimeout() {
        return (int)-536854447L;
    }
    public static MemorySegment kIOFirstMatchNotification() {
        return constants$1.kIOFirstMatchNotification$SEGMENT;
    }
    public static MemorySegment kIOTerminatedNotification() {
        return constants$2.kIOTerminatedNotification$SEGMENT;
    }
    public static MemorySegment kIOUSBDeviceClassName() {
        return constants$2.kIOUSBDeviceClassName$SEGMENT;
    }
}


