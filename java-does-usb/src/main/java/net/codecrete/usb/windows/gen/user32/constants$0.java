// Generated by jextract

package net.codecrete.usb.windows.gen.user32;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryAddress;
import java.lang.invoke.MethodHandle;
class constants$0 {

    static final FunctionDescriptor GetMessageW$FUNC = FunctionDescriptor.of(Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_LONG$LAYOUT
    );
    static final MethodHandle GetMessageW$MH = RuntimeHelper.downcallHandle(
        "GetMessageW",
        constants$0.GetMessageW$FUNC
    );
    static final FunctionDescriptor RegisterDeviceNotificationW$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT
    );
    static final MethodHandle RegisterDeviceNotificationW$MH = RuntimeHelper.downcallHandle(
        "RegisterDeviceNotificationW",
        constants$0.RegisterDeviceNotificationW$FUNC
    );
    static final FunctionDescriptor DefWindowProcW$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle DefWindowProcW$MH = RuntimeHelper.downcallHandle(
        "DefWindowProcW",
        constants$0.DefWindowProcW$FUNC
    );
    static final FunctionDescriptor RegisterClassExW$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle RegisterClassExW$MH = RuntimeHelper.downcallHandle(
        "RegisterClassExW",
        constants$0.RegisterClassExW$FUNC
    );
    static final FunctionDescriptor CreateWindowExW$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle CreateWindowExW$MH = RuntimeHelper.downcallHandle(
        "CreateWindowExW",
        constants$0.CreateWindowExW$FUNC
    );
    static final MemoryAddress HWND_MESSAGE$ADDR = MemoryAddress.ofLong(-3L);
}


