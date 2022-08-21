//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Common configuration
//

#ifndef USBD_CONF_H
#define USBD_CONF_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "stm32f1xx.h"
#include "stm32f1xx_hal.h"

#ifdef __cplusplus
extern "C" {
#endif

#define USBD_MAX_NUM_INTERFACES 1
#define USBD_MAX_NUM_CONFIGURATION 1
#define USBD_MAX_STR_DESC_SIZ 512
#define USBD_SUPPORT_USER_STRING_DESC 1
#define USBD_DEBUG_LEVEL 0
#define USBD_SELF_POWERED 1
#define USBD_DFU_MAX_ITF_NUM 1
#define USBD_DFU_XFER_SIZE 1024
#define USBD_DFU_APP_DEFAULT_ADD 0x08000000

#define DEVICE_FS 0

#ifdef __cplusplus
}
#endif

#endif
