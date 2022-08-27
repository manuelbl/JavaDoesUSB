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

#include "common.h"

#include <libopencm3/stm32/rcc.h>

static volatile uint32_t millis_count;

uint32_t millis() { return millis_count; }

void delay(uint32_t ms) {
    int32_t target_time = millis_count + ms;
    while (target_time - (int32_t)millis_count > 0)
        ;
}

void systick_init() {
    // Initialize SysTick
    systick_set_clocksource(STK_CSR_CLKSOURCE_AHB_DIV8);
    systick_set_reload(rcc_ahb_frequency / 8 / 1000 - 1);

    // Enable and start
    systick_interrupt_enable();
    systick_counter_enable();
}

// System tick timer interrupt handler
extern "C" void sys_tick_handler() { millis_count++; }
