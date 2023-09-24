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

class device_info_set;

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
    std::shared_ptr<usb_device> create_device_from_device_info(device_info_set& dev_info_set, std::wstring&& device_path, std::map<std::wstring, HANDLE>& hub_handles);
    std::shared_ptr<usb_device> create_device(std::wstring&& device_path, bool is_composite, HANDLE hub_handle, DWORD usb_port_num);

    static std::string get_string(HANDLE hub_handle, ULONG usb_port_num, int index);
    static std::vector<uint8_t> get_descriptor(HANDLE hub_handle, ULONG usb_port_num, uint16_t descriptor_type, int index, int language_id, int request_size = 0);
    static int extract_interface_number(const std::vector<std::wstring>& hardware_ids);

    void on_device_connected(const WCHAR* path);
    void on_device_disconnected(const WCHAR* path);

    bool handle_message(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
    static LRESULT handle_windows_message(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);

    std::vector<usb_device_ptr> devices;

    std::function<void(usb_device_ptr device)> on_connected_callback;
    std::function<void(usb_device_ptr device)> on_disconnected_callback;

    std::shared_ptr<usb_device> get_shared_ptr(usb_device* device);

    void async_io_run();
    void add_to_completion_port(HANDLE);
    void add_completion_handler(OVERLAPPED* overlapped, usb_io_callback* completion_handler);
    void remove_completion_handler(OVERLAPPED* overlapped);
    usb_io_callback* get_completion_handler(OVERLAPPED* overlapped);

    std::thread monitor_thread;
    
    bool is_device_list_ready;
    std::mutex monitor_mutex;
    std::condition_variable monitor_condition;
    DWORD monitor_thread_id_;
    HWND message_window;

    std::thread async_io_thread;
    std::mutex async_io_mutex;
    HANDLE async_io_completion_port;
    std::map<OVERLAPPED*, usb_io_callback*> async_io_completion_handlers;

    friend class usb_device;
    friend class usb_istreambuf;
    friend class usb_ostreambuf;
};
