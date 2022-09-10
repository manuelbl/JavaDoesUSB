//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB descriptor
//

#pragma once

#include <libopencm3/usb/usbd.h>

#define INTR_MAX_PACKET_SIZE 16
#define BULK_MAX_PACKET_SIZE 64

// Endpoints
#define EP_LOOPBACK_RX 0x01
#define EP_LOOPBACK_TX 0x82
#define EP_ECHO_RX 0x03
#define EP_ECHO_TX 0x83
#define EP_CDC_COMM 0x84
#define EP_CDC_DATA_RX 0x05
#define EP_CDC_DATA_TX 0x85

// USB descriptor string table
extern const char *const usb_desc_strings[4];
// USB device descriptor
extern const struct usb_device_descriptor usb_device_desc;
// USB device configurations
extern const struct usb_config_descriptor usb_config_descs[];

void usb_init_serial_num();
