//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Loopback interface
//

#include "usbd_loopback.h"

#include "circ_buf.h"
#include "main.h"
#include "usbd_ctlreq.h"

static uint8_t USBD_Vendor_Init(USBD_HandleTypeDef *pdev, uint8_t cfgidx);
static uint8_t USBD_Vendor_DeInit(USBD_HandleTypeDef *pdev, uint8_t cfgidx);
static uint8_t USBD_Vendor_Setup(USBD_HandleTypeDef *pdev, USBD_SetupReqTypedef *req);
static uint8_t USBD_Vendor_EP0_RxReady(USBD_HandleTypeDef *pdev);
static uint8_t USBD_Vendor_DataIn(USBD_HandleTypeDef *pdev, uint8_t epnum);
static uint8_t USBD_Vendor_DataOut(USBD_HandleTypeDef *pdev, uint8_t epnum);
static uint8_t *USBD_Vendor_GetConfigDesc(uint16_t *length);
static uint8_t *USBD_Vendor_GetStringDesc(USBD_HandleTypeDef *pdev, uint8_t index, uint16_t *length);

static uint8_t data_packet_rx[DATA_PACKET_SIZE];
static uint8_t data_packet_tx[DATA_PACKET_SIZE];
static volatile bool is_transmitting = false;
static volatile bool is_receiving = false;
static volatile uint32_t ctrl_req_value = 0;
static uint8_t ctrl_req_buf[4];

USBD_ClassTypeDef USBD_Vendor_Class = {
    USBD_Vendor_Init,
    USBD_Vendor_DeInit,
    USBD_Vendor_Setup,
    NULL,  // EP0_TxSent
    USBD_Vendor_EP0_RxReady,
    USBD_Vendor_DataIn,
    USBD_Vendor_DataOut,
    NULL,  // SOF
    NULL,
    NULL,
    NULL,
    USBD_Vendor_GetConfigDesc,
    NULL,
    NULL,
    USBD_Vendor_GetStringDesc,
};

#define CONFIG_DESC_SIZE 32U

// USB blinky device configuration descriptor
__ALIGN_BEGIN static uint8_t Configuration_Desc[CONFIG_DESC_SIZE] __ALIGN_END = {
    0x09,                         // bLength: Configuration Descriptor size
    USB_DESC_TYPE_CONFIGURATION,  // bDescriptorType: Configuration
    LOBYTE(CONFIG_DESC_SIZE),     // wTotalLength: Bytes returned
    HIBYTE(0x00),                 // (cont.)
    0x01,                         // bNumInterfaces: 1 interface
    0x01,                         // bConfigurationValue: Configuration value
    USBD_IDX_CONFIG_STR,          // iConfiguration: Index of string descriptor for configuration
    0x80,                         // bmAttributes: bus powered
    0xfa,                         // MaxPower 500 mA: this current is used for detecting Vbus

    // Interface 0 descriptor
    // offset 09
    0x09,                     // bLength: Interface Descriptor size
    USB_DESC_TYPE_INTERFACE,  // bDescriptorType: Interface descriptor type
    0x00,                     // bInterfaceNumber: Number of Interface
    0x00,                     // bAlternateSetting: Alternate setting
    0x02,                     // bNumEndpoints
    0xff,                     // bInterfaceClass: vendor-specific
    0x00,                     // bInterfaceSubClass : 1=BOOT, 0=no boot
    0x00,                     // nInterfaceProtocol : 0=vendor specific
    USBD_IDX_INTERFACE_STR,   // iInterface: Index of string descriptor

    // Endpoint 1 descriptor
    // offset 18
    0x07,                      // bLength: endpoint descriptor size
    USB_DESC_TYPE_ENDPOINT,    // bDescriptorType: endpoint
    DATA_OUT_EP,               // bEndpointAddress: Endpoint Address (OUT)
    USBD_EP_TYPE_BULK,         // bmAttributes: bulk endpoint
    LOBYTE(DATA_PACKET_SIZE),  // wMaxPacketSize: maximum of 64
    HIBYTE(0x00),              // (cont.)
    0x00,                      // bInterval: Polling Interval

    // Endpoint 2 descriptor
    // offset 25
    0x07,                      // bLength: endpoint descriptor size
    USB_DESC_TYPE_ENDPOINT,    // bDescriptorType: endpoint
    DATA_IN_EP,                // bEndpointAddress: Endpoint Address (IN)
    USBD_EP_TYPE_BULK,         // bmAttributes: bulk endpoint
    LOBYTE(DATA_PACKET_SIZE),  // wMaxPacketSize: maximum of 64
    HIBYTE(0x00),              // (cont.)
    0x00,                      // bInterval: Polling Interval

    // offset 32
};

#define WCID_VENDOR_CODE 0x37

// Microsoft WCID string descriptor (string index 0xee)
static const uint8_t msft_sig_desc[] = {
    0x12,                  // length = 18 bytes
    USB_DESC_TYPE_STRING,  // descriptor type string
    'M',
    0,
    'S',
    0,
    'F',
    0,
    'T',
    0,  // 'M', 'S', 'F', 'T'
    '1',
    0,
    '0',
    0,
    '0',
    0,                 // '1', '0', '0'
    WCID_VENDOR_CODE,  // vendor code
    0                  // padding
};

// Microsoft WCID feature descriptor (index 0x0004)
static const uint8_t wcid_feature_desc[] = {
    0x28, 0x00, 0x00, 0x00,                          // length = 40 bytes
    0x00, 0x01,                                      // version 1.0 (in BCD)
    0x04, 0x00,                                      // compatibility descriptor index 0x0004
    0x01,                                            // number of sections
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,        // reserved (7 bytes)
    0x00,                                            // interface number 0
    0x01,                                            // reserved
    0x57, 0x49, 0x4E, 0x55, 0x53, 0x42, 0x00, 0x00,  // Compatible ID "WINUSB\0\0"
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  // Subcompatible ID (unused)
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00               // reserved 6 bytes
};

