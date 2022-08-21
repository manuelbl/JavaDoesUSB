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
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.*;

public class CoreFoundation {
    // Fixed-width types
    public static final long kCFNumberSInt8Type = 1;
    public static final long kCFNumberSInt16Type = 2;
    public static final long kCFNumberSInt32Type = 3;
    public static final long kCFNumberSInt64Type = 4;
    public static final long kCFNumberFloat32Type = 5;
    public static final long kCFNumberFloat64Type = 6;    // 64-bit IEEE 754
    // Basic C types
    public static final long kCFNumberCharType = 7;
    public static final long kCFNumberShortType = 8;
    public static final long kCFNumberIntType = 9;
    public static final long kCFNumberLongType = 10;
    public static final long kCFNumberLongLongType = 11;
    public static final long kCFNumberFloatType = 12;
    public static final long kCFNumberDoubleType = 13;
    // Other
    public static final long kCFNumberCFIndexType = 14;
    public static final long kCFNumberNSIntegerType = 15;
    public static final long kCFNumberCGFloatType = 16;

    public static final GroupLayout CFUUIDBytes;

    public static final GroupLayout CFUUID;
    public static final long CFUUID_bytes$Offset;

    public static final GroupLayout CFRange;
    public static final VarHandle CFRange_location;
    public static final VarHandle CFRange_range;


    //    private static final MethodHandle CFUUIDGetConstantUUIDWithBytes$Func;
    private static final MethodHandle CFUUIDCreateFromUUIDBytes$Func;
    private static final MethodHandle CFUUIDCreateString$Func;
    private static final MethodHandle CFRelease$Func;
    private static final MethodHandle CFStringGetLength$Func;
    private static final MethodHandle CFStringGetCharacters$Func;
    private static final MethodHandle CFStringCreateWithCharacters$Func;
    private static final MethodHandle CFGetTypeID$Func;
    private static final MethodHandle CFNumberGetTypeID$Func;
    private static final MethodHandle CFStringGetTypeID$Func;
    private static final MethodHandle CFNumberGetType$Func;
    private static final MethodHandle CFNumberGetValue$Func;

    static {
        var linker = Linker.nativeLinker();
        var cfSession = MemorySession.openShared();
        var coreFoundationLookup = Kernel.frameworkLookup("CoreFoundation", cfSession);

        CFUUIDBytes = structLayout(
                JAVA_BYTE.withName("byte0"),
                JAVA_BYTE.withName("byte1"),
                JAVA_BYTE.withName("byte2"),
                JAVA_BYTE.withName("byte3"),
                JAVA_BYTE.withName("byte4"),
                JAVA_BYTE.withName("byte5"),
                JAVA_BYTE.withName("byte6"),
                JAVA_BYTE.withName("byte7"),
                JAVA_BYTE.withName("byte8"),
                JAVA_BYTE.withName("byte9"),
                JAVA_BYTE.withName("byte10"),
                JAVA_BYTE.withName("byte11"),
                JAVA_BYTE.withName("byte12"),
                JAVA_BYTE.withName("byte13"),
                JAVA_BYTE.withName("byte14"),
                JAVA_BYTE.withName("byte15")
        );
        CFUUID = structLayout(
                ADDRESS.withName("_cfisa"), // uintptr_t _cfisa;
                sequenceLayout(4, JAVA_BYTE).withName("_cfinfo"), // uint8_t _cfinfo[4];
                JAVA_INT.withName("_rc"), // uint32_t _rc;
                CFUUIDBytes.withName("_bytes") // CFUUIDBytes _bytes;
        );
        CFUUID_bytes$Offset = CFUUID.byteOffset(MemoryLayout.PathElement.groupElement("_bytes"));
        CFRange = structLayout(
                JAVA_LONG.withName("location"),
                JAVA_LONG.withName("range")
        );
        CFRange_location = CFRange.varHandle(MemoryLayout.PathElement.groupElement("location"));
        CFRange_range = CFRange.varHandle(MemoryLayout.PathElement.groupElement("range"));

        // CFUUIDRef CFUUIDGetConstantUUIDWithBytes(CFAllocatorRef alloc, uint8_t byte0, uint8_t byte1, uint8_t byte2, uint8_t byte3, uint8_t byte4, uint8_t byte5, uint8_t byte6, uint8_t byte7, uint8_t byte8, uint8_t byte9, uint8_t byte10, uint8_t byte11, uint8_t byte12, uint8_t byte13, uint8_t byte14, uint8_t byte15)
//        CFUUIDGetConstantUUIDWithBytes$Func = linker.downcallHandle(
//                coreFoundationLookup.lookup("CFUUIDGetConstantUUIDWithBytes").get(),
//                FunctionDescriptor.of(
//                        ADDRESS, ADDRESS,
//                        JAVA_BYTE, JAVA_BYTE, JAVA_BYTE, JAVA_BYTE,
//                        JAVA_BYTE, JAVA_BYTE, JAVA_BYTE, JAVA_BYTE,
//                        JAVA_BYTE, JAVA_BYTE, JAVA_BYTE, JAVA_BYTE,
//                        JAVA_BYTE, JAVA_BYTE, JAVA_BYTE, JAVA_BYTE)
//        );

        // CFUUIDRef CFUUIDCreateFromUUIDBytes(CFAllocatorRef alloc, CFUUIDBytes bytes);
        CFUUIDCreateFromUUIDBytes$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFUUIDCreateFromUUIDBytes").get(),
                FunctionDescriptor.of(
                        ADDRESS, ADDRESS, CFUUIDBytes
                )
        );

