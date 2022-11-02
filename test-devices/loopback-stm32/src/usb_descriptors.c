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

#include "usb_descriptors.h"

#include "board.h"
#include "tusb.h"
#include "vendor_custom.h"


// --- Device Descriptor ---

tusb_desc_device_t const desc_device = {
    .bLength = sizeof(tusb_desc_device_t),
    .bDescriptorType = TUSB_DESC_DEVICE,
    .bcdUSB = 0x0200,

    .bDeviceClass = TUSB_CLASS_VENDOR_SPECIFIC,
    .bDeviceSubClass = 0,
    .bDeviceProtocol = 0,
    .bMaxPacketSize0 = CFG_TUD_ENDPOINT0_SIZE,

    .idVendor = 0xCAFE,
    .idProduct = 0xCEAF,
    .bcdDevice = 0x0074,  // version 0.7.4

    .iManufacturer = 0x01,
    .iProduct = 0x02,
    .iSerialNumber = 0x03,

    .bNumConfigurations = 0x01
};

// Invoked when a GET DEVICE DESCRIPTOR request is received.
// Return a pointer to the descriptor.
uint8_t const* tud_descriptor_device_cb(void) {
    return (uint8_t const*)&desc_device;
}


// --- Configuration Descriptor ---

enum {
    INTF_LOOPBACK = 0,
    INTF_NUM_TOTAL
};

#define CONFIG_TOTAL_LEN (TUD_CONFIG_DESC_LEN + 9 + 4 * 7 + 9 + 2 * 7)

uint8_t const desc_fs_configuration[] = {
    // Config number, interface count, string index, total length, attribute, power in mA
    TUD_CONFIG_DESCRIPTOR(1, INTF_NUM_TOTAL, 0, CONFIG_TOTAL_LEN, 0x00, 500),
    // Loopback interface (alternate 0)
    CUSTOM_VENDOR_INTERFACE(0, 4),
    // Loopback endpoint OUT
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_RX, 64),
    // Loopback endpoint IN
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_TX, 64),
    // Echo endpoint OUT
    CUSTOM_VENDOR_INTERRUPT_ENDPOINT(EP_ECHO_RX, INTR_MAX_PACKET_SIZE, 16),
    // Echo endpoint IN
    CUSTOM_VENDOR_INTERRUPT_ENDPOINT(EP_ECHO_TX, INTR_MAX_PACKET_SIZE, 16),
    // Loopback interface (alternate 1)
    CUSTOM_VENDOR_INTERFACE_ALT(0, 1, 2),
    // Loopback endpoint OUT
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_RX, 64),
    // Loopback endpoint IN
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_TX, 64)
};

#if TUD_OPT_HIGH_SPEED

uint8_t const desc_hs_configuration[] = {
    // Config number, interface count, string index, total length, attribute, power in mA
    TUD_CONFIG_DESCRIPTOR(1, INTF_NUM_TOTAL, 0, CONFIG_TOTAL_LEN, 0x00, 500),
    // Loopback interface (alternate 0)
    CUSTOM_VENDOR_INTERFACE(0, 4),
    // Loopback endpoint OUT
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_RX, 512),
    // Loopback endpoint IN
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_TX, 512),
    // Echo endpoint OUT
    CUSTOM_VENDOR_INTERRUPT_ENDPOINT(EP_ECHO_RX, INTR_MAX_PACKET_SIZE, 8),
    // Echo endpoint IN
    CUSTOM_VENDOR_INTERRUPT_ENDPOINT(EP_ECHO_TX, INTR_MAX_PACKET_SIZE, 8),
    // Loopback interface (alternate 1)
    CUSTOM_VENDOR_INTERFACE_ALT(0, 1, 2),
    // Loopback endpoint OUT
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_RX, 512),
    // Loopback endpoint IN
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_TX, 512)
};

