//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Board specific code for STM32F4 family
//

#if defined(STM32F4xx)

#include <stdbool.h>
#include "stm32f4xx.h"
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


// --- additional PWR constants

#define PWR_CR_VOS_SCALE3 (1 << PWR_CR_VOS_Pos)
#define PWR_CR_VOS_SCALE2 (2 << PWR_CR_VOS_Pos)
#define PWR_CR_VOS_SCALE1 (3 << PWR_CR_VOS_Pos)


// --- additional RCC constants

typedef struct rcc_clock_setup {
	uint8_t pllm;
	uint16_t plln;
	uint8_t pllp;
	uint8_t pllq;
	uint32_t pll_source;
	uint32_t flash_config;
	uint32_t hpre;
	uint32_t ppre1;
	uint32_t ppre2;
	uint32_t voltage_scale;
} rcc_clock_setup_t;

const rcc_clock_setup_t clock_setup_hse_value_out_84mhz_3v3 = {
	.pllm = HSE_VALUE / 1000000,
	.plln = 336,
	.pllp = 4,
	.pllq = 7,
	.pll_source = RCC_PLLCFGR_PLLSRC_HSE,
	.hpre = RCC_CFGR_HPRE_DIV1,
	.ppre1 = RCC_CFGR_PPRE1_DIV2,
	.ppre2 = RCC_CFGR_PPRE2_DIV1,
	.voltage_scale = PWR_CR_VOS_SCALE1,
	.flash_config = FLASH_ACR_DCEN | FLASH_ACR_ICEN | FLASH_ACR_LATENCY_2WS
};

// --- additional SysTick constants

#define SysTick_CTRL_CLKSOURCE_AHB_DIV8 (0 << SysTick_CTRL_CLKSOURCE_Pos)
#define SysTick_CTRL_CLKSOURCE_AHB      (1 << SysTick_CTRL_CLKSOURCE_Pos)

// --- additional GPIO constants

#define GPIO_PUPD_NO_PULL 0
#define GPIO_PUPD_PULL_UP 1
#define GPIO_PUPD_PULL_DOWN 2

#define GPIO_MODE_INPUT 0
#define GPIO_MODE_OUTPUT 1
#define GPIO_MODE_ALT 2
#define GPIO_MODE_ANALOG 3

#define GPIO_OSPEED_LOW 0
#define GPIO_OSPEED_MEDIUM 1
#define GPIO_OSPEED_FAST 2
#define GPIO_OSPEED_HIGH 3


// --- additional USB register
#define PCGCCTL ((volatile uint32_t *)((uint32_t)USB_OTG_FS + USB_OTG_PCGCCTL_BASE))



static inline void rcc_wait_for_osc_ready(uint32_t rcc_cr_clk_rdy) {
	while (get_reg(&RCC->CR, rcc_cr_clk_rdy) == 0)
		;
}

static void gpio_mode_setup(GPIO_TypeDef* gpioport, int gpio, uint8_t mode, uint8_t pull_up_down) {

	int offset = gpio * 2;
	set_reg(&gpioport->PUPDR, pull_up_down << offset, 3 << offset);
	set_reg(&gpioport->MODER, mode << offset, 3 << offset);
}

void gpio_set_af(GPIO_TypeDef* gpioport, int gpio, uint8_t alt_func_num) {

	int offset = 4 * gpio;
	__IO uint32_t* reg;
	if (offset < 32) {
		reg = gpioport->AFR;
	} else {
		reg = gpioport->AFR + 1;
		offset -= 32;
	}

	set_reg(reg, alt_func_num << offset, 0xf << offset);
}

static inline void gpio_set_ospeed(GPIO_TypeDef* gpioport, int gpio, uint8_t ospeed) {
	int offset = gpio * 2;
	set_reg(&gpioport->OSPEEDR, ospeed << offset, 3 << offset);
}

static inline void gpio_set(GPIO_TypeDef* gpioport, int gpio) {
	gpioport->BSRR = 1 << gpio;
}

static inline void gpio_clear(GPIO_TypeDef* gpioport, int gpio) {
	gpioport->BSRR = 1 << (gpio + 16);
}


