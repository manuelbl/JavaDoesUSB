// Generated by jextract

package net.codecrete.usb.linux.gen.udev;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
class constants$1 {

    static final FunctionDescriptor udev_device_get_devnode$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle udev_device_get_devnode$MH = RuntimeHelper.downcallHandle(
        "udev_device_get_devnode",
        constants$1.udev_device_get_devnode$FUNC
    );
    static final FunctionDescriptor udev_device_get_action$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle udev_device_get_action$MH = RuntimeHelper.downcallHandle(
        "udev_device_get_action",
        constants$1.udev_device_get_action$FUNC
    );
    static final FunctionDescriptor udev_device_get_sysattr_value$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle udev_device_get_sysattr_value$MH = RuntimeHelper.downcallHandle(
        "udev_device_get_sysattr_value",
        constants$1.udev_device_get_sysattr_value$FUNC
    );
    static final FunctionDescriptor udev_monitor_new_from_netlink$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle udev_monitor_new_from_netlink$MH = RuntimeHelper.downcallHandle(
        "udev_monitor_new_from_netlink",
        constants$1.udev_monitor_new_from_netlink$FUNC
    );
    static final FunctionDescriptor udev_monitor_enable_receiving$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle udev_monitor_enable_receiving$MH = RuntimeHelper.downcallHandle(
        "udev_monitor_enable_receiving",
        constants$1.udev_monitor_enable_receiving$FUNC
    );
    static final FunctionDescriptor udev_monitor_get_fd$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle udev_monitor_get_fd$MH = RuntimeHelper.downcallHandle(
        "udev_monitor_get_fd",
        constants$1.udev_monitor_get_fd$FUNC
    );
}


