// Generated by jextract

package net.codecrete.usb.windows.gen.setupapi;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
class constants$0 {

    static final FunctionDescriptor SetupDiEnumDeviceInfo$FUNC = FunctionDescriptor.of(Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SetupDiEnumDeviceInfo$MH = RuntimeHelper.downcallHandle(
        "SetupDiEnumDeviceInfo",
        constants$0.SetupDiEnumDeviceInfo$FUNC
    );
    static final FunctionDescriptor SetupDiDestroyDeviceInfoList$FUNC = FunctionDescriptor.of(Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SetupDiDestroyDeviceInfoList$MH = RuntimeHelper.downcallHandle(
        "SetupDiDestroyDeviceInfoList",
        constants$0.SetupDiDestroyDeviceInfoList$FUNC
    );
    static final FunctionDescriptor SetupDiEnumDeviceInterfaces$FUNC = FunctionDescriptor.of(Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SetupDiEnumDeviceInterfaces$MH = RuntimeHelper.downcallHandle(
        "SetupDiEnumDeviceInterfaces",
        constants$0.SetupDiEnumDeviceInterfaces$FUNC
    );
    static final FunctionDescriptor SetupDiGetDeviceInterfaceDetailW$FUNC = FunctionDescriptor.of(Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SetupDiGetDeviceInterfaceDetailW$MH = RuntimeHelper.downcallHandle(
        "SetupDiGetDeviceInterfaceDetailW",
        constants$0.SetupDiGetDeviceInterfaceDetailW$FUNC
    );
    static final FunctionDescriptor SetupDiGetClassDevsW$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT
    );
    static final MethodHandle SetupDiGetClassDevsW$MH = RuntimeHelper.downcallHandle(
        "SetupDiGetClassDevsW",
        constants$0.SetupDiGetClassDevsW$FUNC
    );
    static final FunctionDescriptor SetupDiGetDevicePropertyW$FUNC = FunctionDescriptor.of(Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG$LAYOUT
    );
    static final MethodHandle SetupDiGetDevicePropertyW$MH = RuntimeHelper.downcallHandle(
        "SetupDiGetDevicePropertyW",
        constants$0.SetupDiGetDevicePropertyW$FUNC
    );
}


