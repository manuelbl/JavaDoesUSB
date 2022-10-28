//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include <cstdint>
#include <vector>

/// USB endpoint direction
enum class usb_direction : uint8_t {
	/// Direction OUT: host to device
	out = 0x00,
	/// Direction IN: device to host
	in = 0x80
};

/// USB endpoint transfer type
enum class usb_transfer_type : uint8_t {
	/// Control transfer
	control = 0x00,
	/// Isochronous transfer
	isochronous = 0x01,
	/// Bulk transfer
	bulk = 0x02,
	/// Interrupt transfer
	interrupt = 0x03
};

/// <summary>
/// USB endpoint
/// </summary>
struct usb_endpoint {
public:
	/// Endpoint number
	int number() const { return number_;  }
	/// Endpoint direction
	usb_direction direction() const { return direction_; }
	/// Endpoint transfer type
	usb_transfer_type transfer_type() const { return transfer_type_;  }
	/// Maximum packet size
	int packet_size() const { return packet_size_;  }

private:
	usb_endpoint(int number, usb_direction direction, usb_transfer_type transfer_type, int packet_size);

	int number_;
	usb_direction direction_;
	usb_transfer_type transfer_type_;
	int packet_size_;

	friend class config_parser;
};

/// <summary>
/// USB alternate interface
/// </summary>
struct usb_alternate_interface {
public:
	/// Alternate number
	int number() const { return number_; }
	/// Interface class code
	int class_code() const { return class_code_; }
	/// Interface subclass code
	int subclass_code() const { return subclass_code_; }
	/// Interface protocol code
	int protocol_code() const { return protocol_code_; }
	/// List of endpoints
	const std::vector<usb_endpoint>& endpoints() const { return endpoints_; }

private:
	usb_alternate_interface(int number, int class_code, int subclass_code, int protocol_code);
	void add_endpoint(usb_endpoint&& endpoint);

	int number_;
	int class_code_;
	int subclass_code_;
	int protocol_code_;
	std::vector<usb_endpoint> endpoints_;

	friend class config_parser;
};

/// <summary>
/// USB interface
/// </summary>
struct usb_interface {
public:
	/// Interface number
	int number() const { return number_; }
	/// Indicates if interface has been claimed
	bool is_claimed() const { return is_claimed_; }
	/// Currently selected alternate interface
	const usb_alternate_interface& alternate() const { return alternates_[alternate_index_]; }
	/// List of all alternate interfaces of this interfaces
	const std::vector<usb_alternate_interface>& alternates() const { return alternates_; }

private:
	usb_interface(int number);
	void set_claimed(bool claimed);
	usb_alternate_interface* add_alternate(usb_alternate_interface&& alternate);
	void set_alternate(int index);

	int number_;
	bool is_claimed_;
	int alternate_index_;
	std::vector<usb_alternate_interface> alternates_;

	friend class config_parser;
	friend class usb_device;
};

/// <summary>
/// USB composite function
/// <para>
/// For a composite USB device, the composite function describes a single function.
/// A compsite function consists of a single or multiple consecutive interfaces.
/// </para>
/// </summary>
struct usb_composite_function {
public:
	/// Number of first interface
	int first_interface() const { return first_interface_; }
	/// Number of interfaces
	int num_interfaces() const { return num_interfaces_; }
	/// Function class code
	int class_code() const { return class_code_; }
	/// Function subclass code
	int subclass_code() const { return subclass_code_; }
	/// Function protocol code
	int protocol_code() const { return protocol_code_; }

private:
	usb_composite_function(int first_interface, int num_interfaces, int class_code, int subclass_code, int protocol_code);

	int first_interface_;
	int num_interfaces_;
	int class_code_;
	int subclass_code_;
	int protocol_code_;

	friend class config_parser;
};