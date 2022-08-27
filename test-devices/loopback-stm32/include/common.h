//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Commmon functions
//

#pragma once

#include <libopencmsis/core_cm3.h>

/**
 * @brief Initializes systick services
 */
void systick_init();

/**
 * @brief Gets the time.
 *
 * @return number of milliseconds since a fixed time in the past
 */
uint32_t millis();

/**
 * @brief Delays execution (busy wait)
 * @param ms delay length, in milliseconds
 */
void delay(uint32_t ms);
