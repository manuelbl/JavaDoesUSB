//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB BOS descriptor
//

#pragma once

#include "usb_bos.h"
#include <libopencm3/usb/usbd.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MSOS_VENDOR_CODE 0x44

extern const usb_bos_device_capability_desc* const bos_descs[1];
extern const usb_msos20_desc_set_header* msos_desc_set;

#ifdef __cplusplus
}
#endif 
