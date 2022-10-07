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
#include "class/cdc/cdc.h"
#include "vendor_custom.h"


// --- Device Descriptor ---

tusb_desc_device_t const desc_device = {
    .bLength = sizeof(tusb_desc_device_t),
    .bDescriptorType = TUSB_DESC_DEVICE,
    .bcdUSB = 0x0210, // 2.1.0 (minimum for BOS)

    .bDeviceClass = TUSB_CLASS_MISC,
    .bDeviceSubClass = MISC_SUBCLASS_COMMON,
    .bDeviceProtocol = MISC_PROTOCOL_IAD,
    .bMaxPacketSize0 = CFG_TUD_ENDPOINT0_SIZE,

    .idVendor = 0xCAFE,
    .idProduct = 0xCEA0,
    .bcdDevice = 0x0035,  // version 0.3.5

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
    INTF_CDC_COMM = 0,
    INTF_CDC_DATA,
    INTF_LOOPBACK,
    INTF_NUM_TOTAL
};

#define CONFIG_TOTAL_LEN (TUD_CONFIG_DESC_LEN + TUD_CDC_DESC_LEN + 9 + 7 + 7)

uint8_t const desc_configuration[] = {
    // Config number, interface count, string index, total length, attribute, power in mA
    TUD_CONFIG_DESCRIPTOR(1, INTF_NUM_TOTAL, 0, CONFIG_TOTAL_LEN, 0x00, 500),
    // CDC interfaces
    TUD_CDC_DESCRIPTOR(INTF_CDC_COMM, 0, EP_CDC_COMM, 8, EP_CDC_DATA_RX, EP_CDC_DATA_TX, BULK_MAX_PACKET_SIZE),
    // Loopback interface
    CUSTOM_VENDOR_INTERFACE(INTF_LOOPBACK, 2),
    // Loopback endpoint OUT
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_RX, BULK_MAX_PACKET_SIZE),
    // Loopback endpoint IN
    CUSTOM_VENDOR_BULK_ENDPOINT(EP_LOOPBACK_TX, BULK_MAX_PACKET_SIZE)
};

// Invoked when a GET CONFIGURATION DESCRIPTOR request is recieved.
// Return a pointer to descriptor.
// Descriptor contents must exist long enough for transfer to complete
uint8_t const* tud_descriptor_configuration_cb(uint8_t configuration_index) {
    (void)configuration_index;
    return desc_configuration;
}


// --- BOS Descriptor ---

#define BOS_TOTAL_LEN (TUD_BOS_DESC_LEN + TUD_BOS_MICROSOFT_OS_DESC_LEN)

#define MS_OS_20_DESC_LEN  0xB2

// BOS Descriptor is required for webUSB
uint8_t const desc_bos[] = {
    // total length, number of device caps
    TUD_BOS_DESCRIPTOR(BOS_TOTAL_LEN, 1),

    // Microsoft OS 2.0 descriptor
    TUD_BOS_MS_OS_20_DESCRIPTOR(MS_OS_20_DESC_LEN, MSOS_VENDOR_CODE)
};

uint8_t const * tud_descriptor_bos_cb(void) {
    return desc_bos;
}


uint8_t const desc_ms_os_20[] = {
    // Set header: length, type, windows version, total length
    U16_TO_U8S_LE(0x000A), U16_TO_U8S_LE(MS_OS_20_SET_HEADER_DESCRIPTOR), U32_TO_U8S_LE(0x06030000), U16_TO_U8S_LE(MS_OS_20_DESC_LEN),

    // Configuration subset header: length, type, configuration index, reserved, configuration total length
    U16_TO_U8S_LE(0x0008), U16_TO_U8S_LE(MS_OS_20_SUBSET_HEADER_CONFIGURATION), 0, 0, U16_TO_U8S_LE(MS_OS_20_DESC_LEN-0x0A),

    // Function Subset header: length, type, first interface, reserved, subset length
    U16_TO_U8S_LE(0x0008), U16_TO_U8S_LE(MS_OS_20_SUBSET_HEADER_FUNCTION), INTF_LOOPBACK, 0, U16_TO_U8S_LE(MS_OS_20_DESC_LEN-0x0A-0x08),

    // MS OS 2.0 Compatible ID descriptor: length, type, compatible ID, sub compatible ID
    U16_TO_U8S_LE(0x0014), U16_TO_U8S_LE(MS_OS_20_FEATURE_COMPATBLE_ID), 'W', 'I', 'N', 'U', 'S', 'B', 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // sub-compatible

    // MS OS 2.0 Registry property descriptor: length, type
    U16_TO_U8S_LE(MS_OS_20_DESC_LEN-0x0A-0x08-0x08-0x14), U16_TO_U8S_LE(MS_OS_20_FEATURE_REG_PROPERTY),
    U16_TO_U8S_LE(0x0007), U16_TO_U8S_LE(0x002A), // wPropertyDataType, wPropertyNameLength and PropertyName "DeviceInterfaceGUIDs\0" in UTF-16
    'D', 0x00, 'e', 0x00, 'v', 0x00, 'i', 0x00, 'c', 0x00, 'e', 0x00, 'I', 0x00, 'n', 0x00, 't', 0x00, 'e', 0x00,
    'r', 0x00, 'f', 0x00, 'a', 0x00, 'c', 0x00, 'e', 0x00, 'G', 0x00, 'U', 0x00, 'I', 0x00, 'D', 0x00, 's', 0x00, 0x00, 0x00,
    U16_TO_U8S_LE(0x0050), // wPropertyDataLength
    //bPropertyData: “{82DF5D1A-BD37-431C-81B7-52EB2093B98F}”.
    '{', 0x00, '8', 0x00, '2', 0x00, 'D', 0x00, 'F', 0x00, '5', 0x00, 'D', 0x00, '1', 0x00, 'A', 0x00, '-', 0x00,
    'B', 0x00, 'D', 0x00, '3', 0x00, '7', 0x00, '-', 0x00, '4', 0x00, '3', 0x00, '1', 0x00, 'C', 0x00, '-', 0x00,
    '8', 0x00, '1', 0x00, 'B', 0x00, '7', 0x00, '-', 0x00, '5', 0x00, '2', 0x00, 'E', 0x00, 'B', 0x00, '2', 0x00,
    '0', 0x00, '9', 0x00, '3', 0x00, 'B', 0x00, '9', 0x00, '8', 0x00, 'F', 0x00, '}', 0x00, 0x00, 0x00, 0x00, 0x00
};

TU_VERIFY_STATIC(sizeof(desc_ms_os_20) == MS_OS_20_DESC_LEN, "Incorrect size");



// --- String Descriptors ---

// table with strings
const char* const string_table[] = {
    0,                // 0 - supported languages (see below)
    "JavaDoesUSB",    // 1 - manufacturer
    "Loopback",       // 2 - product
    board_serial_num  // 3 - serial number
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
