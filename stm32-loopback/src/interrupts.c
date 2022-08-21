//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Interrupt handlers
//

#include "interrupts.h"

#include "main.h"

extern PCD_HandleTypeDef usb_pcd;

void SysTick_Handler(void) { HAL_IncTick(); }

void USB_LP_CAN1_RX0_IRQHandler(void) { HAL_PCD_IRQHandler(&usb_pcd); }

void NMI_Handler(void) {
    while (1) {
    }
}

void HardFault_Handler(void) {
    while (1) {
    }
}

void MemManage_Handler(void) {
    while (1) {
    }
}

void BusFault_Handler(void) {
    while (1) {
    }
}

void UsageFault_Handler(void) {
    while (1) {
    }
}

void SVC_Handler(void) {}

void DebugMon_Handler(void) {}

void PendSV_Handler(void) {}
