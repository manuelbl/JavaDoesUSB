// Generated by jextract

package net.codecrete.usb.linux.gen.ioctl;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
class constants$0 {

    static final FunctionDescriptor ioctl$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle ioctl$MH = RuntimeHelper.downcallHandleVariadic(
        "ioctl",
        constants$0.ioctl$FUNC
    );
}


