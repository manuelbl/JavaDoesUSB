//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Microsoft WCID descriptors
//

#include <libopencm3/usb/usbd.h>

#include <algorithm>

static usbd_request_return_codes msft_string_desc(usbd_device *usbd_dev, usb_setup_data *req, uint8_t **buf, uint16_t *len, usbd_control_complete_callback *complete);
static usbd_request_return_codes msft_feature_desc(usbd_device *usbd_dev, usb_setup_data *req, uint8_t **buf, uint16_t *len, usbd_control_complete_callback *complete);

// Registers additional control request handlers to implement
// See https://github.com/pbatard/libwdi/wiki/WCID-Devices
void register_wcid_desc(usbd_device *usb_dev) {
    usbd_register_control_callback(usb_dev,
                                   USB_REQ_TYPE_STANDARD | USB_REQ_TYPE_DEVICE, USB_REQ_TYPE_TYPE | USB_REQ_TYPE_RECIPIENT,
                                   msft_string_desc);
    usbd_register_control_callback(usb_dev,
                                   USB_REQ_TYPE_VENDOR, USB_REQ_TYPE_TYPE,
                                   msft_feature_desc);
}

#define WCID_VENDOR_CODE 0x37

// Microsoft WCID string descriptor (string index 0xee)
static const uint8_t msft_sig_desc[] = {
    0x12,                           // length = 18 bytes
    USB_DT_STRING,                  // descriptor type string
    'M', 0, 'S', 0, 'F', 0, 'T', 0, // 'M', 'S', 'F', 'T'
    '1', 0, '0', 0, '0', 0,         // '1', '0', '0'
    WCID_VENDOR_CODE,               // vendor code
    0                               // padding
};

// Microsoft WCID feature descriptor (index 0x0004)
static const uint8_t wcid_feature_desc[] = {
    0x28, 0x00, 0x00, 0x00,                         // length = 40 bytes
    0x00, 0x01,                                     // version 1.0 (in BCD)
    0x04, 0x00,                                     // compatibility descriptor index 0x0004
    0x01,                                           // number of sections
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,       // reserved (7 bytes)
    0x00,                                           // interface number 0
    0x01,                                           // reserved
    0x57, 0x49, 0x4E, 0x55, 0x53, 0x42, 0x00, 0x00, // Compatible ID "WINUSB\0\0"
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Subcompatible ID (unused)
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00              // reserved 6 bytes
};

usbd_request_return_codes msft_string_desc(__attribute__((unused)) usbd_device *usbd_dev, usb_setup_data *req,
                                           uint8_t **buf, uint16_t *len,
                                           __attribute__((unused)) usbd_control_complete_callback *complete) {
    // 0x03: descriptor type string
    // 0xee: Microsoft WCID string index
    if (req->bRequest == USB_REQ_GET_DESCRIPTOR && req->wValue == 0x03ee) {
        *buf = const_cast<uint8_t *>(reinterpret_cast<const uint8_t *>(&msft_sig_desc));
        *len = std::min(*len, (uint16_t)msft_sig_desc[0]);
        return USBD_REQ_HANDLED;
    }

    return USBD_REQ_NEXT_CALLBACK;
}

usbd_request_return_codes msft_feature_desc(__attribute__((unused)) usbd_device *usbd_dev, usb_setup_data *req,
                                            uint8_t **buf, uint16_t *len,
                                            __attribute__((unused)) usbd_control_complete_callback *complete) {
    // 0x0004: Microsoft WCID index for feature descriptor
    if (req->bRequest == WCID_VENDOR_CODE && req->wIndex == 0x0004) {
        *buf = const_cast<uint8_t *>(reinterpret_cast<const uint8_t *>(&wcid_feature_desc));
        *len = std::min(*len, (uint16_t)wcid_feature_desc[0]);
        return USBD_REQ_HANDLED;
    }

    return USBD_REQ_NEXT_CALLBACK;
}