        // CFStringRef CFUUIDCreateString(CFAllocatorRef alloc, CFUUIDRef uuid);
        CFUUIDCreateString$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFUUIDCreateString").get(),
                FunctionDescriptor.of(
                        ADDRESS, ADDRESS, ADDRESS
                )
        );

        // void CFRelease(CFTypeRef cf);
        CFRelease$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFRelease").get(),
                FunctionDescriptor.ofVoid(ADDRESS)
        );

        // CFIndex CFStringGetLength(CFStringRef theString);
        CFStringGetLength$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFStringGetLength").get(),
                FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        );

        // void CFStringGetCharacters(CFStringRef theString, CFRange range, UniChar *buffer);
        CFStringGetCharacters$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFStringGetCharacters").get(),
                FunctionDescriptor.ofVoid(ADDRESS, CFRange, ADDRESS)
        );

        // CFStringRef CFStringCreateWithCharacters(CFAllocatorRef alloc, const UniChar *chars, CFIndex numChars);
        CFStringCreateWithCharacters$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFStringCreateWithCharacters").get(),
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG)
        );

        // CFTypeID CFGetTypeID(CFTypeRef cf);
        CFGetTypeID$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFGetTypeID").get(),
                FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        );

        // CFTypeID CFNumberGetTypeID(void);
        CFNumberGetTypeID$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFNumberGetTypeID").get(),
                FunctionDescriptor.of(JAVA_LONG)
        );

        // CFTypeID CFStringGetTypeID(void);
        CFStringGetTypeID$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFStringGetTypeID").get(),
                FunctionDescriptor.of(JAVA_LONG)
        );

        // CFNumberType CFNumberGetType(CFNumberRef number);
        CFNumberGetType$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFNumberGetType").get(),
                FunctionDescriptor.of(JAVA_LONG, ADDRESS)
        );

        // Boolean CFNumberGetValue(CFNumberRef number, CFNumberType theType, void *valuePtr);
        CFNumberGetValue$Func = linker.downcallHandle(
                coreFoundationLookup.lookup("CFNumberGetValue").get(),
                FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, JAVA_LONG, ADDRESS)
        );
    }

    public static String cfStringToJavaString(MemoryAddress string) {
        try (var session = MemorySession.openConfined()) {
            long strLen = CFStringGetLength(string);
            var buffer = session.allocateArray(JAVA_CHAR, strLen);
            var range = session.allocate(CFRange);
            CFRange_location.set(range, 0);
            CFRange_range.set(range, strLen);
            CFStringGetCharacters(string, range, buffer);
            return new String(buffer.toArray(JAVA_CHAR));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemoryAddress javaStringToCfString(String string) {
        try (var session = MemorySession.openConfined()) {
            char[] charArray = string.toCharArray();
            var chars = session.allocateArray(JAVA_CHAR, charArray.length);
            chars.copyFrom(MemorySegment.ofArray(charArray));
            return CFStringCreateWithCharacters(NULL, chars, string.length());
        }
    }

    // CFUUIDRef CFUUIDCreateFromUUIDBytes(CFAllocatorRef alloc, CFUUIDBytes bytes);
    public static MemoryAddress CFUUIDCreateFromUUIDBytes(Addressable allocator, byte[] bytes) {
        try (var session = MemorySession.openConfined()) {
            var uuidBytes = session.allocate(16);
            uuidBytes.asByteBuffer().put(bytes);
            return (MemoryAddress) CFUUIDCreateFromUUIDBytes$Func.invokeExact(allocator, uuidBytes);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFStringRef CFUUIDCreateString(CFAllocatorRef alloc, CFUUIDRef uuid);
    public static String CFUUIDCreateString(Addressable allocator, Addressable uuid) {
        try {
            MemoryAddress str = (MemoryAddress) CFUUIDCreateString$Func.invokeExact(allocator, uuid);
            String result = cfStringToJavaString(str);
            CFRelease(str);
            return result;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // void CFRelease(CFTypeRef cf);
    public static void CFRelease(Addressable object) {
        try {
            CFRelease$Func.invokeExact(object);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFIndex CFStringGetLength(CFStringRef theString);
    public static long CFStringGetLength(Addressable str) {
        try {
            return (long) CFStringGetLength$Func.invokeExact(str);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // void CFStringGetCharacters(CFStringRef theString, CFRange range, UniChar *buffer);
    public static void CFStringGetCharacters(Addressable str, MemorySegment range, Addressable buffer) {
        try {
            CFStringGetCharacters$Func.invokeExact(str, range, buffer);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFStringRef CFStringCreateWithCharacters(CFAllocatorRef alloc, const UniChar *chars, CFIndex numChars);
    public static MemoryAddress CFStringCreateWithCharacters(Addressable allocator, Addressable chars, long numChars) {
        try {
            return (MemoryAddress) CFStringCreateWithCharacters$Func.invokeExact(allocator, chars, numChars);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFTypeID CFGetTypeID(CFTypeRef cf);
    public static long CFGetTypeID(Addressable cf) {
        try {
            return (long) CFGetTypeID$Func.invokeExact(cf);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFTypeID CFNumberGetTypeID(void);
    public static long CFNumberGetTypeID() {
        try {
            return (long) CFNumberGetTypeID$Func.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFTypeID CFStringGetTypeID(void);
    public static long CFStringGetTypeID() {
        try {
            return (long) CFStringGetTypeID$Func.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // CFNumberType CFNumberGetType(CFNumberRef number);
    public static long CFNumberGetType(Addressable number) {
        try {
            return (long) CFNumberGetType$Func.invokeExact(number);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // Boolean CFNumberGetValue(CFNumberRef number, CFNumberType theType, void *valuePtr);
    public static boolean CFNumberGetValue(Addressable number, long theType, Addressable valuePtr) {
        try {
            return (boolean) CFNumberGetValue$Func.invokeExact(number, theType, valuePtr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