uint8_t USBD_Vendor_Init(USBD_HandleTypeDef *pdev, uint8_t cfgidx) {
    // open RX bulk endpoint
    USBD_LL_OpenEP(pdev, DATA_OUT_EP, USBD_EP_TYPE_BULK, DATA_PACKET_SIZE);
    pdev->ep_in[DATA_OUT_EP & 0x7F].is_used = 1;

    // Enable it to receive data (NAK -> VALID)
    is_receiving = true;
    USBD_LL_PrepareReceive(pdev, DATA_OUT_EP, data_packet_rx, sizeof(data_packet_rx));

    // open TX bulk endpoint
    USBD_LL_OpenEP(pdev, DATA_IN_EP, USBD_EP_TYPE_BULK, DATA_PACKET_SIZE);
    pdev->ep_in[DATA_IN_EP & 0x7F].is_used = 1;

    return USBD_OK;
}

uint8_t USBD_Vendor_DeInit(USBD_HandleTypeDef *pdev, uint8_t cfgidx) {
    // close bulk end points
    USBD_LL_CloseEP(pdev, DATA_OUT_EP);
    pdev->ep_in[DATA_OUT_EP & 0x7F].is_used = 0;

    USBD_LL_CloseEP(pdev, DATA_IN_EP);
    pdev->ep_in[DATA_IN_EP & 0x7F].is_used = 0;

    return USBD_OK;
}

uint8_t USBD_Vendor_Setup(USBD_HandleTypeDef *pdev, USBD_SetupReqTypedef *req) {
    // Handle vendor control request
    USBD_StatusTypeDef ret = USBD_OK;

    switch (req->bmRequest & USB_REQ_TYPE_MASK) {
        case USB_REQ_TYPE_VENDOR:
            if (req->bmRequest == 0x41 && req->bRequest == 0x01 && req->wIndex == 0x0000 && req->wLength == 0) {
                // save value
                ctrl_req_value = req->wValue;

            } else if (req->bmRequest == 0x41 && req->bRequest == 0x02 && req->wIndex == 0x0000 && req->wLength == 4) {
                // receive 4 data bytes
                USBD_CtlPrepareRx(pdev, ctrl_req_buf, 4);

            } else if (req->bmRequest == 0xc1 && req->bRequest == 0x03 && req->wIndex == 0x0000 && req->wLength == 4) {
                // send saved value
                uint32_t* p_value = (uint32_t*)&ctrl_req_buf;
                *p_value = ctrl_req_value;
                USBD_CtlSendData(pdev, ctrl_req_buf, 4);

            } else if (req->bRequest == WCID_VENDOR_CODE && req->wIndex == 0x0004) {
                // WCID feature request
                uint16_t len = sizeof(wcid_feature_desc);
                if (len > req->wLength)
                    len = req->wLength;
                USBD_CtlSendData(pdev, (uint8_t *)wcid_feature_desc, len);

            } else {
                USBD_CtlError(pdev, req);
                ret = USBD_FAIL;
            }
            break;

        default:
            USBD_CtlError(pdev, req);
            ret = USBD_FAIL;
            break;
    }

    return ret;
}

uint8_t USBD_Vendor_EP0_RxReady(USBD_HandleTypeDef *pdev) {
    uint32_t* p_value = (uint32_t*)&ctrl_req_buf;
    ctrl_req_value = *p_value;
    return USBD_OK;
}


uint8_t *USBD_Vendor_GetConfigDesc(uint16_t *length) {
    // Return configuration descriptor
    *length = sizeof(Configuration_Desc);
    return Configuration_Desc;
}

uint8_t *USBD_Vendor_GetStringDesc(USBD_HandleTypeDef *pdev, uint8_t index, uint16_t *length) {
    // Return Microsoft OS string descriptor for index 0xee
    if (index == 0xee) {
        *length = sizeof(msft_sig_desc);
        return (uint8_t *)msft_sig_desc;
    } else {
        *length = 0;
        USBD_CtlError(pdev, NULL);
        return NULL;
    }
}

uint8_t USBD_Vendor_DataOut(USBD_HandleTypeDef *pdev, uint8_t epnum) {
    uint32_t num_bytes_received = USBD_LL_GetRxDataSize(pdev, epnum);

    // add recievied data to circular buffer
    circ_buf_add_data(data_packet_rx, num_bytes_received);
    is_receiving = false;

    return USBD_OK;
}

uint8_t USBD_Vendor_DataIn(USBD_HandleTypeDef *pdev, uint8_t epnum) {
    is_transmitting = false;
    return USBD_OK;
}

void usbd_check(USBD_HandleTypeDef *pdev) {
    if (!is_transmitting) {
        // get data from circular buffer
        int size = circ_buf_get_data(data_packet_tx, DATA_PACKET_SIZE);
        if (size != 0) {
            is_transmitting = true;
            USBD_LL_Transmit(pdev, DATA_IN_EP, data_packet_tx, size);
        }
    }

    if (!is_receiving) {
        // check for sufficient space to enable receiving
        bool has_space = circ_buf_avail_size() >= DATA_PACKET_SIZE;
        if (has_space) {
            is_receiving = true;
            USBD_LL_PrepareReceive(pdev, DATA_OUT_EP, data_packet_rx, sizeof(data_packet_rx));
        }
    }
}
