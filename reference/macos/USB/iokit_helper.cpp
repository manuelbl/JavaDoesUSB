//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#include "iokit_helper.hpp"
#include "scope.hpp"

#include <CoreFoundation/CoreFoundation.h>

std::string iokit_helper::string_from_cfstring(CFStringRef str) {
    const char* cstr = CFStringGetCStringPtr(str, kCFStringEncodingUTF8);
    if (cstr != nullptr)
        return std::string(cstr);

    CFIndex len = CFStringGetLength(str) * 3 + 1;
    std::string result(len, 0);
    CFStringGetCString(str, &result[0], len, kCFStringEncodingUTF8);
    len = strlen(result.c_str());
    result.resize(len);

    return result;
}

std::string iokit_helper::ioreg_get_property_as_string(io_service_t service, CFStringRef property_name) {
    CFTypeRef property = IORegistryEntryCreateCFProperty(service, property_name, nullptr, 0);
    if (property == nullptr)
        return std::string();
    
    auto property_guard = make_scope_exit([&property]() { CFRelease(property); });

    CFTypeID type = CFGetTypeID(property);
    if (type != CFStringGetTypeID())
        return std::string();
    
    return string_from_cfstring(static_cast<CFStringRef>(property));
}

int iokit_helper::ioreg_get_property_as_int(io_service_t service, CFStringRef property_name) {
    CFTypeRef property = IORegistryEntryCreateCFProperty(service, property_name, nullptr, 0);
    if (property == nullptr)
        return 0;

    auto property_guard = make_scope_exit([&property]() { CFRelease(property); });

    CFTypeID type = CFGetTypeID(property);
    if (type != CFNumberGetTypeID())
        return 0;
    
    int value = 0;
    CFNumberGetValue(static_cast<CFNumberRef>(property), kCFNumberSInt32Type, &value);
    return value;
}

int iokit_helper::get_ref_count(void* obj) {
    int* data = reinterpret_cast<int**>(obj)[1];
    return data[2];
}
