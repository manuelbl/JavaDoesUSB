//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "tests.hpp"
#include "assertion.hpp"
#include "speed_test.hpp"

#include <iostream>
#include <random>

#undef min
#undef max


using random_ushort_engine = std::independent_bits_engine<
    std::default_random_engine, 16, unsigned short>;


void tests::run() {
    registry.start();
    
    for (auto& device : registry.get_devices()) {
        std::cout << "Present:      " << device->description() << std::endl;
    }

    registry.set_on_device_connected([this](auto device) { on_device_connected(device); });
    registry.set_on_device_disconnected([this](auto device) { on_device_disconnected(device); });

    for (auto& device : registry.get_devices())
        on_device(device);
    
    std::cout << "Press RETURN to quit" << std::endl;
    std::string line;
    std::getline(std::cin, line);
}

void tests::test_current_device() {
    try {
        std::cout << "Found test device" << std::endl;

        is_composite = test_device->product_id() == 0xcea0;
        loopback_intf = is_composite ? 3 : 0;
        loopback_ep_out = is_composite ? 1 : 1;
        loopback_ep_in = is_composite ? 2 : 2;

        test_device->open();
        test_device->claim_interface(loopback_intf);

        test_control_transfers();
        test_bulk_transfers();
        test_speed();

        test_device->release_interface(loopback_intf);
        test_device->close();

        std::cout << "Test completed" << std::endl;
    }
    catch (const std::exception& e) {
        std::cout << "Test failed: " << e.what() << std::endl;
    }
}

void tests::test_control_transfers() {
    usb_control_request request_set_value_no_data = { 0 };
    request_set_value_no_data.bmRequestType = usb_control_request::request_type(usb_request_type::direction_out,
        usb_request_type::type_vendor, usb_request_type::recipient_interface);
    request_set_value_no_data.bRequest = 0x01;
    request_set_value_no_data.wValue = 0x9a41;
    request_set_value_no_data.wIndex = loopback_intf;
    request_set_value_no_data.wLength = 0;
    test_device->control_transfer(request_set_value_no_data);
    
    usb_control_request request_get_data = { 0 };
    request_get_data.bmRequestType = usb_control_request::request_type(usb_request_type::direction_in,
        usb_request_type::type_vendor, usb_request_type::recipient_interface);
    request_get_data.bRequest = 0x03;
    request_get_data.wValue = 0;
    request_get_data.wIndex = loopback_intf;
    request_get_data.wLength = 4;
    auto data = test_device->control_transfer_in(request_get_data);
    std::vector<uint8_t> expected_data{ 0x41, 0x9a, 0x00, 0x00 };
    assert_equals(expected_data, data);

    std::vector<uint8_t> sent_value{ 0x83, 0x03, 0xda, 0x3d };
    usb_control_request request_set_value_data = { 0 };
    request_set_value_data.bmRequestType = usb_control_request::request_type(usb_request_type::direction_out,
        usb_request_type::type_vendor, usb_request_type::recipient_interface);
    request_set_value_data.bRequest = 0x02;
    request_set_value_data.wValue = 0;
    request_set_value_data.wIndex = loopback_intf;
    request_set_value_data.wLength = static_cast<uint16_t>(sent_value.size());
    test_device->control_transfer_out(request_set_value_data, sent_value);

    data = test_device->control_transfer_in(request_get_data);
    assert_equals(sent_value, data);

    test_control_transfer_intf(loopback_intf);

    if (is_composite) {
        test_device->claim_interface(2);
        test_control_transfer_intf(2);
        test_device->release_interface(2);
    }
}

void tests::test_control_transfer_intf(int intf_num) {
    usb_control_request request_get_intf_num = { 0 };
    request_get_intf_num.bmRequestType = usb_control_request::request_type(usb_request_type::direction_in,
        usb_request_type::type_vendor, usb_request_type::recipient_interface);
    request_get_intf_num.bRequest = 0x05;
    request_get_intf_num.wValue = 0;
    request_get_intf_num.wIndex = intf_num;
    request_get_intf_num.wLength = 1;
    auto data = test_device->control_transfer_in(request_get_intf_num);

    std::vector<uint8_t> expected_data{ (uint8_t)intf_num };
    assert_equals(expected_data, data);
}


void tests::test_bulk_transfers() {
    test_loopback(12);
    test_loopback(130);
    test_loopback(123709);
}

void tests::test_loopback(int num_bytes) {
    
    // generate random bytes
    auto random_data = random_bytes(num_bytes);

    // prepare vector for receiving data
    std::vector<uint8_t> rx_data;
    rx_data.reserve(num_bytes);
    
    // read in separate thread
    std::thread reader([this, &rx_data, num_bytes]() {
        size_t bytes_read = 0;
        while (bytes_read < num_bytes) {
            auto data = test_device->transfer_in(loopback_ep_in);
            rx_data.insert(rx_data.end(), data.begin(), data.end());
            bytes_read += data.size();
        }
    });
    
    // send rand bytes in chunks
    const int chunk_size = 100;
    int bytes_written = 0;
    while (bytes_written < num_bytes) {
        int size = std::min(chunk_size, num_bytes - bytes_written);
        std::vector<uint8_t> chunk = {random_data.begin() + bytes_written, random_data.begin() + bytes_written + size};
        test_device->transfer_out(loopback_ep_out, chunk);
        bytes_written += size;
    }
    
    // wait for reader
    reader.join();

    // check result
    assert_equals(random_data, rx_data);
}

void tests::test_speed() {
    int packet_size = test_device->get_endpoint(usb_direction::out, 1).packet_size();
    
    speed_test test(test_device, 1, 2);
    test.run(packet_size == 512 ? 20000000 : 2000000);
}

void tests::on_device(usb_device_ptr device) {
    if (is_test_device(device)) {
        test_device = device;
        test_current_device();
        test_device = nullptr;
    }
}

void tests::on_device_connected(usb_device_ptr device) {
    std::cout << "Connected:    " << device->description() << std::endl;
    on_device(device);
}

void tests::on_device_disconnected(usb_device_ptr device) {
    std::cout << "Disconnected: " << device->description() << std::endl;
}

std::vector<uint8_t> tests::random_bytes(int num) {
    random_ushort_engine rbe;
    std::vector<unsigned short> data((num + 1) / 2);
    std::generate(begin(data), end(data), std::ref(rbe));
    const uint8_t* p = reinterpret_cast<const uint8_t*>(data.data());
    return std::vector<uint8_t>(p, p + num);
}

bool tests::is_test_device(usb_device_ptr device) {
    return device->vendor_id() == 0xcafe
        && (device->product_id() == 0xceaf || device->product_id() == 0xcea0);
}
