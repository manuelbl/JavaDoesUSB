//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include <vector>
#include "configuration.hpp"

/// <summary>
/// Parses a USB configuration descriptor.
/// </summary>
class config_parser {
public:
	config_parser();
	void parse(const std::vector<uint8_t>& config_desc);

	uint8_t configuration_value;

	std::vector<usb_interface> interfaces;
	std::vector<usb_composite_function> functions;

private:
	usb_interface* get_interface(int number);
	usb_composite_function* get_function(int intf_number);
};
