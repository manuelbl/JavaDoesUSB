// Generated by jextract

package net.codecrete.usb.linux.gen.unistd;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
class constants$9 {

    static final FunctionDescriptor setreuid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle setreuid$MH = RuntimeHelper.downcallHandle(
        "setreuid",
        constants$9.setreuid$FUNC
    );
    static final FunctionDescriptor seteuid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle seteuid$MH = RuntimeHelper.downcallHandle(
        "seteuid",
        constants$9.seteuid$FUNC
    );
    static final FunctionDescriptor setgid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle setgid$MH = RuntimeHelper.downcallHandle(
        "setgid",
        constants$9.setgid$FUNC
    );
    static final FunctionDescriptor setregid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle setregid$MH = RuntimeHelper.downcallHandle(
        "setregid",
        constants$9.setregid$FUNC
    );
    static final FunctionDescriptor setegid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle setegid$MH = RuntimeHelper.downcallHandle(
        "setegid",
        constants$9.setegid$FUNC
    );
    static final FunctionDescriptor fork$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle fork$MH = RuntimeHelper.downcallHandle(
        "fork",
        constants$9.fork$FUNC
    );
}


