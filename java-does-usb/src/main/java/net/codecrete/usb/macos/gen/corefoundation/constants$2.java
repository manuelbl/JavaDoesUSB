// Generated by jextract

package net.codecrete.usb.macos.gen.corefoundation;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.invoke.MethodHandle;
class constants$2 {

    static final FunctionDescriptor CFUUIDCreateFromUUIDBytes$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        MemoryLayout.structLayout(
            Constants$root.C_CHAR$LAYOUT.withName("byte0"),
            Constants$root.C_CHAR$LAYOUT.withName("byte1"),
            Constants$root.C_CHAR$LAYOUT.withName("byte2"),
            Constants$root.C_CHAR$LAYOUT.withName("byte3"),
            Constants$root.C_CHAR$LAYOUT.withName("byte4"),
            Constants$root.C_CHAR$LAYOUT.withName("byte5"),
            Constants$root.C_CHAR$LAYOUT.withName("byte6"),
            Constants$root.C_CHAR$LAYOUT.withName("byte7"),
            Constants$root.C_CHAR$LAYOUT.withName("byte8"),
            Constants$root.C_CHAR$LAYOUT.withName("byte9"),
            Constants$root.C_CHAR$LAYOUT.withName("byte10"),
            Constants$root.C_CHAR$LAYOUT.withName("byte11"),
            Constants$root.C_CHAR$LAYOUT.withName("byte12"),
            Constants$root.C_CHAR$LAYOUT.withName("byte13"),
            Constants$root.C_CHAR$LAYOUT.withName("byte14"),
            Constants$root.C_CHAR$LAYOUT.withName("byte15")
        )
    );
    static final MethodHandle CFUUIDCreateFromUUIDBytes$MH = RuntimeHelper.downcallHandle(
        "CFUUIDCreateFromUUIDBytes",
        constants$2.CFUUIDCreateFromUUIDBytes$FUNC
    );
}


