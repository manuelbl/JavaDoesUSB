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

#include <stdint.h>

#define INTR_MAX_PACKET_SIZE 16
#define BULK_MAX_PACKET_SIZE 64

// Endpoints
#define EP_LOOPBACK_RX 0x01
#define EP_LOOPBACK_TX 0x82
#define EP_ECHO_RX 0x03
#define EP_ECHO_TX 0x83

#define WCID_VENDOR_CODE 0x37

const uint8_t wcid_feature_desc[40];


void usb_init_serial_num();
