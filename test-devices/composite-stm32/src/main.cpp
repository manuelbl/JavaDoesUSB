//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Main program
//

#include <libopencm3/stm32/gpio.h>
#include <libopencm3/stm32/rcc.h>
#include <libopencm3/usb/cdc.h>
#include <libopencm3/usb/usbd.h>
#include <string.h>

#include <algorithm>

#include "circ_buf.h"
#include "common.h"
#include "usb_descriptor.h"
#include "usb_bos_desc.h"

#define USB_CDC_REQ_GET_LINE_CODING 0x21

static void on_usb_set_config(usbd_device *usbd_dev, uint16_t wValue);
static usbd_request_return_codes on_vendor_control_request(usbd_device *usbd_dev, usb_setup_data *req, uint8_t **buf,
                                                           uint16_t *len, usbd_control_complete_callback *complete);
static usbd_request_return_codes on_class_control_request(usbd_device *usbd_dev, usb_setup_data *req, uint8_t **buf,
                                                          uint16_t *len, usbd_control_complete_callback *complete);
static void on_usb_loopback_received(usbd_device *usbd_dev, uint8_t ep);
static void on_usb_loopback_transmitted(usbd_device *usbd_dev, uint8_t ep);
static void on_usb_serial_received(usbd_device *usbd_dev, uint8_t ep);
static void on_usb_serial_transmitted(usbd_device *usbd_dev, uint8_t ep);
static void on_usb_serial_notif_transmitted(usbd_device *usbd_dev, uint8_t ep);
static void on_usb_echo_received(usbd_device *usbd_dev, uint8_t ep);
static void on_usb_echo_transmitted(usbd_device *usbd_dev, uint8_t ep);
static void check_loopback_buffers();
static void check_serial_buffers();

// USB device instance
static usbd_device *usb_device;

// buffer for control requests
static uint8_t usbd_control_buffer[256];

// Circular buffer for loopback test endpoints
static circ_buf<1024> loopback_buffer;

// Minimum free space in circular buffer for requesting more packets
static constexpr int MIN_FREE_SPACE = 2 * BULK_MAX_PACKET_SIZE;

// indicates if loopback data is being transmitted
static bool is_loopback_tx = false;

// indicates if the loopback RX endpoint is forced to NAK to prevent receiving further data
static bool is_loopback_rx_nak = false;

// value that can be saved and retrieved with control requests
static uint32_t saved_value;

// echo message
static char echo_msg[INTR_MAX_PACKET_SIZE];

// echo message length
static int echo_msg_len;

// number of echos left to transmit (if > 1, RX endpointed is NAKed)
static int num_echos_left;

// Circular buffer for serial port
static circ_buf<1024> serial_buffer;

// indicates if serial data is being transmitted
static bool is_serial_tx = false;

// indicates if the serial RX endpoint is forced to NAK to prevent receiving further data
static bool is_serial_rx_nak = false;


void init() {
    // Enable required clocks
    rcc_clock_setup_in_hse_8mhz_out_72mhz();
    rcc_periph_clock_enable(RCC_GPIOA);
    rcc_periph_clock_enable(RCC_GPIOC);
    rcc_periph_clock_enable(RCC_AFIO);
    rcc_periph_clock_enable(RCC_SPI1);
    rcc_periph_clock_enable(RCC_USB);

    // Initialize systick services
    systick_init();
}

void usb_init() {
    // reset USB peripheral
    rcc_periph_reset_pulse(RST_USB);

    // Pull USB D+ (A12) low for 80ms to trigger device reenumeration
    gpio_set_mode(GPIOA, GPIO_MODE_OUTPUT_10_MHZ, GPIO_CNF_OUTPUT_PUSHPULL, GPIO12);
    gpio_clear(GPIOA, GPIO12);
    delay(80);

    usb_init_serial_num();

    // create USB device
    usb_device = usbd_init(&st_usbfs_v1_usb_driver, &usb_device_desc, usb_config_descs, usb_desc_strings,
                           sizeof(usb_desc_strings) / sizeof(usb_desc_strings[0]), usbd_control_buffer,
                           sizeof(usbd_control_buffer));

    // Set callback for config calls
    usbd_register_set_config_callback(usb_device, on_usb_set_config);
    usb_dev_register_bos(usb_device, bos_descs, sizeof(bos_descs) / sizeof(bos_descs[0]),
            msos_desc_set, MSOS_VENDOR_CODE);
}

