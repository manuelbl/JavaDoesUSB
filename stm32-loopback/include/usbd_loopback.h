//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Loopback interface
//

#ifndef USB_LOOPBACK_H
#define USB_LOOPBACK_H

#include "usbd_ioreq.h"

#define DATA_OUT_EP 0x01U
#define DATA_IN_EP 0x82U
#define DATA_PACKET_SIZE 64

#ifdef __cplusplus
extern "C" {
#endif

extern USBD_ClassTypeDef USBD_Vendor_Class;

void usbd_check(USBD_HandleTypeDef *pdev);

#ifdef __cplusplus
}
#endif

#endif
