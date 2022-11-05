//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "configuration.hpp"

// --- usb_endpoint

usb_endpoint::usb_endpoint(int number, usb_direction direction, usb_transfer_type transfer_type, int packet_size)
	: number_(number), direction_(direction), transfer_type_(transfer_type), packet_size_(packet_size) { }

usb_endpoint usb_endpoint::invalid(-1, usb_direction::out, usb_transfer_type::bulk, 0);


// --- usb_alternate_interface

usb_alternate_interface::usb_alternate_interface(int number, int class_code, int subclass_code, int protocol_code)
	: number_(number), class_code_(class_code), subclass_code_(subclass_code), protocol_code_(protocol_code) { }

void usb_alternate_interface::add_endpoint(usb_endpoint&& endpoint) {
	endpoints_.push_back(std::move(endpoint));
}


// --- usb_interface

usb_interface::usb_interface(int number)
	: number_(number), is_claimed_(false), alternate_index_(0) { }

void usb_interface::set_claimed(bool claimed) {
	is_claimed_ = claimed;
}

usb_alternate_interface* usb_interface::add_alternate(usb_alternate_interface&& alternate) {
	alternates_.push_back(std::move(alternate));
	return &alternates_.back();
}

void usb_interface::set_alternate(int index) {
	alternate_index_ = index;
}

usb_interface usb_interface::invalid(-1);


// --- usb_composite_function

usb_composite_function::usb_composite_function(int first_interface, int num_interfaces, int class_code, int subclass_code, int protocol_code)
	: first_interface_(first_interface), num_interfaces_(num_interfaces), class_code_(class_code), subclass_code_(subclass_code), protocol_code_(protocol_code) { }