// Called when the host connects to the device and selects a configuration
void on_usb_set_config(usbd_device *usbd_dev, __attribute__((unused)) uint16_t wValue) {
    usb_dev_register_bos(usb_device, bos_descs, sizeof(bos_descs) / sizeof(bos_descs[0]),
            msos_desc_set, MSOS_VENDOR_CODE);

    // register control request handler for vendor specific requests (used for test)
    usbd_register_control_callback(usbd_dev, USB_REQ_TYPE_VENDOR | USB_REQ_TYPE_INTERFACE,
                                   USB_REQ_TYPE_TYPE | USB_REQ_TYPE_RECIPIENT, on_vendor_control_request);

    // register control request handler for class specific requests (used for CDC ACM)
    usbd_register_control_callback(usbd_dev, USB_REQ_TYPE_CLASS | USB_REQ_TYPE_INTERFACE,
                                   USB_REQ_TYPE_TYPE | USB_REQ_TYPE_RECIPIENT, on_class_control_request);

    usbd_ep_setup(usbd_dev, EP_CDC_COMM, USB_ENDPOINT_ATTR_INTERRUPT, INTR_MAX_PACKET_SIZE,
                  on_usb_serial_notif_transmitted);
    usbd_ep_setup(usbd_dev, EP_CDC_DATA_RX, USB_ENDPOINT_ATTR_BULK, BULK_MAX_PACKET_SIZE, on_usb_serial_received);
    usbd_ep_setup(usbd_dev, EP_CDC_DATA_TX, USB_ENDPOINT_ATTR_BULK, BULK_MAX_PACKET_SIZE, on_usb_serial_transmitted);

    usbd_ep_setup(usbd_dev, EP_LOOPBACK_RX, USB_ENDPOINT_ATTR_BULK, BULK_MAX_PACKET_SIZE, on_usb_loopback_received);
    usbd_ep_setup(usbd_dev, EP_LOOPBACK_TX, USB_ENDPOINT_ATTR_BULK, BULK_MAX_PACKET_SIZE, on_usb_loopback_transmitted);
    usbd_ep_setup(usbd_dev, EP_ECHO_RX, USB_ENDPOINT_ATTR_INTERRUPT, INTR_MAX_PACKET_SIZE, on_usb_echo_received);
    usbd_ep_setup(usbd_dev, EP_ECHO_TX, USB_ENDPOINT_ATTR_INTERRUPT, INTR_MAX_PACKET_SIZE, on_usb_echo_transmitted);

    loopback_buffer.reset();
    is_loopback_tx = false;
    is_loopback_rx_nak = false;
    num_echos_left = 0;
    echo_msg_len = 0;
    saved_value = 0;
    serial_buffer.reset();
    is_serial_tx = false;
    is_serial_rx_nak = false;
}

// Called when loopback data has been received
void on_usb_loopback_received(usbd_device *usbd_dev, uint8_t ep) {
    // Retrieve USB data (has side effect of setting endpoint to VALID)
    uint8_t packet[BULK_MAX_PACKET_SIZE] __attribute__((aligned(4)));
    int len = usbd_ep_read_packet(usbd_dev, ep, packet, sizeof(packet));

    // copy data into circular buffer
    loopback_buffer.add_data(packet, len);
}

// Called when loopback data has been transmitted
void on_usb_loopback_transmitted(__attribute__((unused)) usbd_device *usbd_dev, __attribute__((unused)) uint8_t ep) {
    is_loopback_tx = false;
}

void check_loopback_buffers() {
    // If RX is stopped and there is sufficient space in the buffer, resume it
    if (is_loopback_rx_nak) {
        if (loopback_buffer.avail_size() >= MIN_FREE_SPACE) {
            usbd_ep_nak_set(usb_device, EP_LOOPBACK_RX, 0);
            is_loopback_rx_nak = false;
        }

        // If RX is enabled but the space in the buffer is low, stop it
    } else {
        // check if there is space for less than 2 packets
        if (loopback_buffer.avail_size() < MIN_FREE_SPACE) {
            // set endpoint from VALID to NAK
            usbd_ep_nak_set(usb_device, EP_LOOPBACK_RX, 1);
            is_loopback_rx_nak = true;
        }
    }

    // If no data is being transmitted and there is data in the buffer, transmit a packet
    if (!is_loopback_tx && loopback_buffer.data_size() >= 0) {
        uint8_t packet[BULK_MAX_PACKET_SIZE] __attribute__((aligned(4)));
        int len = loopback_buffer.get_data(packet, BULK_MAX_PACKET_SIZE);
        usbd_ep_write_packet(usb_device, EP_LOOPBACK_TX, packet, len);
        is_loopback_tx = true;
    }
}

// Called when echo data has been received
void on_usb_echo_received(usbd_device *usbd_dev, uint8_t ep) {
    // Retrieve USB data (has side effect of setting endpoint to VALID)
    echo_msg_len = usbd_ep_read_packet(usbd_dev, EP_ECHO_RX, echo_msg, sizeof(echo_msg));
    usbd_ep_nak_set(usbd_dev, ep, 1);

    usbd_ep_write_packet(usbd_dev, EP_ECHO_TX, echo_msg, echo_msg_len);
    num_echos_left = 2;
}

// Called when echo data has been transmitted
void on_usb_echo_transmitted(__attribute__((unused)) usbd_device *usbd_dev, __attribute__((unused)) uint8_t ep) {
    num_echos_left--;
    if (num_echos_left > 0) {
        usbd_ep_write_packet(usbd_dev, ep, echo_msg, echo_msg_len);
    } else {
        usbd_ep_nak_set(usbd_dev, EP_ECHO_RX, 0);
    }
}

