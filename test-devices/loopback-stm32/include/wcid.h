//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Microsoft WCID descriptors
//

#pragma once

#include <libopencm3/usb/usbd.h>

// Register control request handlers for Microsoft WCID descriptors
void register_wcid_desc(usbd_device *usb_dev);
