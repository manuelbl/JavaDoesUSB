// Generated by jextract

package net.codecrete.usb.macos.gen.corefoundation;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
final class constants$1 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$1() {}
    static final FunctionDescriptor CFNumberGetTypeID$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT);
    static final MethodHandle CFNumberGetTypeID$MH = RuntimeHelper.downcallHandle(
        "CFNumberGetTypeID",
        constants$1.CFNumberGetTypeID$FUNC
    );
    static final FunctionDescriptor CFNumberGetValue$FUNC = FunctionDescriptor.of(Constants$root.C_CHAR$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle CFNumberGetValue$MH = RuntimeHelper.downcallHandle(
        "CFNumberGetValue",
        constants$1.CFNumberGetValue$FUNC
    );
    static final FunctionDescriptor CFRunLoopGetCurrent$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT);
    static final MethodHandle CFRunLoopGetCurrent$MH = RuntimeHelper.downcallHandle(
        "CFRunLoopGetCurrent",
        constants$1.CFRunLoopGetCurrent$FUNC
    );
    static final FunctionDescriptor CFRunLoopRun$FUNC = FunctionDescriptor.ofVoid();
    static final MethodHandle CFRunLoopRun$MH = RuntimeHelper.downcallHandle(
        "CFRunLoopRun",
        constants$1.CFRunLoopRun$FUNC
    );
    static final FunctionDescriptor CFRunLoopAddSource$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle CFRunLoopAddSource$MH = RuntimeHelper.downcallHandle(
        "CFRunLoopAddSource",
        constants$1.CFRunLoopAddSource$FUNC
    );
    static final FunctionDescriptor CFRunLoopRemoveSource$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle CFRunLoopRemoveSource$MH = RuntimeHelper.downcallHandle(
        "CFRunLoopRemoveSource",
        constants$1.CFRunLoopRemoveSource$FUNC
    );
}


