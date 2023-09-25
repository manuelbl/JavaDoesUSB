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

#define OPT_WINUSB_NONE 0
#define OPT_WINUSB_MSOS20 2

#ifndef CFG_WINUSB
#define CFG_WINUSB OPT_WINUSB_MSOS20
#endif


// interfaces
enum {
    INTF_CDC_COMM = 0,
    INTF_CDC_DATA,
    INTF_LOOPBACK_CTRL,
    INTF_LOOPBACK,
    INTF_NUM_TOTAL
};


#define INTR_MAX_PACKET_SIZE 16
#define BULK_MAX_PACKET_SIZE 64

// Endpoints
#define EP_CDC_COMM 0x83
#define EP_CDC_DATA_RX 0x02
#define EP_CDC_DATA_TX 0x81

#define EP_LOOPBACK_RX 0x01
#define EP_LOOPBACK_TX 0x82

#if CFG_WINUSB == OPT_WINUSB_MSOS20

#define MSOS_VENDOR_CODE 0x44
extern uint8_t const desc_ms_os_20[];

#endif

void usb_init_serial_num();
