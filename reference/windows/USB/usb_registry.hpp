//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include "usb_device.hpp"

#include <condition_variable>
#include <functional>
#include <map>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

#include <Windows.h>
#undef min
#undef max
#undef LowSpeed
#include <SetupAPI.h>

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
    
    /// Gets the currently connected devices.
    std::vector<std::shared_ptr<usb_device>> get_devices();

private:
    /// Method run in background to monitor USB devices
    void monitor();

    void detect_present_devices();
    std::map<int, std::wstring> enumerate_child_devices(const std::vector<std::wstring> child_ids);
    
    void on_device_connected(const WCHAR* path);
    void on_device_disconnected(const WCHAR* path);

    bool handle_message(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
    static LRESULT handle_windows_message(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
    std::shared_ptr<usb_device> create_device(HDEVINFO dev_info_set, SP_DEVINFO_DATA* dev_info);

    std::vector<usb_device_ptr> devices;

    std::function<void(usb_device_ptr device)> on_connected_callback;
    std::function<void(usb_device_ptr device)> on_disconnected_callback;

    std::shared_ptr<usb_device> get_shared_ptr(usb_device* device);

    void async_io_run();
    void add_to_completion_port(HANDLE);
    void add_completion_handler(OVERLAPPED* overlapped, std::function<void(DWORD result, DWORD num_bytes)>* completion_handler);
    void remove_completion_handler(OVERLAPPED* overlapped);
    std::function<void(DWORD result, DWORD num_bytes)>* get_completion_handler(OVERLAPPED* overlapped);

    std::thread monitor_thread;
    
    bool is_device_list_ready;
    std::mutex monitor_mutex;
    std::condition_variable monitor_condition;
    DWORD monitor_thread_id_;
    HWND message_window;

    std::thread async_io_thread;
    std::mutex async_io_mutex;
    HANDLE async_io_completion_port;
    std::map<OVERLAPPED*, std::function<void(DWORD result, DWORD num_bytes)>*> async_io_completion_handlers;

    friend class usb_device;
    friend class usb_istreambuf;
    friend class usb_ostreambuf;
};
