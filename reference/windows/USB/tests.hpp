//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#pragma once

#include "usb_device.hpp"
#include "usb_registry.hpp"

class tests {
public:
    void run();
    
private:
    void test_current_device();
    void test_control_transfers();
    void test_bulk_transfers();
    
    void test_loopback(int num_bytes);

    void on_device(usb_device_ptr device);
    void on_device_connected(usb_device_ptr device);
    void on_device_disconnected(usb_device_ptr device);

    static std::vector<uint8_t> random_bytes(int num);
    static bool is_test_device(usb_device_ptr device);
    
    usb_device_ptr test_device;
    usb_registry registry;
};

