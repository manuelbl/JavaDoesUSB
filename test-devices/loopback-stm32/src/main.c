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

// buffer for echoed packet
uint8_t echo_buffer[16];
int num_echos;


// Blink durations
enum  {
    BLINK_NOT_MOUNTED = 250,
    BLINK_MOUNTED = 1000,
    BLINK_SUSPENDED = 2500,
};

static uint32_t blink_interval_ms = BLINK_NOT_MOUNTED;

static void led_blinking_task(void);
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
        led_blinking_task();
    }

    return 0;
}

// reset device in predictable state
void reset_buffers(void) {
    tu_fifo_clear(&loopback_fifo);
    num_echos = 0;
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
    }
}

// Check if receiving should be started again
void loopback_check_rx(void) {

    int n = tu_fifo_remaining(&loopback_fifo);
    if (n >= sizeof(loopback_rx_buffer) && !cust_vendor_is_receiving(EP_LOOPBACK_RX)) {

        cust_vendor_prepare_recv(EP_LOOPBACK_RX, loopback_rx_buffer, sizeof(loopback_rx_buffer));
    }
}


// --- Vendor class callbacks

// Invoked when new data has been received
void cust_vendor_rx_cb(uint8_t ep_addr, uint32_t recv_bytes) {
    if (ep_addr == EP_LOOPBACK_RX) {
        tu_fifo_write_n(&loopback_fifo, loopback_rx_buffer, recv_bytes);
        loopback_check_rx();
        loopback_check_tx();

    } else if (ep_addr == EP_ECHO_RX) {
        num_echos = 2;
        cust_vendor_start_transmit(EP_ECHO_TX, echo_buffer, recv_bytes);
    }
}

// Invoked when last tx transfer finished
void cust_vendor_tx_cb(uint8_t ep_addr, uint32_t sent_bytes) {
    if (ep_addr == EP_LOOPBACK_TX) {

        // If buffer has been reset in the mean time,
        // we might not be able to advance it fully or at all.
        int max_advance = tu_fifo_count(&loopback_fifo);
        if (sent_bytes > max_advance)
            sent_bytes = max_advance;
        if (sent_bytes > 0)
            tu_fifo_advance_read_pointer(&loopback_fifo, sent_bytes);

        loopback_check_tx();
        loopback_check_rx();

    } else if (ep_addr == EP_ECHO_TX) {
        num_echos--;
        if (num_echos > 0) {
            cust_vendor_start_transmit(EP_ECHO_TX, echo_buffer, sent_bytes);
        } else {
            cust_vendor_prepare_recv(EP_ECHO_RX, echo_buffer, sizeof(echo_buffer));
        }
    }
}

// Invoked when interface has been opened
void cust_vendor_intf_open_cb(uint8_t intf) {
    loopback_check_rx();
    cust_vendor_prepare_recv(EP_ECHO_RX, echo_buffer, sizeof(echo_buffer));
}



// --- Control messages (see README)

static uint32_t saved_value = 0;

bool tud_vendor_control_xfer_cb(uint8_t rhport, uint8_t stage, tusb_control_request_t const * request) {
    if (stage != CONTROL_STAGE_SETUP)
        return true; // nothing to do

    if (request->bmRequestType_bit.type != TUSB_REQ_TYPE_VENDOR)
        return false; // stall unknown request

    switch (request->bRequest) {

    case 0x01:
        if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 0) {
            // save value from wValue
            saved_value = request->wValue;
            return tud_control_status(rhport, request);
        }
        break;

    case 0x02:
        if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 4) {
            // receive into `saved_value`
            return tud_control_xfer(rhport, request, &saved_value, 4);
        }
        break;

    case 0x03:
        if (request->bmRequestType_bit.direction == TUSB_DIR_IN && request->wLength == 4) {
            // transmit from `saved_value`
            return tud_control_xfer(rhport, request, &saved_value, 4);
        }
        break;

    case 0x04:
        if (request->bmRequestType_bit.direction == TUSB_DIR_OUT && request->wLength == 0) {
            reset_buffers();
            return tud_control_status(rhport, request);
        }
        break;
    
    // Microsoft WCID descriptor (for automatic WinUSB installation)
    case WCID_VENDOR_CODE:
        if (request->bmRequestType_bit.direction == TUSB_DIR_IN && request->wIndex == 0x0004) {
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
    blink_interval_ms = BLINK_MOUNTED;
}

// Invoked when device is unmounted
void tud_umount_cb(void) {
    blink_interval_ms = BLINK_NOT_MOUNTED;
}

// Invoked when usb bus is suspended
// remote_wakeup_en: if host allow us to perform remote wakeup
// Within 7ms, device must draw an average of current less than 2.5 mA from bus
void tud_suspend_cb(bool remote_wakeup_en) {
    (void) remote_wakeup_en;
    blink_interval_ms = BLINK_SUSPENDED;
}

// Invoked when usb bus is resumed
void tud_resume_cb(void) {
    blink_interval_ms = BLINK_MOUNTED;
}

// --- LED blinking ---

void led_blinking_task(void) {
    static uint32_t start_ms = 0;
    static bool led_state = false;

    // Blink every interval ms
    if ( board_millis() - start_ms < blink_interval_ms)
        return; // not enough time
    start_ms += blink_interval_ms;

    board_led_write(led_state);
    led_state = 1 - led_state; // toggle
}
