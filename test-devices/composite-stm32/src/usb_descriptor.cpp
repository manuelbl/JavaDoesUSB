//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB descriptor
//

#include "usb_descriptor.h"

#include <libopencm3/stm32/desig.h>
#include <libopencm3/usb/cdc.h>

#define ARRAY_SIZE(x) (sizeof(x) / sizeof(x[0]))

static void put_hex(uint32_t value, char *buf, int len);

#define USB_VID 0xcafe         // Vendor ID
#define USB_PID 0xcea0         // Product ID
#define USB_DEVICE_REL 0x0034  // release 0.3.3

// Interface index
#define INTF_CDC_COMM 0
#define INTF_CDC_DATA 1
#define INTF_LOOPBACK 2

static char serial_num[13];

const char *const usb_desc_strings[] = {
    "JavaDoesUSB",          //  USB Manufacturer
    "Loopback",             //  USB Product
    serial_num,             //  Serial number,
    "Loopback Serial Port"  // Function description
};

enum usb_strings_index {  //  Index of USB strings.  Must sync with above, starts from 1.
    USB_STRINGS_MANUFACTURER_ID = 1,
    USB_STRINGS_PRODUCT_ID,
    USB_STRINGS_SERIAL_NUMBER_ID,
    USB_STRINGS_SERIAL_PORT_ID,
};

typedef struct {
    struct usb_cdc_header_descriptor header;
    struct usb_cdc_call_management_descriptor call_mgmt;
    struct usb_cdc_acm_descriptor acm;
    struct usb_cdc_union_descriptor cdc_union;
} __attribute__((packed)) cdcacm_functional_descriptors;

// --- CDC ACM ---

// CDC communicatoin endpoint
static const usb_endpoint_descriptor cdc_comm_ep_desc[] = {
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_CDC_COMM,
        .bmAttributes = USB_ENDPOINT_ATTR_INTERRUPT,
        .wMaxPacketSize = INTR_MAX_PACKET_SIZE,
        .bInterval = 32,
        .extra = nullptr,
        .extralen = 0,
    }
};

// CDC data endpoints
static const usb_endpoint_descriptor cdc_data_ep_desc[] = {
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_CDC_DATA_RX,
        .bmAttributes = USB_ENDPOINT_ATTR_BULK,
        .wMaxPacketSize = BULK_MAX_PACKET_SIZE,
        .bInterval = 1,
        .extra = nullptr,
        .extralen = 0,
    },
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_CDC_DATA_TX,
        .bmAttributes = USB_ENDPOINT_ATTR_BULK,
        .wMaxPacketSize = BULK_MAX_PACKET_SIZE,
        .bInterval = 1,
        .extra = nullptr,
        .extralen = 0,
    }
};

// CDC ACM function descriptor
static const cdcacm_functional_descriptors cdc_func_desc = {
    .header = {
        .bFunctionLength = sizeof(usb_cdc_header_descriptor),
        .bDescriptorType = CS_INTERFACE,
        .bDescriptorSubtype = USB_CDC_TYPE_HEADER,
        .bcdCDC = 0x0110,
    },
    .call_mgmt = {
        // see chapter 5.3.1 in PSTN120
        .bFunctionLength = sizeof(usb_cdc_call_management_descriptor),
        .bDescriptorType = CS_INTERFACE,
        .bDescriptorSubtype = USB_CDC_TYPE_CALL_MANAGEMENT,
        .bmCapabilities = 0,  // no call management
        .bDataInterface = INTF_CDC_DATA,
    },
    .acm = {
        // see chapter 5.3.2 in PSTN120
        .bFunctionLength = sizeof(usb_cdc_acm_descriptor),
        .bDescriptorType = CS_INTERFACE,
        .bDescriptorSubtype = USB_CDC_TYPE_ACM,
        .bmCapabilities = 2,  // device supports the request combination of Set_Line_Coding, Set_Control_Line_State,
                                // Get_Line_Coding, and the notification Serial_State
    },
    .cdc_union = {
        .bFunctionLength = sizeof(usb_cdc_union_descriptor),
        .bDescriptorType = CS_INTERFACE,
        .bDescriptorSubtype = USB_CDC_TYPE_UNION,
        .bControlInterface = INTF_CDC_COMM,
        .bSubordinateInterface0 = INTF_CDC_DATA,
    }
};

// CDC interfaces descriptors
static const usb_interface_descriptor cdc_comm_if_desc[] = {
    {
        .bLength = USB_DT_INTERFACE_SIZE,
        .bDescriptorType = USB_DT_INTERFACE,
        .bInterfaceNumber = INTF_CDC_COMM,
        .bAlternateSetting = 0,
        .bNumEndpoints = ARRAY_SIZE(cdc_comm_ep_desc),
        .bInterfaceClass = USB_CLASS_CDC,
        .bInterfaceSubClass = USB_CDC_SUBCLASS_ACM,
        .bInterfaceProtocol = USB_CDC_PROTOCOL_AT,
        .iInterface = 0,
        .endpoint = cdc_comm_ep_desc,
        .extra = &cdc_func_desc,
        .extralen = sizeof(cdc_func_desc),
    }
};

static const usb_interface_descriptor cdc_data_if_desc[] = {
    {
        .bLength = USB_DT_INTERFACE_SIZE,
        .bDescriptorType = USB_DT_INTERFACE,
        .bInterfaceNumber = INTF_CDC_DATA,
        .bAlternateSetting = 0,
        .bNumEndpoints = ARRAY_SIZE(cdc_data_ep_desc),
        .bInterfaceClass = USB_CLASS_DATA,
        .bInterfaceSubClass = 0,
        .bInterfaceProtocol = 0,
        .iInterface = 0,
        .endpoint = cdc_data_ep_desc,
        .extra = nullptr,
        .extralen = 0,
    }
};

