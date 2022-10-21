//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Board specific functions (HAL)
//

#pragma once

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif


// Initialize the board
void board_init(void);

// Set the LED on or off
void board_led_write(bool on);

// Return the number of milliseconds since a time in the past
uint32_t board_millis(void);

// USB serial number
extern char board_serial_num[13];


#ifdef __cplusplus
}
#endif