static void rcc_clock_setup_pll(const rcc_clock_setup_t* setup) {

	// Enable internal high-speed oscillator (HSI)
	set_reg(&RCC->CR, RCC_CR_HSION, RCC_CR_HSION_Msk);
	rcc_wait_for_osc_ready(RCC_CR_HSIRDY);

	// Select HSI as SYSCLK source
	set_reg(&RCC->CFGR, RCC_CFGR_SW_HSI, RCC_CFGR_SW_Msk);

	// Enable external high-speed oscillator (HSE)
	if (setup->pll_source == RCC_PLLCFGR_PLLSRC_HSE) {
		set_reg(&RCC->CR, RCC_CR_HSEON, RCC_CR_HSEON_Msk);
		rcc_wait_for_osc_ready(RCC_CR_HSERDY);
	}

	// Set the VOS scale mode
	set_reg(&RCC->APB1ENR, RCC_APB1ENR_PWREN, RCC_APB1ENR_PWREN_Msk);
	set_reg(&PWR->CR, setup->voltage_scale, PWR_CR_VOS_Msk);

	// Set prescalers for AHB, APB1, APB2
	set_reg(&RCC->CFGR, setup->hpre | setup->ppre1 | setup->ppre2,
			RCC_CFGR_HPRE_Msk | RCC_CFGR_PPRE1_Msk | RCC_CFGR_PPRE2_Msk);

	// Disable PLL oscillator before changing its configuration
	set_reg(&RCC->CR, 0, RCC_CR_PLLON_Msk);

	// Configure the PLL oscillator
	int pllp_val = (setup->pllp >> 1) - 1;
	RCC->PLLCFGR = setup->pll_source
		| (setup->pllm << RCC_PLLCFGR_PLLM_Pos)
		| (setup->plln << RCC_PLLCFGR_PLLN_Pos)
		| (pllp_val << RCC_PLLCFGR_PLLP_Pos)
		| (setup->pllq << RCC_PLLCFGR_PLLQ_Pos);

	// Enable PLL oscillator and wait for it to stabilize
	set_reg(&RCC->CR, RCC_CR_PLLON, RCC_CR_PLLON_Msk);
	rcc_wait_for_osc_ready(RCC_CR_PLLRDY);

	// Configure flash settings
	set_reg(&FLASH->ACR, setup->flash_config, FLASH_ACR_DCEN_Msk | FLASH_ACR_ICEN_Msk | FLASH_ACR_LATENCY_Msk);

	// Select PLL as SYSCLK source
	set_reg(&RCC->CFGR, RCC_CFGR_SW_PLL, RCC_CFGR_SW_Msk);

	// Wait for PLL clock to be selected
	while (get_reg(&RCC->CFGR, RCC_CFGR_SWS_Msk) != RCC_CFGR_SWS_PLL)
		;

	// Disable internal high-speed oscillator
	if (setup->pll_source == RCC_PLLCFGR_PLLSRC_HSE)
		set_reg(&RCC->CR, 0, RCC_CR_HSION_Msk);

	// Update the SystemCoreClock variable used by TinyUSB
	SystemCoreClockUpdate();
}

static volatile uint32_t millis_count;

static void systick_init(void) {

    // Initialize SysTick
    SysTick->CTRL = (SysTick->CTRL & ~SysTick_CTRL_CLKSOURCE_Msk) | SysTick_CTRL_CLKSOURCE_AHB_DIV8;
    SysTick->LOAD = SystemCoreClock / 8 / 1000 - 1;

    // Enable and start
    SysTick->CTRL |= SysTick_CTRL_TICKINT_Msk;
    SysTick->CTRL |= SysTick_CTRL_ENABLE_Msk;
}


// --- Serial number ---

char board_serial_num[13];

const static char HEX_DIGITS[] = "0123456789ABCDEF";

static void put_hex(uint32_t value, char *buf, int len) {
    for (int idx = 0; idx < len; idx++) {
        buf[idx] = HEX_DIGITS[value >> 28];
        value = value << 4;
    }
}

