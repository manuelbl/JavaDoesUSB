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
#define EP_CDC_COMM 0x83
#define EP_CDC_DATA_RX 0x02
#define EP_CDC_DATA_TX 0x81

#define EP_LOOPBACK_RX 0x01
#define EP_LOOPBACK_TX 0x82

#define MSOS_VENDOR_CODE 0x44

extern uint8_t const desc_ms_os_20[];

void usb_init_serial_num();
