//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "speed_test.hpp"

#include "prng.hpp"
#include "usb_error.hpp"

#include <algorithm>
#include <iostream>
#include <iomanip>
#include <thread>


static constexpr uint32_t PRNG_INIT = 0x7b;


speed_test::speed_test(usb_device_ptr device, int ep_out, int ep_in)
: device_(device), ep_out_(ep_out), ep_in_(ep_in) { }

void speed_test::run(int num_bytes) {
    reset_buffers();
    start_measurement();

    std::thread sender(&speed_test::transmit, this, num_bytes / 2);
    bool successful = receive(num_bytes / 2);
    sender.join();

    if (successful)
        stop_measurement();
}

bool speed_test::transmit(int num_bytes) {
    
    prng seq(PRNG_INIT);
    
    std::vector<uint8_t> buf(2048);
    int pos = 0;
    
    while (num_bytes > 0) {
        int n = std::min(num_bytes, static_cast<int>(buf.size()));
        try {
            seq.fill(buf, n);
            device_->transfer_out(ep_out_, buf, n);
                        
        } catch (usb_error& error) {
            std::cerr << std::endl << "ERROR: " << error.what() << " (writing at pos " << pos << ")" << std::endl;
            return false;
        }

        num_bytes -= n;
        pos += n;
        update_progress(n);
    }
    
    return true;
}

bool speed_test::receive(int num_bytes) {
    
    prng seq(PRNG_INIT);
    int pos = 0;
    
    while (num_bytes > 0) {
        int n;
        try {
            std::vector<uint8_t> data = device_->transfer_in(ep_in_);
            n = std::min(num_bytes, static_cast<int>(data.size()));
            int p = seq.verify(data, n);
            if (p != -1) {
                std::cerr << std::endl << "Invalid data received at pos " << (pos + p) << std::endl;
                return false;
            }
            
        } catch (usb_error& error) {
            std::cerr << std::endl << "ERROR: " << error.what() << " (reading at pos " << pos << ")" << std::endl;
            return false;
        }

        num_bytes -= n;
        pos += n;
        
        update_progress(n);
    }
    
    return true;
}

void speed_test::reset_buffers() {
    usb_control_request request_set_value_no_data = { 0 };
    request_set_value_no_data.bmRequestType = usb_control_request::request_type(usb_request_type::direction_out,
        usb_request_type::type_vendor, usb_request_type::recipient_interface);
    request_set_value_no_data.bRequest = 0x04;
    request_set_value_no_data.wIndex = 0; // interface number
    device_->control_transfer(request_set_value_no_data);

}


// --- throughput measurement ---

void speed_test::start_measurement() {
    start_time = std::chrono::high_resolution_clock::now();
    processed_bytes = 0;
}

void speed_test::update_progress(int n) {
    std::lock_guard<std::mutex> lockGuard(progress_mutex);
    processed_bytes += n;
}

int speed_test::stop_measurement() {
    auto end_time = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> dur = end_time - start_time;
    double thr = processed_bytes / dur.count();
    
    std::cout << "Throughput: " << std::fixed << std::setprecision(1) << thr << " kByte/s" << std::endl;
    
    return (int)(thr * 1000);
}
