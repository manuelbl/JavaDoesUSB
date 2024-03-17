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

// FIFO buffer for loopback data
tu_fifo_t loopback_fifo;
uint8_t loopback_buffer[512];

// RX buffer for loopback
uint8_t loopback_rx_buffer[64];

static bool is_blinking = true;
static uint32_t led_on_until = 0;
static uint32_t blink_toogle_at = 0;
static bool is_blink_on = true;

static inline bool has_expired(uint32_t deadline, uint32_t now) {
    return (int32_t)(now - deadline) >= 0;
}

static void led_busy(void);
static void led_blinking_task(void);
static void cdc_task(void);
static void loopback_init(void);
static void loopback_check_rx(void);
static void loopback_check_tx(void);
static void reset_buffers(void);


int main(void) {

    board_init();
    loopback_init();

    // init device stack
    tud_init(BOARD_TUD_RHPORT);

    while (1) {
        tud_task();
        cdc_task();
        led_blinking_task();
    }

    return 0;
}

// reset device in predictable state
void reset_buffers(void) {
    tu_fifo_clear(&loopback_fifo);
}

// --- Loopback

void loopback_init(void) {
    tu_fifo_config(&loopback_fifo, loopback_buffer, sizeof(loopback_buffer), 1, false);
}

// Check if the next transmission should be started
void loopback_check_tx(void) {

    tu_fifo_buffer_info_t info;
    tu_fifo_get_read_info(&loopback_fifo, &info);

    if (info.len_lin > 0 && !cust_vendor_is_transmitting(EP_LOOPBACK_TX)) {
        int n = info.len_lin;
        if (n > 128)
            n = 128;
        
        cust_vendor_start_transmit(EP_LOOPBACK_TX, info.ptr_lin, n);
        led_busy();
    }
}

// Check if receiving should be started again
void loopback_check_rx(void) {

    int n = tu_fifo_remaining(&loopback_fifo);
    if (n >= sizeof(loopback_rx_buffer) && !cust_vendor_is_receiving(EP_LOOPBACK_RX))
        cust_vendor_prepare_recv(EP_LOOPBACK_RX, loopback_rx_buffer, sizeof(loopback_rx_buffer));
}

// --- CDC class

void cdc_task(void) {
    // echo all received data
    if (!tud_cdc_available())
        return;

    uint8_t buf[64];
    uint32_t n = tud_cdc_read(buf, sizeof(buf));

    tud_cdc_write(buf, n);
    tud_cdc_write_flush();
    led_busy();
}


// --- Vendor class callbacks

// Invoked when new data has been received
void cust_vendor_rx_cb(uint8_t ep_addr, uint32_t recv_bytes) {
    tu_fifo_write_n(&loopback_fifo, loopback_rx_buffer, recv_bytes);
    loopback_check_rx();
    loopback_check_tx();
    led_busy();
}

// Invoked when last tx transfer finished
void cust_vendor_tx_cb(uint8_t ep_addr, uint32_t sent_bytes) {
    // If buffer has been reset in the mean time,
    // we might not be able to advance it fully or at all.
    int max_advance = tu_fifo_count(&loopback_fifo);
    if (sent_bytes > max_advance)
        sent_bytes = max_advance;
    if (sent_bytes > 0)
        tu_fifo_advance_read_pointer(&loopback_fifo, sent_bytes);

    loopback_check_tx();
    loopback_check_rx();

    // check ZLP
    if ((sent_bytes & (BULK_MAX_PACKET_SIZE - 1)) == 0
            && !cust_vendor_is_transmitting(ep_addr))
        cust_vendor_start_transmit(EP_LOOPBACK_TX, NULL, 0);

    led_busy();
}

// Invoked when interface has been opened
void cust_vendor_intf_open_cb(uint8_t intf) {
    loopback_check_rx();
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

    switch (request->bmRequestType_bit.type) {
    case TUSB_REQ_TYPE_VENDOR:

        switch (request->bRequest) {

        case REQUEST_SAVE_VALUE:
            if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 0) {
                led_busy();
                // save value from wValue
                saved_value = request->wValue;
                return tud_control_status(rhport, request);
            }
            break;

        case REQUEST_SAVE_DATA:
            if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 4) {
                led_busy();
                // receive into `saved_value`
                return tud_control_xfer(rhport, request, &saved_value, 4);
            }
            break;

        case REQUEST_SEND_DATA:
            if (request->bmRequestType_bit.direction == TUSB_DIR_IN && request->wLength == 4) {
                led_busy();
                // transmit from `saved_value`
                return tud_control_xfer(rhport, request, &saved_value, 4);
            }
            break;

        case REQUEST_RESET_BUFFERS:
            if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 0) {
                led_busy();
                reset_buffers();
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

#if CFG_WINUSB == OPT_WINUSB_MSOS20
        case MSOS_VENDOR_CODE:
            if (request->wIndex == 7) {
                // Get Microsoft OS 2.0 compatible descriptor
                uint16_t total_len;
                memcpy(&total_len, desc_ms_os_20 + 8, 2);
                return tud_control_xfer(rhport, request, (uint8_t*) desc_ms_os_20, total_len);
            }
            break;
#endif

        default:
            break;
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