// Called when serial data has been received
void on_usb_serial_received(usbd_device *usbd_dev, uint8_t ep) {
    // Retrieve USB data (has side effect of setting endpoint to VALID)
    uint8_t packet[BULK_MAX_PACKET_SIZE] __attribute__((aligned(4)));
    int len = usbd_ep_read_packet(usbd_dev, ep, packet, sizeof(packet));

    // copy data into circular buffer
    serial_buffer.add_data(packet, len);
}

// Called when serial data has been transmitted
void on_usb_serial_transmitted(__attribute__((unused)) usbd_device *usbd_dev, __attribute__((unused)) uint8_t ep) {
    is_serial_tx = false;
}

// Called when a serial state notification has been transmitted
void on_usb_serial_notif_transmitted(__attribute__((unused)) usbd_device *usbd_dev,
                                     __attribute__((unused)) uint8_t ep) {
    // not used
}

void check_serial_buffers() {
    // If RX is stopped and there is sufficient space in the buffer, resume it
    if (is_serial_rx_nak) {
        if (serial_buffer.avail_size() >= MIN_FREE_SPACE) {
            usbd_ep_nak_set(usb_device, EP_CDC_DATA_RX, 0);
            is_serial_rx_nak = false;
        }

        // If RX is enabled but the space in the buffer is low, stop it
    } else {
        // check if there is space for less than 2 packets
        if (serial_buffer.avail_size() < MIN_FREE_SPACE) {
            // set endpoint from VALID to NAK
            usbd_ep_nak_set(usb_device, EP_CDC_DATA_RX, 1);
            is_serial_rx_nak = true;
        }
    }

    // If no data is being transmitted and there is data in the buffer, transmit a packet
    if (!is_serial_tx && serial_buffer.data_size() >= 0) {
        uint8_t packet[BULK_MAX_PACKET_SIZE] __attribute__((aligned(4)));
        int len = serial_buffer.get_data(packet, BULK_MAX_PACKET_SIZE);
        usbd_ep_write_packet(usb_device, EP_CDC_DATA_TX, packet, len);
        is_serial_tx = true;
    }
}

usbd_request_return_codes on_vendor_control_request(__attribute__((unused)) usbd_device *usbd_dev, usb_setup_data *req,
                                                    uint8_t **buf, uint16_t *len,
                                                    __attribute__((unused)) usbd_control_complete_callback *complete) {
    switch (req->bRequest) {
        case 1:
            if (req->wIndex == 0 && req->wLength == 0) {
                saved_value = req->wValue;
                return USBD_REQ_HANDLED;
            } else {
                return USBD_REQ_NOTSUPP;
            }
            break;

        case 2:
            if (req->wIndex == 0 && req->wLength == 4) {
                uint32_t *value = reinterpret_cast<uint32_t *>(*buf);
                saved_value = *value;
                return USBD_REQ_HANDLED;
            } else {
                return USBD_REQ_NOTSUPP;
            }
            break;

        case 3:
            if (req->wIndex == 0) {
                uint8_t *value = reinterpret_cast<uint8_t *>(&saved_value);
                *len = std::min(*len, (uint16_t)4);
                memcpy(*buf, value, *len);
                return USBD_REQ_HANDLED;
            } else {
                return USBD_REQ_NOTSUPP;
            }
            break;

        default:;  // fall through
    }

    return USBD_REQ_NEXT_CALLBACK;
}

// Process ACM requests on control endpoint
usbd_request_return_codes on_class_control_request(__attribute__((unused)) usbd_device *usbd_dev, usb_setup_data *req,
                                                   uint8_t **buf, uint16_t *len,
                                                   __attribute__((unused)) usbd_control_complete_callback *complete) {
    switch (req->bRequest) {
        case USB_CDC_REQ_SET_LINE_CODING:
            if (*len < sizeof(struct usb_cdc_line_coding))
                return USBD_REQ_NOTSUPP;

            return USBD_REQ_HANDLED;

        case USB_CDC_REQ_GET_LINE_CODING: {
            if (*len < sizeof(struct usb_cdc_line_coding))
                return USBD_REQ_NOTSUPP;

            struct usb_cdc_line_coding *line_coding = (struct usb_cdc_line_coding *)*buf;
            line_coding->dwDTERate = 115200;
            line_coding->bDataBits = 8;
            line_coding->bParityType = 0;
            line_coding->bCharFormat = 0;

            *len = sizeof(struct usb_cdc_line_coding);
            return USBD_REQ_HANDLED;
        }

        case USB_CDC_REQ_SET_CONTROL_LINE_STATE:
            return USBD_REQ_HANDLED;
    }
    return USBD_REQ_NEXT_CALLBACK;
}

int main() {
    init();
    usb_init();

    while (true) {
        usbd_poll(usb_device);
        check_loopback_buffers();
        check_serial_buffers();
    }
}
