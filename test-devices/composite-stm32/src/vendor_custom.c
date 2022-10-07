//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB driver for interfaces with vendor specific class.
// The interface can have any number of Bulk and Interrupt endpoints.
//

#include "tusb_option.h"

#if (CFG_TUD_ENABLED && CFG_VENDOR_ADVANCED)

#include "device/usbd.h"
#include "vendor_custom.h"


static void cv_init(void);
static void cv_reset(uint8_t rhport);
static uint16_t cv_open(uint8_t rhport, tusb_desc_interface_t const* desc_intf, uint16_t max_len);
static bool cv_xfer_cb(uint8_t rhport, uint8_t ep_addr, xfer_result_t result, uint32_t xferred_bytes);

const usbd_class_driver_t cust_vendor_driver = {
    .init = cv_init,
    .reset = cv_reset,
    .open = cv_open,
    .control_xfer_cb = NULL,
    .xfer_cb = cv_xfer_cb,
    .sof = NULL
};


void cv_init(void) {
    // nothing to do
}

void cv_reset(uint8_t rhport) {
    // nothing to do
}

uint16_t cv_open(uint8_t rhport, tusb_desc_interface_t const * desc_intf, uint16_t max_len) {

    // return 0 if interface class is not "vendor specific"
    TU_VERIFY(TUSB_CLASS_VENDOR_SPECIFIC == desc_intf->bInterfaceClass, 0);

    uint8_t const * p_desc = tu_desc_next(desc_intf);
    uint8_t const * desc_end = p_desc + max_len;
    int num_endpoints = desc_intf->bNumEndpoints;

    // iterate all endpoints
    while (num_endpoints > 0 && p_desc < desc_end) {

        // open endpoint
        tusb_desc_endpoint_t const * desc_ep = (tusb_desc_endpoint_t const *) p_desc;
        TU_ASSERT(usbd_edpt_open(rhport, desc_ep));

        p_desc = tu_desc_next(p_desc);
        num_endpoints--;
    }

    uint16_t processed_bytes = p_desc - (uint8_t const *) desc_intf;

    cust_vendor_intf_open_cb(desc_intf->bInterfaceNumber);

    return processed_bytes;
}

bool cv_xfer_cb(uint8_t rhport, uint8_t ep_addr, xfer_result_t result, uint32_t xferred_bytes) {

    if (tu_edpt_dir(ep_addr) == TUSB_DIR_IN) {
        cust_vendor_tx_cb(ep_addr, xferred_bytes);

    } else {
        cust_vendor_rx_cb(ep_addr, xferred_bytes);
    }

    return true;
}

void cust_vendor_prepare_recv(uint8_t ep_addr, void* buf, uint32_t buf_len) {

    uint8_t const rhport = 0;

    if (usbd_edpt_busy(rhport, ep_addr))
        return;

    usbd_edpt_xfer(rhport, ep_addr, buf, buf_len);
}

void cust_vendor_start_transmit(uint8_t ep_addr, void const * data, uint32_t data_len) {

    uint8_t const rhport = 0;

    if (usbd_edpt_busy(rhport, ep_addr))
        return;

    usbd_edpt_xfer(rhport, ep_addr, (void*) data, data_len);
}

bool cust_vendor_is_receiving(uint8_t ep_addr) {
    return usbd_edpt_busy(0, ep_addr);
}

bool cust_vendor_is_transmitting(uint8_t ep_addr) {
    return usbd_edpt_busy(0, ep_addr);
}

#endif
