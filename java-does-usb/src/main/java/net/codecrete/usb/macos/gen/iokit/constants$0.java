// Generated by jextract

package net.codecrete.usb.macos.gen.iokit;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.OfAddress;
import static java.lang.foreign.ValueLayout.OfInt;
class constants$0 {

    static final  OfAddress kCFRunLoopDefaultMode$LAYOUT = Constants$root.C_POINTER$LAYOUT;
    static final VarHandle kCFRunLoopDefaultMode$VH = constants$0.kCFRunLoopDefaultMode$LAYOUT.varHandle();
    static final MemorySegment kCFRunLoopDefaultMode$SEGMENT = RuntimeHelper.lookupGlobalVariable("kCFRunLoopDefaultMode", constants$0.kCFRunLoopDefaultMode$LAYOUT);
    static final  OfInt kIOMasterPortDefault$LAYOUT = Constants$root.C_INT$LAYOUT;
    static final VarHandle kIOMasterPortDefault$VH = constants$0.kIOMasterPortDefault$LAYOUT.varHandle();
    static final MemorySegment kIOMasterPortDefault$SEGMENT = RuntimeHelper.lookupGlobalVariable("kIOMasterPortDefault", constants$0.kIOMasterPortDefault$LAYOUT);
    static final FunctionDescriptor IONotificationPortCreate$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle IONotificationPortCreate$MH = RuntimeHelper.downcallHandle(
        "IONotificationPortCreate",
        constants$0.IONotificationPortCreate$FUNC
    );
    static final FunctionDescriptor IONotificationPortGetRunLoopSource$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle IONotificationPortGetRunLoopSource$MH = RuntimeHelper.downcallHandle(
        "IONotificationPortGetRunLoopSource",
        constants$0.IONotificationPortGetRunLoopSource$FUNC
    );
    static final FunctionDescriptor IOObjectRelease$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle IOObjectRelease$MH = RuntimeHelper.downcallHandle(
        "IOObjectRelease",
        constants$0.IOObjectRelease$FUNC
    );
    static final FunctionDescriptor IOIteratorNext$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle IOIteratorNext$MH = RuntimeHelper.downcallHandle(
        "IOIteratorNext",
        constants$0.IOIteratorNext$FUNC
    );
}


