//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#include "usb_registry.hpp"
#include "usb_device.hpp"
#include "usb_error.hpp"
#include "iokit_helper.hpp"
#include "scope.hpp"

#include <algorithm>
#include <string>
#include <mutex>
#include <thread>

usb_registry::usb_registry()
: notify_port(nullptr), run_loop_source(nullptr),
    device_connected_iter(0), device_disconnected_iter(0),
    on_connected_callback(nullptr), on_disconnected_callback(nullptr),
    is_device_list_ready(false) {
}

usb_registry::~usb_registry() {
    if (device_connected_iter != 0) {
        IOObjectRelease(device_connected_iter);
        device_connected_iter = 0;
    }
    if (device_disconnected_iter != 0) {
        IOObjectRelease(device_disconnected_iter);
        device_disconnected_iter = 0;
    }
    
    CFRunLoopStop(run_loop);
    monitor_thread.join();
    
    if (notify_port != nullptr) {
        IONotificationPortDestroy(notify_port);
        notify_port = nullptr;
    }
}

std::vector<usb_device_ptr> usb_registry::get_devices() {
    return devices;
}

void usb_registry::set_on_device_connected(std::function<void(usb_device_ptr device)> callback) {
    on_connected_callback = callback;
}

void usb_registry::set_on_device_disconnected(std::function<void(usb_device_ptr device)> callback) {
    on_disconnected_callback = callback;
}

void usb_registry::start() {
    monitor_thread = std::thread(&usb_registry::monitor, this);
    
    std::unique_lock wait_lock(monitor_mutex);
    monitor_condition.wait(wait_lock, [this] { return is_device_list_ready; });
}

void usb_registry::monitor() {
    
    notify_port = IONotificationPortCreate(kIOMainPortDefault);
    run_loop_source = IONotificationPortGetRunLoopSource(notify_port);
    
    run_loop = CFRunLoopGetCurrent();
    CFRunLoopAddSource(run_loop, run_loop_source, kCFRunLoopDefaultMode);
    
    auto matching_dict = IOServiceMatching(kIOUSBDeviceClassName);  // Interested in instances of USB device

    // Now set up a notification to be called when a device is first matched by I/O Kit.
    // This method consumes the matchingDict reference.
    kern_return_t kr = IOServiceAddMatchingNotification(notify_port,
                                                        kIOFirstMatchNotification,  // notificationType
                                                        matching_dict,              // matching
                                                        device_connected_f,         // callback
                                                        this,                       // refCon
                                                        &device_connected_iter      // notification
                                                        );
    usb_error::check(kr, "IOServiceAddMatchingNotification failed");
    
    // iterate the already connected devices and activate notifications
    device_connected(device_connected_iter);
    
    matching_dict = IOServiceMatching(kIOUSBDeviceClassName);  // Interested in instances of USB device
    
    // Now set up a notification to be called when a device is disconnected.
    // This method consumes the matchingDict reference.
    kr = IOServiceAddMatchingNotification(notify_port,
                                                        kIOTerminatedNotification,  // notificationType
                                                        matching_dict,              // matching
                                                        device_disconnected_f,      // callback
                                                        this,                       // refCon
                                                        &device_disconnected_iter   // notification
                                                        );
    usb_error::check(kr, "IOServiceAddMatchingNotification failed");
    
    // iterate to activate notifications
    device_disconnected(device_disconnected_iter);
    
    is_device_list_ready = true;
    monitor_condition.notify_all();

    // start run loop
    CFRunLoopRun();
}

void usb_registry::device_connected_f(void *refcon, io_iterator_t iterator) {
    auto self = reinterpret_cast<usb_registry*>(refcon);
    self->device_connected(iterator);
}

void usb_registry::device_connected(io_iterator_t iterator) {
    // Walk the iterator
    io_service_t service;
    while ((service = IOIteratorNext(iterator)) != 0) {

        auto service_guard = make_scope_exit([service]() { IOObjectRelease(service); });

        // Test if the device has a client interface (otherwise it's likely a controller)
        IOUSBDeviceInterface** dev = iokit_helper::get_interface<IOUSBDeviceInterface>(service, kIOUSBDeviceUserClientTypeID, kIOUSBDeviceInterfaceID);
        if (dev == nullptr)
            continue;
        
        auto dev_guard = make_scope_exit([dev]() { (*dev)->Release(dev); });

        // Get VID and PID
        int vendor_id = iokit_helper::ioreg_get_property_as_int(service, CFSTR(kUSBVendorID));
        int product_id = iokit_helper::ioreg_get_property_as_int(service, CFSTR(kUSBProductID));
        if (vendor_id == 0 || product_id == 0)
            continue; // controller or similar

        // Get entry ID
        uint64_t entry_id = 0;
        kern_return_t kr = IORegistryEntryGetRegistryEntryID(service, &entry_id);
        if (kr != 0)
            continue; // ignore
        
        // Create new device
        std::shared_ptr<usb_device> device(new usb_device(service, dev, entry_id, vendor_id, product_id));
        devices.push_back(device);
        
        // Call callback function
        if (on_connected_callback != nullptr)
            on_connected_callback(device);
    }
}

void usb_registry::device_disconnected_f(void *refcon, io_iterator_t iterator) {
    auto self = reinterpret_cast<usb_registry*>(refcon);
    self->device_disconnected(iterator);
}

void usb_registry::device_disconnected(io_iterator_t iterator) {
    // Walk the iterator
    io_service_t service;
    while ((service = IOIteratorNext(iterator)) != 0) {
        
        auto service_guard = make_scope_exit([service]() { IOObjectRelease(service); });

        // Get entry ID
        uint64_t entry_id = 0;
        kern_return_t kr = IORegistryEntryGetRegistryEntryID(service, &entry_id);
        if (kr != 0)
            continue; // ignore
        
        // find device in device list
        auto it = std::find_if(devices.cbegin(), devices.cend(), [entry_id](auto device) { return device->entry_id() == entry_id; });
        if (it == devices.cend())
            continue; // not part of the device list
        
        // remove from device list
        usb_device_ptr device = *it;
        devices.erase(it);
        
        // call callback function
        if (on_disconnected_callback != nullptr)
            on_disconnected_callback(device);
    }
}
