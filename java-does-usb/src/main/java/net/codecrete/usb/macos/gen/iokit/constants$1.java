// Generated by jextract

package net.codecrete.usb.macos.gen.iokit;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
class constants$1 {

    static final FunctionDescriptor IOServiceAddMatchingNotification$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle IOServiceAddMatchingNotification$MH = RuntimeHelper.downcallHandle(
        "IOServiceAddMatchingNotification",
        constants$1.IOServiceAddMatchingNotification$FUNC
    );
    static final FunctionDescriptor IORegistryEntryGetRegistryEntryID$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle IORegistryEntryGetRegistryEntryID$MH = RuntimeHelper.downcallHandle(
        "IORegistryEntryGetRegistryEntryID",
        constants$1.IORegistryEntryGetRegistryEntryID$FUNC
    );
    static final FunctionDescriptor IORegistryEntryCreateCFProperty$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle IORegistryEntryCreateCFProperty$MH = RuntimeHelper.downcallHandle(
        "IORegistryEntryCreateCFProperty",
        constants$1.IORegistryEntryCreateCFProperty$FUNC
    );
    static final FunctionDescriptor IOServiceMatching$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle IOServiceMatching$MH = RuntimeHelper.downcallHandle(
        "IOServiceMatching",
        constants$1.IOServiceMatching$FUNC
    );
    static final FunctionDescriptor IOCreatePlugInInterfaceForService$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle IOCreatePlugInInterfaceForService$MH = RuntimeHelper.downcallHandle(
        "IOCreatePlugInInterfaceForService",
        constants$1.IOCreatePlugInInterfaceForService$FUNC
    );
    static final MemorySegment kIOFirstMatchNotification$SEGMENT = RuntimeHelper.CONSTANT_ALLOCATOR.allocateUtf8String("IOServiceFirstMatch");
}

