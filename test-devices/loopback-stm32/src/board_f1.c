//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Board specific code for STM32F1 family
//

#if defined(STM32F1)

#include <stdbool.h>
#include "stm32f1xx.h"
#include "device/usbd.h"

#define EXTI_USBWakeUp_Line EXTI_IMR_IM18


extern uint32_t SystemCoreClock;
void SystemCoreClockUpdate(void);


static inline uint32_t get_reg(__I uint32_t* reg, uint32_t mask) {
	return *reg & mask;
}

static inline void set_reg(__IO uint32_t* reg, uint32_t value, uint32_t mask) {
	*reg = (*reg & ~mask) | (value & mask);
}


// --- additional RCC constants

#define RCC_CFGR_PLLSRC_HSI (0 << RCC_CFGR_PLLSRC_Pos)
#define RCC_CFGR_PLLSRC_HSE (1 << RCC_CFGR_PLLSRC_Pos)

// --- additional SysTick constants

#define SysTick_CTRL_CLKSOURCE_AHB_DIV8 (0 << SysTick_CTRL_CLKSOURCE_Pos)
#define SysTick_CTRL_CLKSOURCE_AHB      (1 << SysTick_CTRL_CLKSOURCE_Pos)

// --- additional GPIO constants

#define GPIO_CNF_INPUT_ANALOG 0
#define GPIO_CNF_INPUT_FLOAT 1
#define GPIO_CNF_INPUT_PUPD 2
#define GPIO_CNF_OUTPUT_PUSH_PULL 0
#define GPIO_CNF_OUTPUT_OPEN_DRAIN 1
#define GPIO_CNF_OUTPUT_ALT_PUSH_PULL 2
#define GPIO_CNG_OUTPUT_ALT_OPEN_DRAIN 3

#define GPIO_MODE_INPUT 0
#define GPIO_MODE_OUTPUT_10_MHZ 1
#define GPIO_MODE_OUTPUT_2_MHZ 2
#define GPIO_MODE_OUTPUT_50_MHZ 3


static inline void rcc_wait_for_osc_ready(uint32_t rcc_cr_clk_rdy) {
	while (get_reg(&RCC->CR, rcc_cr_clk_rdy) == 0)
		;
}


static void gpio_set_mode(GPIO_TypeDef* gpioport, int gpio, uint8_t mode, uint8_t cnf) {

	int offset;
	__IO uint32_t* reg;
	if (gpio < 8) {
		offset = 4 * gpio;
		reg = &gpioport->CRL;
	} else {
		offset = 4 * (gpio - 8);
		reg = &gpioport->CRH;
	}

	set_reg(reg, ((cnf << 2) | mode) << offset, 0xf << offset);
}

static inline void gpio_set(GPIO_TypeDef* gpioport, int gpio) {
	gpioport->BSRR = 1 << gpio;
}

static inline void gpio_clear(GPIO_TypeDef* gpioport, int gpio) {
	gpioport->BSRR = 1 << (gpio + 16);
}


static void rcc_clock_setup_in_hse_8mhz_out_72mhz(void) {

	// Enable internal high-speed oscillator
    set_reg(&RCC->CR, RCC_CR_HSION, RCC_CR_HSION_Msk);
	rcc_wait_for_osc_ready(RCC_CR_HSIRDY);

	// Select HSI as SYSCLK source
	set_reg(&RCC->CFGR, RCC_CFGR_SW_HSI, RCC_CFGR_SW_Msk);

	// Enable external high-speed oscillator 8MHz
	set_reg(&RCC->CR, RCC_CR_HSEON, RCC_CR_HSEON_Msk);
	rcc_wait_for_osc_ready(RCC_CR_HSERDY);
	set_reg(&RCC->CFGR, RCC_CFGR_SW_HSE, RCC_CFGR_SW_Msk);

	// Set prescalers for AHB, ADC, APB1, APB2
	set_reg(&RCC->CFGR, RCC_CFGR_HPRE_DIV1 | RCC_CFGR_ADCPRE_DIV8 | RCC_CFGR_PPRE1_DIV2 | RCC_CFGR_PPRE2_DIV1,
			RCC_CFGR_HPRE_Msk | RCC_CFGR_ADCPRE_Msk | RCC_CFGR_PPRE1_Msk | RCC_CFGR_PPRE2_Msk);

	// System clock of 72 MHz requires 2 wait states
	set_reg(&FLASH->ACR, FLASH_ACR_LATENCY_2, FLASH_ACR_LATENCY_Msk);

	// PLL multiplier 9 (for 72 MHz), HSE as PLL source, no clock predevision
	set_reg(&RCC->CFGR, RCC_CFGR_PLLMULL9 | RCC_CFGR_PLLSRC_HSE | RCC_CFGR_PLLXTPRE_HSE,
			RCC_CFGR_PLLMULL_Msk | RCC_CFGR_PLLSRC_Msk | RCC_CFGR_PLLXTPRE_Msk);

	// Enable PLL oscillator and wait for it to stabilize
	set_reg(&RCC->CR, RCC_CR_PLLON, RCC_CR_PLLON_Msk);
	rcc_wait_for_osc_ready(RCC_CR_PLLRDY);

	// Select PLL as SYSCLK source
	set_reg(&RCC->CFGR, RCC_CFGR_SW_PLL, RCC_CFGR_SW_Msk);

	// Update the SystemCoreClock variable used by TinyUSB
	SystemCoreClockUpdate();
}

