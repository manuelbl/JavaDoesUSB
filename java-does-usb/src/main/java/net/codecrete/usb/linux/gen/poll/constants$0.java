// Generated by jextract

package net.codecrete.usb.linux.gen.poll;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
class constants$0 {

    static final FunctionDescriptor poll$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle poll$MH = RuntimeHelper.downcallHandle(
        "poll",
        constants$0.poll$FUNC
    );
}


