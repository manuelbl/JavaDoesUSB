//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Board specific function (HAL)
//

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "board.h"
#include "tusb.h"
#include "usb_descriptors.h"
#include "vendor_custom.h"

#if BOARD_TUD_MAX_SPEED == OPT_MODE_HIGH_SPEED
  #define BUFFER_SIZE 16384
#else
  #define BUFFER_SIZE 2048
#endif

// FIFO buffer for loopback data
tu_fifo_t loopback_fifo;
uint8_t loopback_buffer[BUFFER_SIZE] __attribute__ ((aligned(4)));
bool delay_loopback_reset = false;

uint16_t bulk_packet_size = 64;
const int num_rx_packets = 2;
const int num_tx_packets = 4;

// buffer for echoed packet
uint8_t echo_buffer[16];
int echo_buffer_len;
int num_echos;


static bool is_blinking = true;
static uint32_t led_on_until = 0;
static uint32_t blink_toogle_at = 0;
static bool is_blink_on = true;

static inline bool has_expired(uint32_t deadline, uint32_t now) {
    return (int32_t)(now - deadline) >= 0;
}

static void led_busy(void);
static void led_blinking_task(void);
static void loopback_init(void);
static void loopback_check_rx(void);
static void loopback_check_tx(void);
static void echo_update_state(void);
static void reset_buffers(void);


int main(void) {

    board_init();
    loopback_init();

    // init device stack
    tud_init(BOARD_TUD_RHPORT);

    while (1) {
        tud_task();
        led_blinking_task();
    }

    return 0;
}

// reset device in predictable state
void reset_buffers(void) {
    if (cust_vendor_is_transmitting(EP_LOOPBACK_TX)) {
        delay_loopback_reset = true;
    } else {
        tu_fifo_clear(&loopback_fifo);
    }

    num_echos = 0;
}

// --- Loopback

void loopback_init(void) {
    tu_fifo_config(&loopback_fifo, loopback_buffer, sizeof(loopback_buffer), 1, false);
}

// Check if the next transmission should be started
void loopback_check_tx(void) {

    if (delay_loopback_reset) {
        tu_fifo_clear(&loopback_fifo);
        delay_loopback_reset = false;
    }

    uint16_t n = tu_fifo_count(&loopback_fifo);

    if (n > 0 && !cust_vendor_is_transmitting(EP_LOOPBACK_TX)) {
        uint16_t max_size = num_tx_packets * bulk_packet_size;
        if (n > max_size)
            n = max_size;
        
        cust_vendor_start_transmit_fifo(EP_LOOPBACK_TX, &loopback_fifo, n);
        led_busy();
    }
}

// Check if receiving should be started again
void loopback_check_rx(void) {

    uint16_t n = tu_fifo_remaining(&loopback_fifo);
    if (n >= num_rx_packets * bulk_packet_size && !cust_vendor_is_receiving(EP_LOOPBACK_RX))
        cust_vendor_prepare_recv_fifo(EP_LOOPBACK_RX, &loopback_fifo, num_rx_packets * bulk_packet_size);
}


// --- Echo

void echo_update_state(void) {
    if (num_echos > 0) {
        cust_vendor_start_transmit(EP_ECHO_TX, echo_buffer, echo_buffer_len);
        led_busy();
    } else {
        cust_vendor_prepare_recv(EP_ECHO_RX, echo_buffer, sizeof(echo_buffer));
    }
}


// --- Vendor class callbacks

// Invoked when new data has been received
void cust_vendor_rx_cb(uint8_t ep_addr, uint32_t recv_bytes) {
    if (ep_addr == EP_LOOPBACK_RX) {
        loopback_check_rx();
        loopback_check_tx();

    } else if (ep_addr == EP_ECHO_RX) {
        num_echos = 2;
        echo_buffer_len = recv_bytes;
        echo_update_state();
    }
    led_busy();
}

// Invoked when last tx transfer finished
void cust_vendor_tx_cb(uint8_t ep_addr, uint32_t sent_bytes) {
    if (ep_addr == EP_LOOPBACK_TX) {
        loopback_check_tx();
        loopback_check_rx();

        // check ZLP
        if (sent_bytes > 0
                && (sent_bytes & (bulk_packet_size - 1)) == 0
                && !cust_vendor_is_transmitting(ep_addr)) {
            cust_vendor_start_transmit(EP_LOOPBACK_TX, NULL, 0);
            led_busy();
        }

    } else if (ep_addr == EP_ECHO_TX) {
        num_echos--;
        echo_update_state();
    }
}

