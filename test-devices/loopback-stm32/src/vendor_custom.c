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
// Alternate interfaces are supported.
//

#include "tusb_option.h"

#if (CFG_TUD_ENABLED && CFG_VENDOR_ADVANCED)

#include "device/usbd.h"
#include "vendor_custom.h"


static void cv_init(void);
static void cv_reset(uint8_t rhport);
static uint16_t cv_open(uint8_t rhport, tusb_desc_interface_t const* desc_intf, uint16_t max_len);
static bool cv_control_xfer(uint8_t rhport, uint8_t stage, tusb_control_request_t const * request);
static bool cv_xfer_cb(uint8_t rhport, uint8_t ep_addr, xfer_result_t result, uint32_t xferred_bytes);
static uint16_t setup_endpoints(uint8_t rhport, tusb_desc_interface_t const* desc_intf, uint16_t max_len, uint8_t alt_num);
static void close_endpoints();

const usbd_class_driver_t cust_vendor_driver = {
    .init = cv_init,
    .reset = cv_reset,
    .open = cv_open,
    .control_xfer_cb = cv_control_xfer,
    .xfer_cb = cv_xfer_cb,
    .sof = NULL
};

// open endpoints
static uint8_t cv_eps_open[8];
static uint16_t cv_eps_packet_size[8];
static int cv_num_eps_open;

// current alternate setting
static uint8_t cv_alternate_setting;

// interface descriptor (length covers all alternate interfaces and endpoints)
static tusb_desc_interface_t const * cv_intf_desc;
static uint16_t cv_intf_desc_len;


void cv_init(void) {
    // nothing to do
}

void cv_reset(uint8_t rhport) {
    // nothing to do
}

// Open interface is the descriptor matches this class
uint16_t cv_open(uint8_t rhport, tusb_desc_interface_t const * desc_intf, uint16_t max_len) {

    int ret = setup_endpoints(rhport, desc_intf, max_len, 0);

    if (ret != 0)
        cust_vendor_intf_open_cb(desc_intf->bInterfaceNumber);

    return ret;
}

// Setup endpoints for the given alternate interface.
// Endpoints details are extracted from the configuration descriptor
// returns: number of processed bytes
uint16_t setup_endpoints(uint8_t rhport, tusb_desc_interface_t const* desc_intf, uint16_t max_len, uint8_t alt_num) {

    uint8_t const * p_desc = (uint8_t const *) desc_intf;
    uint8_t const * p_desc_end = p_desc + max_len;

    // process the interface descriptor including all alternate interface descriptors
    while (p_desc < p_desc_end) {

        // check for interface descriptor with class "vendor specific"
        tusb_desc_interface_t const * desc_if = (tusb_desc_interface_t const *) p_desc;
        if (desc_if->bDescriptorType != TUSB_DESC_INTERFACE || desc_if->bInterfaceClass != TUSB_CLASS_VENDOR_SPECIFIC
            || desc_if->bInterfaceNumber != desc_intf->bInterfaceNumber)
            break;
        
        uint8_t curr_alt_num = desc_if->bAlternateSetting;
        if (curr_alt_num == alt_num) {
            // desired alternate interface found
            close_endpoints();
            cv_alternate_setting = alt_num;
        }

        p_desc = tu_desc_next(p_desc);

        // iterate endpoints
        while (p_desc < p_desc_end) {
            tusb_desc_endpoint_t const * desc_ep = (tusb_desc_endpoint_t const *) p_desc;
            if (desc_ep->bDescriptorType != TUSB_DESC_ENDPOINT)
                break;
            
            // open endpoint if it is for selected alternate setting
            if (curr_alt_num == alt_num) {
                TU_ASSERT(usbd_edpt_open(rhport, desc_ep));

                // remember open endpoints
                cv_eps_open[cv_num_eps_open] = desc_ep->bEndpointAddress;
                cv_eps_packet_size[cv_num_eps_open] = desc_ep->wMaxPacketSize;
                cv_num_eps_open += 1;
            }
            
            p_desc = tu_desc_next(p_desc);
        }
    }

    uint16_t processed_bytes = p_desc - (uint8_t const *) desc_intf;

    if (processed_bytes > 0) {
        // remember interface descriptor
        cv_intf_desc = desc_intf;
        cv_intf_desc_len = processed_bytes;
    }

    return processed_bytes;
}

