//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB driver for interfaces with vendor specific class.
// The interface can have any number of bulk and interrupt endpoints.
//

#pragma once

#include "common/tusb_common.h"
#include "device/usbd_pvt.h"

// --- Macro to create USB configuration descriptor

// Interface descriptor: interface number, number of endponts
#define CUSTOM_VENDOR_INTERFACE(_itfnum, _numeps) \
    /* Interface */\
    9, TUSB_DESC_INTERFACE, _itfnum, 0, _numeps, TUSB_CLASS_VENDOR_SPECIFIC, 0x00, 0x00, 0

// Interface descriptor: interface number, number of endponts
#define CUSTOM_VENDOR_INTERFACE_ALT(_itfnum, _altnum, _numeps) \
    /* Interface */\
    9, TUSB_DESC_INTERFACE, _itfnum, _altnum, _numeps, TUSB_CLASS_VENDOR_SPECIFIC, 0x00, 0x00, 0

// Bulk endpoint descriptor: endpoint address, packet size
#define CUSTOM_VENDOR_BULK_ENDPOINT(_epaddr, _packetsize) \
    /* Endpoint */\
    7, TUSB_DESC_ENDPOINT, _epaddr, TUSB_XFER_BULK, U16_TO_U8S_LE(_packetsize), 0

// Interrupt endpoint descriptor: endpoint address, packet size, interval
#define CUSTOM_VENDOR_INTERRUPT_ENDPOINT(_epaddr, _packetsize, _interval) \
    /* Endpoint */\
    7, TUSB_DESC_ENDPOINT, _epaddr, TUSB_XFER_INTERRUPT, U16_TO_U8S_LE(_packetsize), _interval


// --- Application API

/**
 * @brief Prepares to recieve data on an OUT endpoint.
 * 
 * The buffer must stay valid until the `cust_vendor_rx_cb()` callback
 * reports that data has been received.
 * 
 * If the OUT endpoint is already receiving data, this function does nothing.
 * 
 * @param ep_addr endpoint address (1 to 127)
 * @param buf pointer to buffer for received data
 * @param buf_len length of buffer
 */
void cust_vendor_prepare_recv(uint8_t ep_addr, void* buf, uint32_t buf_len);

/**
 * @brief Gets if the endpoint is busy receiving data.
 * 
 * @param ep_addr endpoint address (1 to 127)
 * @return true if the endpoint is busy receiving
 * @return false it the endpoint is idle
 */
bool cust_vendor_is_receiving(uint8_t ep_addr);

/**
 * @brief Transmits data on an IN endpoint.
 * 
 * The data must stay valid until the `cust_vendor_tx_cb()` callback
 * reports that the data has been transmitted.
 * 
 * If the IN endpoint is already transmitting data, this function does nothing.
 * 
 * @param ep_addr endpoint address (129 to 255)
 * @param data pointer to data to transmit
 * @param data_len number of bytes to transmit
 */
void cust_vendor_start_transmit(uint8_t ep_addr, void const * data, uint32_t data_len);

/**
 * @brief Gets if the endpoint is busy transmitting data.
 * 
 * @param ep_addr endpoint address (129 to 255)
 * @return true if the endpoint is busy transmitting
 * @return false it the endpoint is idle
 */
bool cust_vendor_is_transmitting(uint8_t ep_addr);


// --- Application Callback API

/**
 * @brief Invoked when new data has been received on an OUT endpoint.
 * 
 * @param ep_addr endpoint address (1 to 127)
 * @param recv_bytes number of received bytes
 */
TU_ATTR_WEAK void cust_vendor_rx_cb(uint8_t ep_addr, uint32_t recv_bytes);

/**
 * @brief Invoked when data has been transmitted on an IN endpoint.
 * 
 * @param ep_addr endpoint address (129 to 255)
 * @param sent_bytes number of sent bytes
 */
TU_ATTR_WEAK void cust_vendor_tx_cb(uint8_t ep_addr, uint32_t sent_bytes);

/**
 * @brief Invoked when an interface of this class has been opened.
 * 
 * This function is called as part of a SET CONFIGURATION control request.
 * 
 * @param intf interface number
 */
TU_ATTR_WEAK void cust_vendor_intf_open_cb(uint8_t intf);

/**
 * @brief Invoked when an alternate interface has been selected.
 * 
 * This function is called as part of a SET INTERFACE control request.
 * 
 * @param intf interface number
 * @param alt alternate interface number
 */
TU_ATTR_WEAK void cust_vendor_alt_intf_selected_cb(uint8_t intf, uint8_t alt);

/**
 * @brief Invoked when an endpoint's halt condition has been cleared.
 * 
 * This function is called as part of a SET FEATURE control request.
 * 
 * @param ep_addr endpoint address
 */
TU_ATTR_WEAK void cust_vendor_halt_cleared_cb(uint8_t ep_addr);


// --- Driver to be registered in usbd_app_driver_get_cb()

const usbd_class_driver_t cust_vendor_driver;
