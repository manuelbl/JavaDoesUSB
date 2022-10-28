//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "config_parser.hpp"
#include "usb_error.hpp"


// --- USB descriptor types

enum class usb_descriptor_type : uint8_t {
    configuration = 0x02,
    string = 0x03,
    interface = 0x04,
    endpoint = 0x05,
    interface_association = 0x0b
};


// --- USB configuration descriptor

#pragma pack(push, 1)

struct usb_config_desc {
    uint8_t  bLength;
    uint8_t  bDescriptorType;
    uint16_t wTotalLength;
    uint8_t  bNumInterfaces;
    uint8_t  bConfigurationValue;
    uint8_t  iConfiguration;
    uint8_t  bmAttributes;
    uint8_t  bMaxPower;
};

#pragma pack(pop)


// --- USB interface descriptor

#pragma pack(push, 1)

struct usb_interface_desc {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bInterfaceNumber;
    uint8_t bAlternateSetting;
    uint8_t bNumEndpoints;
    uint8_t bInterfaceClass;
    uint8_t bInterfaceSubClass;
    uint8_t bInterfaceProtocol;
    uint8_t iInterface;
};

#pragma pack(pop)


// --- USB interface association descriptor

#pragma pack(push, 1)

struct usb_interface_association_desc {
    uint8_t  bLength;
    uint8_t  bDescriptorType;
    uint8_t  bFirstInterface;
    uint8_t  bInterfaceCount;
    uint8_t  bFunctionClass;
    uint8_t  bFunctionSubClass;
    uint8_t  bFunctionProtocol;
    uint8_t  iFunction;
};

#pragma pack(pop)


// --- USB endpoint descriptor

#pragma pack(push, 1)

 struct usb_endpoint_desc {
     uint8_t bLength;
     uint8_t bDescriptorType;
     uint8_t bEndpointAddress;
     uint8_t bmAttributes;
     uint16_t wMaxPacketSize;
     uint8_t bInterval;
 };

#pragma pack(pop)


// --- Configuration parser

 static int peek_desc_length(const uint8_t* desc, int offset) { return desc[offset]; }
 static usb_descriptor_type peek_desc_type(const uint8_t* desc, int offset) { return static_cast<usb_descriptor_type>(desc[offset + 1]); }


 config_parser::config_parser()
     : configuration_value(0) { }

void config_parser::parse(const uint8_t* config_desc, int desc_len)
{
    const usb_config_desc* header = reinterpret_cast<const usb_config_desc*>(config_desc);
    if (desc_len <= sizeof(usb_config_desc)
        || header->bDescriptorType != static_cast<uint8_t>(usb_descriptor_type::configuration)
        || header->wTotalLength != desc_len)
        throw usb_error("Invalid configuration descriptor");

    configuration_value = header->bConfigurationValue;

    int offset = peek_desc_length(config_desc, 0);
    usb_alternate_interface* last_alternate = nullptr;

    while (offset + 2 < desc_len) {

        int len = peek_desc_length(config_desc, offset);
        usb_descriptor_type type = peek_desc_type(config_desc, offset);

        if (offset + len > desc_len)
            break;

        if (type == usb_descriptor_type::interface_association) {

            const usb_interface_association_desc* ia_desc = reinterpret_cast<const usb_interface_association_desc*>(config_desc + offset);
            functions.push_back(usb_composite_function(ia_desc->bFirstInterface, ia_desc->bInterfaceCount,
                ia_desc->bFunctionClass, ia_desc->bFunctionSubClass, ia_desc->bFunctionProtocol));
            last_alternate = nullptr;

        } else if (type == usb_descriptor_type::interface) {

            const usb_interface_desc* intf_desc = reinterpret_cast<const usb_interface_desc*>(config_desc + offset);
            int number = intf_desc->bInterfaceNumber;

            // If there is no interface with this number yet, it's a new interface
            // and not just an additional alternate interface.
            auto intf = get_interface(number);
            if (intf == nullptr) {
                interfaces.push_back(usb_interface(number));
                intf = &interfaces.back();
            }

            // add alternate interface
            last_alternate = intf->add_alternate(usb_alternate_interface(intf_desc->bAlternateSetting,
                intf_desc->bInterfaceClass, intf_desc->bInterfaceSubClass, intf_desc->bInterfaceProtocol));

            // If there is no function for this interface, there was not preceeding IAD.
            // So create a new function with a single interface.
            if (get_function(intf->number()) == nullptr)
                functions.push_back(usb_composite_function(intf->number(), 1, last_alternate->class_code(), last_alternate->subclass_code(), last_alternate->protocol_code()));

        } else if (type == usb_descriptor_type::endpoint) {

            if (last_alternate == nullptr)
                throw usb_error("invalid configuration descriptor");

            const usb_endpoint_desc* ep_desc = reinterpret_cast<const usb_endpoint_desc*>(config_desc + offset);
            last_alternate->add_endpoint(usb_endpoint(ep_desc->bEndpointAddress & 0x7f, static_cast<usb_direction>(ep_desc->bEndpointAddress & 0x80),
                static_cast<usb_transfer_type>(ep_desc->bmAttributes & 0x03), ep_desc->wMaxPacketSize));
        }

        offset += len;
    }

    if (offset != desc_len)
        throw usb_error("invalid configuration descriptor");
}

usb_interface* config_parser::get_interface(int number) {
    auto iter = std::find_if(interfaces.begin(), interfaces.end(), [number](const usb_interface& itf) { return itf.number() == number; });
    if (iter == interfaces.end())
        return nullptr;

    return &*iter;
}

usb_composite_function* config_parser::get_function(int intf_number) {
	auto iter = std::find_if(functions.begin(), functions.end(), [intf_number](const usb_composite_function& f) {
		return intf_number >= f.first_interface() && intf_number < f.first_interface() + f.num_interfaces();
	});
	if (iter == functions.end())
		return nullptr;

	return &*iter;
}