// device qualifier (same values as device descriptor where possible)
tusb_desc_device_qualifier_t const desc_device_qualifier =
{
    .bLength = sizeof(tusb_desc_device_qualifier_t),
    .bDescriptorType = TUSB_DESC_DEVICE,
    .bcdUSB = 0x0200,

    .bDeviceClass = TUSB_CLASS_VENDOR_SPECIFIC,
    .bDeviceSubClass = 0,
    .bDeviceProtocol = 0,

    .bMaxPacketSize0 = CFG_TUD_ENDPOINT0_SIZE,
    .bNumConfigurations = 0x01,
    .bReserved  = 0x00
};

// Invoked when received GET DEVICE QUALIFIER DESCRIPTOR request
// Application return pointer to descriptor, whose contents must exist long enough for transfer to complete.
// device_qualifier descriptor describes information about a high-speed capable device that would
// change if the device were operating at the other speed. If not highspeed capable stall this request.
uint8_t const* tud_descriptor_device_qualifier_cb(void)
{
    return (uint8_t const*) &desc_device_qualifier;
}

// Invoked when received GET OTHER SPEED CONFIGURATION DESCRIPTOR request
// Application return pointer to descriptor, whose contents must exist long enough for transfer to complete
// Configuration descriptor in the other speed e.g if high speed then this is for full speed and vice versa
uint8_t const* tud_descriptor_other_speed_configuration_cb(uint8_t index)
{
    (void) index; // for multiple configurations

    // if link speed is high return fullspeed config, and vice versa
    return (tud_speed_get() == TUSB_SPEED_HIGH) ?  desc_fs_configuration : desc_hs_configuration;
}

#endif


// Invoked when a GET CONFIGURATION DESCRIPTOR request is recieved.
// Return a pointer to descriptor.
// Descriptor contents must exist long enough for transfer to complete
uint8_t const* tud_descriptor_configuration_cb(uint8_t configuration_index) {
    (void)configuration_index;

#if TUD_OPT_HIGH_SPEED
    // Although we are highspeed, host may be fullspeed.
    return (tud_speed_get() == TUSB_SPEED_HIGH) ?  desc_hs_configuration : desc_fs_configuration;
#else
    return desc_fs_configuration;
#endif
}


// --- String Descriptors ---

// table with strings
const char* const string_table[] = {
    0,                // 0 - supported languages (see below)
    "JavaDoesUSB",    // 1 - manufacturer
    "Loopback",       // 2 - product
    board_serial_num  // 3 - serial number
};

// Microsoft WCID (Microsoft OS 1.0 Descriptors) string descriptor (for string index 0xee)
// https://docs.microsoft.com/en-us/windows-hardware/drivers/usbcon/microsoft-defined-usb-descriptors
static const uint8_t msft_sig_desc[] = {
    0x12,                           // length = 18 bytes
    TUSB_DESC_STRING,               // descriptor type string
    'M', 0, 'S', 0, 'F', 0, 'T', 0, // 'M', 'S', 'F', 'T'
    '1', 0, '0', 0, '0', 0,         // '1', '0', '0'
    WCID_VENDOR_CODE,               // vendor code
    0                               // padding
};


static uint16_t str_desc_buf[32];

// Invoked when a GET STRING DESCRIPTOR request is received.
// Return pointer to string descriptor.
uint16_t const* tud_descriptor_string_cb(uint8_t index, uint16_t langid) {
    (void)langid;

    int str_len;

    if (index == 0) {
        str_desc_buf[1] = 0x0409;  // US English
        str_len = 1;

    } else if (index == 0xee) {
        return (const uint16_t*) msft_sig_desc;

    } else {
        if (index >= TU_ARRAY_SIZE(string_table))
            return NULL;

        const char* str = string_table[index];
        str_len = strlen(str);

        // Convert ASCII to UTF-16
        for (uint8_t i = 0; i < str_len; i++)
            str_desc_buf[1 + i] = str[i];
    }

    // first byte is length (including header), second byte is string type
    str_desc_buf[0] = (uint16_t)((2 * str_len + 2) | (TUSB_DESC_STRING << 8));

    return str_desc_buf;
}

// --- Microsoft WCID feature descriptor ---

const uint8_t wcid_feature_desc[] = {
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