void close_endpoints() {

    uint8_t const rhport = BOARD_TUD_RHPORT;

    // close in reverse order
    while (cv_num_eps_open > 0) {
        cv_num_eps_open -= 1;
        usbd_edpt_close(rhport, cv_eps_open[cv_num_eps_open]);
    }
}

bool cv_control_xfer(uint8_t rhport, uint8_t stage, tusb_control_request_t const * request) {
    TU_VERIFY(TUSB_REQ_TYPE_STANDARD == request->bmRequestType_bit.type);

    if (request->bRequest == TUSB_REQ_GET_INTERFACE) {
        if (stage == CONTROL_STAGE_SETUP) {
            tud_control_xfer(rhport, request, &cv_alternate_setting, 1);
        }
        return true; // indicate that request has been handled

    } else if (request->bRequest == TUSB_REQ_SET_INTERFACE) {
        if (stage == CONTROL_STAGE_SETUP) {
            uint8_t alt_num = request->wValue;
            setup_endpoints(rhport, cv_intf_desc, cv_intf_desc_len, alt_num);
            if (cust_vendor_alt_intf_selected_cb != NULL)
                cust_vendor_alt_intf_selected_cb((uint8_t) request->wIndex, alt_num);
            tud_control_status(rhport, request);
        }
        return true; // indicate that request has been handled

    } else if (request->bRequest == TUSB_REQ_CLEAR_FEATURE
            && request->wValue == TUSB_REQ_FEATURE_EDPT_HALT
            && request->bmRequestType_bit.recipient == TUSB_REQ_RCPT_ENDPOINT
            && cust_vendor_halt_cleared_cb != NULL) {
        uint8_t const ep_addr = tu_u16_low(request->wIndex);
        cust_vendor_halt_cleared_cb(ep_addr);
        return true; // ignored by caller
    }

    return false;
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

    uint8_t const rhport = BOARD_TUD_RHPORT;

    if (usbd_edpt_busy(rhport, ep_addr))
        return;

    usbd_edpt_xfer(rhport, ep_addr, buf, buf_len);
}

void cust_vendor_prepare_recv_fifo(uint8_t ep_addr, tu_fifo_t * fifo, uint32_t buf_len) {
    uint8_t const rhport = BOARD_TUD_RHPORT;

    if (usbd_edpt_busy(rhport, ep_addr))
        return;

    usbd_edpt_xfer_fifo(rhport, ep_addr, fifo, buf_len);
}


void cust_vendor_start_transmit(uint8_t ep_addr, void const * data, uint32_t data_len) {

    uint8_t const rhport = BOARD_TUD_RHPORT;

    if (usbd_edpt_busy(rhport, ep_addr))
        return;

    usbd_edpt_xfer(rhport, ep_addr, (void*) data, data_len);
}

void cust_vendor_start_transmit_fifo(uint8_t ep_addr, tu_fifo_t * fifo, uint32_t data_len) {

    uint8_t const rhport = BOARD_TUD_RHPORT;

    if (usbd_edpt_busy(rhport, ep_addr))
        return;

    usbd_edpt_xfer_fifo(rhport, ep_addr, (void*) fifo, data_len);
}


bool cust_vendor_is_receiving(uint8_t ep_addr) {

    uint8_t const rhport = BOARD_TUD_RHPORT;

    return usbd_edpt_busy(rhport, ep_addr);
}

bool cust_vendor_is_transmitting(uint8_t ep_addr) {

    uint8_t const rhport = BOARD_TUD_RHPORT;

    return usbd_edpt_busy(rhport, ep_addr);
}

uint16_t cust_vendor_packet_size(uint8_t ep_addr) {
    for (int i = 0; i < cv_num_eps_open; i++)
        if (cv_eps_open[i] == ep_addr)
            return cv_eps_packet_size[i];
    return 1;
}

#endif
