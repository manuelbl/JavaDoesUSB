//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB binary device object store (BOS)
//

#include "usb_bos.h"
#include <string.h>

static uint32_t build_descriptor(uint8_t* buf);
static enum usbd_request_return_codes on_bos_control_request(
    usbd_device* usbd_dev, struct usb_setup_data* req,
    uint8_t** buf, uint16_t* len, usbd_control_complete_callback* complete);

static inline int imin(int a, int b) { return a < b ? a : b; }
static inline int descriptor_type(uint16_t wValue) { return wValue >> 8; }
static inline int descriptor_index(uint16_t wValue) { return wValue & 0xFF; }

static const usb_bos_device_capability_desc* const* bos_descs;
static int num_bos_descs;
static const usb_msos20_desc_set_header* msos_desc_set;
static uint8_t msos_vendor_code;


void usb_dev_register_bos(usbd_device* device,
        const usb_bos_device_capability_desc* const * descs, int num_descs,
        const usb_msos20_desc_set_header* msos_set, uint8_t vendor_code) {

    bos_descs = descs;
    num_bos_descs = num_descs;
    msos_desc_set = msos_set;
    msos_vendor_code = vendor_code;
    
    usbd_register_control_callback(device, USB_REQ_TYPE_IN | USB_REQ_TYPE_DEVICE,
                                   USB_REQ_TYPE_DIRECTION | USB_REQ_TYPE_RECIPIENT, on_bos_control_request);
}

#define APPEND_TO_DESC(data_ptr, data_len) \
    memcpy(buf_end, data_ptr, data_len);   \
    buf_end += data_len;

uint32_t build_descriptor(uint8_t* buf) {
    uint8_t* buf_end = buf;

    usb_bos_desc root_desc = {
        .bLength = sizeof(usb_bos_desc),
        .bDescriptorType = USB_DT_BOS,
        .wTotalLength = 0,
        .bNumDeviceCaps = (uint8_t) num_bos_descs
    };

    APPEND_TO_DESC(&root_desc, sizeof(root_desc))

    for (int i = 0; i < num_bos_descs; i++) {
        APPEND_TO_DESC(bos_descs[i], bos_descs[i]->bLength)
    }

    uint32_t length = buf_end - buf;

    // use memcpy() as buffer might not be word aligned
    memcpy(buf + 2, &length, sizeof(uint16_t));

    return length;
}

#undef APPEND_TO_DESC

enum usbd_request_return_codes on_bos_control_request(
        __attribute__((unused)) usbd_device* dev, struct usb_setup_data* req, uint8_t** buf, uint16_t* len,
        __attribute__((unused)) usbd_control_complete_callback* complete) {

    if (req->bmRequestType == (USB_REQ_TYPE_IN | USB_REQ_TYPE_STANDARD | USB_REQ_TYPE_DEVICE)) {

        if (req->bRequest == USB_REQ_GET_DESCRIPTOR && descriptor_type(req->wValue) == USB_DT_BOS) {

            // USB BOS descriptor (incl. Microsoft OS 2.0 platform capability descriptor)
            if (descriptor_index(req->wValue) != 0)
                return USBD_REQ_NOTSUPP;
            *len = imin(*len, build_descriptor(*buf));
            return USBD_REQ_HANDLED;
        }

    } else if (req->bmRequestType == (USB_REQ_TYPE_IN | USB_REQ_TYPE_VENDOR | USB_REQ_TYPE_DEVICE)) {
        // Microsoft OS 2.0 descriptor
        if (req->bRequest == msos_vendor_code && req->wValue == 0 && req->wIndex == USB_MSOS20_CTRL_INDEX_DESC) {
            memcpy(*buf, msos_desc_set, msos_desc.wMSOSDescriptorSetTotalLength);
            *len = imin(*len, msos_desc.wMSOSDescriptorSetTotalLength);
            return USBD_REQ_HANDLED;
        }
    }

    return USBD_REQ_NEXT_CALLBACK;
}