static volatile uint32_t millis_count;

static void systick_init(void) {

    // Initialize SysTick
    set_reg(&SysTick->CTRL, SysTick_CTRL_CLKSOURCE_AHB_DIV8, SysTick_CTRL_CLKSOURCE_Msk);
    SysTick->LOAD = SystemCoreClock / 8 / 1000 - 1;

    // Enable and start
    set_reg(&SysTick->CTRL, SysTick_CTRL_TICKINT_Msk | SysTick_CTRL_ENABLE_Msk,
			SysTick_CTRL_TICKINT_Msk | SysTick_CTRL_ENABLE_Msk);
}


// --- Serial number ---

char board_serial_num[13];

const static char HEX_DIGITS[] = "0123456789ABCDEF";

void put_hex(uint32_t value, char *buf, int len) {
    for (int idx = 0; idx < len; idx++) {
        buf[idx] = HEX_DIGITS[value >> 28];
        value = value << 4;
    }
}

void usb_init_serial_num() {
	__I uint32_t* unique_id =(__I uint32_t*) UID_BASE;
    uint32_t id0 = unique_id[0];
    uint32_t id1 = unique_id[1];
    uint32_t id2 = unique_id[2];

    id0 += id2;

    put_hex(id0, board_serial_num, 8);
    put_hex(id1, board_serial_num + 8, 4);
    board_serial_num[12] = 0;
}


// --- Exported board functions

void board_init(void) {

    rcc_clock_setup_in_hse_8mhz_out_72mhz();
    systick_init();

	// clock for GPIOA (USB pins)
    RCC->APB2ENR |= RCC_APB2ENR_IOPAEN_Msk;
	// clock for GPIOB (LED)
    RCC->APB2ENR |= RCC_APB2ENR_IOPBEN_Msk;
	// clock for USB
    RCC->APB1ENR |= RCC_APB1ENR_USBEN_Msk;

	// LED
    gpio_set_mode(GPIOB, 12, GPIO_MODE_OUTPUT_10_MHZ, GPIO_CNF_OUTPUT_PUSH_PULL);

	usb_init_serial_num();

	// Wake up event is only available as interrupt, not as an event.
	// See product errata sheet
	set_reg(&EXTI->RTSR, EXTI_USBWakeUp_Line, EXTI_USBWakeUp_Line);
	set_reg(&EXTI->IMR, EXTI_USBWakeUp_Line, EXTI_USBWakeUp_Line);
	NVIC_EnableIRQ(USBWakeUp_IRQn);
}

uint32_t board_millis(void) {
    return millis_count;
}

void board_led_write(bool on) {
    if (on)
        gpio_clear(GPIOB, 12);
    else
        gpio_set(GPIOB, 12);
}

void board_sleep(void) {

	// turn off LED
	board_led_write(false);

	// pause systick interrupts
	set_reg(&SysTick->CTRL, 0, SysTick_CTRL_TICKINT_Msk);

	// enter Stop mode when the CPU enters deep sleep
	set_reg(&PWR->CR, 0, PWR_CR_PDDS_Msk | PWR_CR_LPDS_Msk);

	set_reg(&SCB->SCR, SCB_SCR_SLEEPDEEP_Msk, SCB_SCR_SLEEPDEEP_Msk);

	// sleep until an interrupt occurs
	__WFI();

	// reset SLEEPDEEP bit
	set_reg(&SCB->SCR, 0, SCB_SCR_SLEEPDEEP_Msk);

	// after wakeup, re-enable PLL as clock source
    rcc_clock_setup_in_hse_8mhz_out_72mhz();

	// resume systick interrupts
	set_reg(&SysTick->CTRL, SysTick_CTRL_TICKINT_Msk, SysTick_CTRL_TICKINT_Msk);

	// turn on LED
	board_led_write(true);
}


// --- Interrupt handlers ---

void SysTick_Handler (void) {
	millis_count++;
}

void USBWakeUp_IRQHandler(void) {
	// clear interrupt
	EXTI->PR = EXTI_USBWakeUp_Line;
}

void USB_HP_IRQHandler(void) {
    tud_int_handler(0);
}

void USB_LP_IRQHandler(void) {
    tud_int_handler(0);
}

#endif
