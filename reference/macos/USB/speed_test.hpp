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

#include <chrono>
#include <mutex>

class speed_test {
public:
    speed_test(usb_device_ptr device, int ep_out, int ep_in);
    void run(int num_bytes);
    
private:
    void reset_buffers();
    bool transmit(int num_bytes);
    bool receive(int num_bytes);

    void start_measurement();
    void update_progress(int n);
    int stop_measurement();
    
    usb_device_ptr device_;
    int ep_out_;
    int ep_in_;
    
    std::chrono::time_point<std::chrono::high_resolution_clock> start_time;
    int processed_bytes;
    std::mutex progress_mutex;
};
