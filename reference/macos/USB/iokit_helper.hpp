//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#pragma once

#include <string>
#include <IOKit/usb/IOUSBLib.h>
#include <IOKit/IOCFPlugIn.h>

class iokit_helper {
public:
    template<typename T> static T** get_interface(io_service_t service, CFUUIDRef plugin_type, CFUUIDRef interface_id);

    static std::string string_from_cfstring(CFStringRef str);
    static std::string ioreg_get_property_as_string(io_service_t service, CFStringRef property_name);
    static int ioreg_get_property_as_int(io_service_t service, CFStringRef property_name);
};

template<typename T> T** iokit_helper::get_interface(io_service_t service, CFUUIDRef plugin_type, CFUUIDRef interface_id) {
    
    // get plug-in interface
    IOCFPlugInInterface** plug = nullptr;
    SInt32 score = 0;
    kern_return_t kret = IOCreatePlugInInterfaceForService(service, plugin_type, kIOCFPlugInInterfaceID, &plug, &score);
    if (kret != kIOReturnSuccess)
        return nullptr;
    
    auto plug_guard = make_scope_exit([&plug]() { (*plug)->Release(plug); });

    // Get requested interface
    T** intf = nullptr;
    (*plug)->QueryInterface(plug, CFUUIDGetUUIDBytes(interface_id), (void**)&intf);
    return intf;
}