// CDC interface association
static const usb_iface_assoc_descriptor cdc_assoc_desc = {
    .bLength = USB_DT_INTERFACE_ASSOCIATION_SIZE,
    .bDescriptorType = USB_DT_INTERFACE_ASSOCIATION,
    .bFirstInterface = INTF_CDC_COMM,
    .bInterfaceCount = 2,
    .bFunctionClass = USB_CLASS_CDC,
    .bFunctionSubClass = USB_CDC_SUBCLASS_ACM,
    .bFunctionProtocol = USB_CDC_PROTOCOL_AT,
    .iFunction = USB_STRINGS_SERIAL_PORT_ID,
};

// --- Loopback test interface ---

// Test endpoints
static const struct usb_endpoint_descriptor loopback_endpoint_descs[] = {
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_LOOPBACK_RX,
        .bmAttributes = USB_ENDPOINT_ATTR_BULK,
        .wMaxPacketSize = BULK_MAX_PACKET_SIZE,
        .bInterval = 0,
        .extra = nullptr,
        .extralen = 0,
    },
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_LOOPBACK_TX,
        .bmAttributes = USB_ENDPOINT_ATTR_BULK,
        .wMaxPacketSize = BULK_MAX_PACKET_SIZE,
        .bInterval = 0,
        .extra = nullptr,
        .extralen = 0,
    },
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_ECHO_RX,
        .bmAttributes = USB_ENDPOINT_ATTR_INTERRUPT,
        .wMaxPacketSize = INTR_MAX_PACKET_SIZE,
        .bInterval = 16,
        .extra = nullptr,
        .extralen = 0,
    },
    {
        .bLength = USB_DT_ENDPOINT_SIZE,
        .bDescriptorType = USB_DT_ENDPOINT,
        .bEndpointAddress = EP_ECHO_TX,
        .bmAttributes = USB_ENDPOINT_ATTR_INTERRUPT,
        .wMaxPacketSize = INTR_MAX_PACKET_SIZE,
        .bInterval = 16,
        .extra = nullptr,
        .extralen = 0,
    },
};

// Test interface
static const struct usb_interface_descriptor loopback_if_descs[] = {
    {
        .bLength = USB_DT_INTERFACE_SIZE,
        .bDescriptorType = USB_DT_INTERFACE,
        .bInterfaceNumber = INTF_LOOPBACK,
        .bAlternateSetting = 0,
        .bNumEndpoints = ARRAY_SIZE(loopback_endpoint_descs),
        .bInterfaceClass = USB_CLASS_VENDOR,
        .bInterfaceSubClass = 0,
        .bInterfaceProtocol = 0,  // vendor specific
        .iInterface = 0,
        .endpoint = loopback_endpoint_descs,
        .extra = nullptr,
        .extralen = 0,
    },
};

static const struct usb_interface usb_interfaces[] = {
    {
        .cur_altsetting = nullptr,
        .num_altsetting = ARRAY_SIZE(cdc_comm_if_desc),
        .iface_assoc = &cdc_assoc_desc,
        .altsetting = cdc_comm_if_desc,
    },
    {
        .cur_altsetting = nullptr,
        .num_altsetting = ARRAY_SIZE(cdc_data_if_desc),
        .iface_assoc = nullptr,
        .altsetting = cdc_data_if_desc,
    },
    {
        .cur_altsetting = nullptr,
        .num_altsetting = ARRAY_SIZE(loopback_if_descs),
        .iface_assoc = nullptr,
        .altsetting = loopback_if_descs,
    },
};

const struct usb_config_descriptor usb_config_descs[] = {
    {
        .bLength = USB_DT_CONFIGURATION_SIZE,
        .bDescriptorType = USB_DT_CONFIGURATION,
        .wTotalLength = 0,
        .bNumInterfaces = ARRAY_SIZE(usb_interfaces),
        .bConfigurationValue = 1,
        .iConfiguration = 0,
        .bmAttributes = 0x80,  // bus-powered, i.e. it draws power from USB bus
        .bMaxPower = 0xfa,     // 500 mA
        .interface = usb_interfaces,
    },
};

const struct usb_device_descriptor usb_device_desc = {
    .bLength = USB_DT_DEVICE_SIZE,
    .bDescriptorType = USB_DT_DEVICE,
    .bcdUSB = 0x0210,   // USB version 2.1.0 (minimum version for BOS)
    .bDeviceClass = 0xef,
    .bDeviceSubClass = 0x02,
    .bDeviceProtocol = 0x01,
    .bMaxPacketSize0 = 64,
    .idVendor = USB_VID,
    .idProduct = USB_PID,
    .bcdDevice = USB_DEVICE_REL,
    .iManufacturer = USB_STRINGS_MANUFACTURER_ID,
    .iProduct = USB_STRINGS_PRODUCT_ID,
    .iSerialNumber = USB_STRINGS_SERIAL_NUMBER_ID,
    .bNumConfigurations = ARRAY_SIZE(usb_config_descs),
};

void usb_init_serial_num() {
    uint32_t id0 = DESIG_UNIQUE_ID0;
    uint32_t id1 = DESIG_UNIQUE_ID1;
    uint32_t id2 = DESIG_UNIQUE_ID2;

    id0 += id2;

    put_hex(id0, serial_num, 8);
    put_hex(id1, serial_num + 8, 4);
    serial_num[12] = 0;
}

const static char HEX_DIGITS[] = "0123456789ABCDEF";

void put_hex(uint32_t value, char *buf, int len) {
    for (int idx = 0; idx < len; idx++) {
        buf[idx] = HEX_DIGITS[value >> 28];
        value = value << 4;
    }
}