static void usb_init_serial_num() {
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

	rcc_clock_setup_pll(&clock_setup_hse_value_out_84mhz_3v3);
    systick_init();

	// clock for GPIOA (USB pins)
	set_reg(&RCC->AHB1ENR, RCC_AHB1ENR_GPIOAEN, RCC_AHB1ENR_GPIOAEN_Msk);

	// Configure USB D+/D- pins
    gpio_mode_setup(GPIOA, 11, GPIO_MODE_ALT, GPIO_PUPD_NO_PULL);
    gpio_set_af(GPIOA, 11, 10);
	gpio_set_ospeed(GPIOA, 11, GPIO_OSPEED_HIGH);
    gpio_mode_setup(GPIOA, 12, GPIO_MODE_ALT, GPIO_PUPD_NO_PULL);
    gpio_set_af(GPIOA, 12, 10);
	gpio_set_ospeed(GPIOA, 12, GPIO_OSPEED_HIGH);

	// clock for USB
    set_reg(&RCC->AHB2ENR, RCC_AHB2ENR_OTGFSEN, RCC_AHB2ENR_OTGFSEN_Msk);

	// Disable VBUS sense
	set_reg(&USB_OTG_FS->GCCFG, USB_OTG_GCCFG_NOVBUSSENS,
		USB_OTG_GCCFG_NOVBUSSENS_Msk | USB_OTG_GCCFG_VBUSASEN_Msk | USB_OTG_GCCFG_VBUSBSEN_Msk);

	// clock for GPIOC (LED)
    set_reg(&RCC->AHB1ENR, RCC_AHB1ENR_GPIOCEN, RCC_AHB1ENR_GPIOCEN_Msk);

	// LED pin
	gpio_mode_setup(GPIOC, 13, GPIO_MODE_OUTPUT, GPIO_PUPD_NO_PULL);

	usb_init_serial_num();

	// enable USB wakeup interrupt
	EXTI->PR = EXTI_USBWakeUp_Line;
	EXTI->RTSR |= EXTI_USBWakeUp_Line;
	EXTI->IMR |= EXTI_USBWakeUp_Line;
    NVIC_SetPriority(OTG_FS_WKUP_IRQn, 0);
	NVIC_EnableIRQ(OTG_FS_WKUP_IRQn);
}

uint32_t board_millis(void) {
    return millis_count;
}

void board_led_write(bool on) {
    if (on)
        gpio_clear(GPIOC, 13);
    else
        gpio_set(GPIOC, 13);
}

void board_sleep(void) {

	// turn off LED
	board_led_write(false);

	// stop PCLK to USB
	set_reg(PCGCCTL, USB_OTG_PCGCCTL_STOPCLK, USB_OTG_PCGCCTL_STOPCLK_Msk);

	// pause systick interrupts
	set_reg(&SysTick->CTRL, 0, SysTick_CTRL_TICKINT_Msk);

	// enter stop mode when the CPU enters deep sleep
	set_reg(&PWR->CR, 0, PWR_CR_PDDS_Msk | PWR_CR_LPDS_Msk);

	// use deep sleep mode
	set_reg(&SCB->SCR, SCB_SCR_SLEEPDEEP_Msk, SCB_SCR_SLEEPDEEP_Msk);
	
    __WFI();

	// reset to regular sleep mode
	set_reg(&SCB->SCR, 0, SCB_SCR_SLEEPDEEP_Msk);

	// after wakeup, re-enable PLL as clock source
	rcc_clock_setup_pll(&clock_setup_hse_value_out_84mhz_3v3);

	// resume systick interrupts
	set_reg(&SysTick->CTRL, SysTick_CTRL_TICKINT_Msk, SysTick_CTRL_TICKINT_Msk);

	// restart PCLK to USB
	set_reg(PCGCCTL, 0, USB_OTG_PCGCCTL_STOPCLK_Msk);

	// turn on LED
	board_led_write(true);
}


// --- Interrupt handlers ---

void SysTick_Handler (void) {
	millis_count++;
}

void OTG_FS_IRQHandler(void) {
    tud_int_handler(0);
}

void OTG_FS_WKUP_IRQHandler(void) {
	// clear interrupt
	EXTI->PR = EXTI_USBWakeUp_Line;
}

#endif