// Invoked when interface has been opened
void cust_vendor_intf_open_cb(uint8_t intf) {
    bulk_packet_size = cust_vendor_packet_size(EP_LOOPBACK_RX);
    loopback_check_rx();
    echo_update_state();
    led_busy();
}

// Invoked when an alternate interface has been selected
void cust_vendor_alt_intf_selected_cb(uint8_t intf, uint8_t alt) {
    reset_buffers();
    bulk_packet_size = cust_vendor_packet_size(EP_LOOPBACK_RX);
    loopback_check_rx();
    if (alt == 0)
        echo_update_state();
    led_busy();
}

void cust_vendor_halt_cleared_cb(uint8_t ep_addr) {
    switch (ep_addr) {
        case EP_LOOPBACK_RX:
            loopback_check_rx();
            break;
        case EP_LOOPBACK_TX:
            loopback_check_tx();
            break;
        case EP_ECHO_RX:
            if (num_echos == 0)
                echo_update_state();
            break;
        case EP_ECHO_TX:
            if (num_echos > 0)
                echo_update_state();
            break;
        default:
            break;
    }
    led_busy();
}


// --- Control messages (see README)

#define REQUEST_SAVE_VALUE 0x01
#define REQUEST_SAVE_DATA 0x02
#define REQUEST_SEND_DATA 0x03
#define REQUEST_RESET_BUFFERS 0x04
#define REQUEST_GET_INTF_NUM 0x05

static uint32_t saved_value = 0;

bool tud_vendor_control_xfer_cb(uint8_t rhport, uint8_t stage, tusb_control_request_t const * request) {
    if (stage != CONTROL_STAGE_SETUP)
        return true; // nothing to do

    if (request->bmRequestType_bit.type != TUSB_REQ_TYPE_VENDOR)
        return false; // stall unknown request

    switch (request->bRequest) {

    case REQUEST_SAVE_VALUE:
        if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 0) {
            // save value from wValue
            saved_value = request->wValue;
            led_busy();
            return tud_control_status(rhport, request);
        }
        break;

    case REQUEST_SAVE_DATA:
        if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 4) {
            // receive into `saved_value`
            led_busy();
            return tud_control_xfer(rhport, request, &saved_value, 4);
        }
        break;

    case REQUEST_SEND_DATA:
        if (request->bmRequestType_bit.direction == TUSB_DIR_IN && request->wLength == 4) {
            // transmit from `saved_value`
            led_busy();
            return tud_control_xfer(rhport, request, &saved_value, 4);
        }
        break;

    case REQUEST_RESET_BUFFERS:
        if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 0) {
            reset_buffers();
            led_busy();
            return tud_control_status(rhport, request);
        }
        break;

    case REQUEST_GET_INTF_NUM:
        if (request->bmRequestType_bit.direction == TUSB_DIR_IN && request->wLength == 1) {
            uint8_t intf_num = request->wIndex & 0xff;
            if (intf_num < 4) {
                led_busy();
                // return inteface number
                return tud_control_xfer(rhport, request, &intf_num, 1);
            }
        }
        break;

    // Microsoft WCID descriptor (for automatic WinUSB installation)
    case WCID_VENDOR_CODE:
        if (request->bmRequestType_bit.direction == TUSB_DIR_IN && request->wIndex == 0x0004) {
            led_busy();
            // transmit WCID feature descriptor
            int len = sizeof(wcid_feature_desc);
            if (len >= request->wLength)
                len = request->wLength;
            return tud_control_xfer(rhport, request, (void*) wcid_feature_desc, len);
        }

    default:
        break;
    }

    // stall unknown request
    return false;
}


// --- Device callbacks

// Register additional driver
usbd_class_driver_t const* usbd_app_driver_get_cb(uint8_t* driver_count) {
    *driver_count = 1;
    return &cust_vendor_driver;
}


// Invoked when device is mounted
void tud_mount_cb(void) {
    is_blinking = false;
}

// Invoked when usb bus is suspended
// remote_wakeup_en: if host allow us to perform remote wakeup
// Within 7ms, device must draw an average of current less than 2.5 mA from bus
void tud_suspend_cb(bool remote_wakeup_en) {
    (void) remote_wakeup_en;
    board_sleep();
}


// --- LED blinking ---

void led_busy(void) {
    led_on_until = board_millis() + 100;
    board_led_write(true);
}

void led_blinking_task(void) {
    uint32_t now = board_millis();
    if (is_blinking) {
        if (has_expired(blink_toogle_at, now)) {
            is_blink_on = !is_blink_on;
            blink_toogle_at = now + 250;
        }
        board_led_write(is_blink_on && (now & 7) == 0);
        
    } else if (has_expired(led_on_until, now)) {
        board_led_write((now & 3) == 0);
    }
}
