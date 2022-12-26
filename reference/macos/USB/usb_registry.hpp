//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#pragma once

#include "usb_device.hpp"

#include <IOKit/IOKitLib.h>

#include <condition_variable>
#include <functional>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

/**
 * Registry of connected USB devices.
 */
class usb_registry {
public:
    /// Create a new instance
    usb_registry();
    ~usb_registry();
    
    /// Sets a function to be called when a new device is connected.
    void set_on_device_connected(std::function<void(usb_device_ptr device)> callback);
    /// Sets a function to be called when a device is disconnected
    void set_on_device_disconnected(std::function<void(usb_device_ptr device)> callback);

    /// Starts the registry
    void start();
    
    /// Indicates if the registry has been started
    bool isStarted() { return notify_port != nullptr; }
    
    /// Gets the currently connected devices.
    std::vector<std::shared_ptr<usb_device>> get_devices();

private:
    /// Method run in background to monitor USB devices
    void monitor();
    
    void device_connected(io_iterator_t iterator);
    void device_disconnected(io_iterator_t iterator);

    static void device_connected_f(void *refcon, io_iterator_t iterator);
    static void device_disconnected_f(void *refcon, io_iterator_t iterator);

    std::vector<usb_device_ptr> devices;

    std::function<void(usb_device_ptr device)> on_connected_callback;
    std::function<void(usb_device_ptr device)> on_disconnected_callback;

    std::thread monitor_thread;
    CFRunLoopRef run_loop;
    IONotificationPortRef notify_port;
    CFRunLoopSourceRef run_loop_source;
    io_iterator_t device_connected_iter;
    io_iterator_t device_disconnected_iter;
    
    bool is_device_list_ready;
    std::mutex monitor_mutex;
    std::condition_variable monitor_condition;
};
