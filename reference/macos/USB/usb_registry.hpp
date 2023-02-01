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
    
    std::shared_ptr<usb_device> get_shared_ptr(usb_device* device);
    // Add event source to background thread for handling asynchronous IO completion
    void add_event_source(CFRunLoopSourceRef source);
    // Remove event source to background thread for handling asynchronous IO completion
    void remove_event_source(CFRunLoopSourceRef source);
    // Main function for background thread
    void async_io_run(CFRunLoopSourceRef first_source);

    std::vector<usb_device_ptr> devices;

    std::function<void(usb_device_ptr device)> on_connected_callback;
    std::function<void(usb_device_ptr device)> on_disconnected_callback;

    // monitoring thread, port, run loop, mutex, condition etc.
    std::thread monitor_thread;
    CFRunLoopRef monitor_run_loop;
    IONotificationPortRef notify_port;
    CFRunLoopSourceRef monitor_run_loop_source;
    std::mutex monitor_mutex;
    std::condition_variable monitor_condition;

    // async_io thread and run loop
    std::thread async_io_thread;
    volatile CFRunLoopRef async_io_run_loop;
    std::mutex async_io_mutex;
    std::condition_variable async_io_condition;

    io_iterator_t device_connected_iter;
    io_iterator_t device_disconnected_iter;
    
    bool is_device_list_ready;
    
    friend class usb_device;
};
