//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB device descriptor
//

#include "usbd_desc.h"

#include "stm32f1xx_ll_utils.h"
#include "usbd_conf.h"
#include "usbd_core.h"

#define USBD_VID 0xcafe
#define USBD_LANGID_STRING 1033
#define USBD_MANUFACTURER_STRING "JavaDoesUSB"
#define USBD_PID_FS 0xceaf
#define USBD_PRODUCT_STRING_FS "Loopback"
#define USBD_CONFIGURATION_STRING_FS "Loopback Config"
#define USBD_INTERFACE_STRING_FS "Loopback Interface"
#define USBD_DEV_RELESE 0x0061

static void GetSerialNumber(void);
static void IntToUnicode(uint32_t value, uint8_t *pbuf, uint8_t len);

static uint8_t *GetDeviceDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);
static uint8_t *GetLangIDStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);
static uint8_t *GetManufacturerStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);
static uint8_t *GetProductStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);
static uint8_t *GetSerialStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);
static uint8_t *GetConfigurationStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);
static uint8_t *GetInterfaceStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length);

USBD_DescriptorsTypeDef USBD_Descriptors = {
    GetDeviceDescriptor,    GetLangIDStrDescriptor,        GetManufacturerStrDescriptor, GetProductStrDescriptor,
    GetSerialStrDescriptor, GetConfigurationStrDescriptor, GetInterfaceStrDescriptor,
};

// USB device descriptor.
static const __ALIGN_BEGIN uint8_t deviceDesc[USB_LEN_DEV_DESC] __ALIGN_END = {
    0x12,                       // bLength
    USB_DESC_TYPE_DEVICE,       // bDescriptorType
    0x00,                       // bcdUSB = 2.00
    0x02,                       //
    0xff,                       // bDeviceClass = vendor specific
    0x00,                       // bDeviceSubClass
    0x00,                       // bDeviceProtocol = vendor specific
    USB_MAX_EP0_SIZE,           // bMaxPacketSize
    LOBYTE(USBD_VID),           // idVendor
    HIBYTE(USBD_VID),           //
    LOBYTE(USBD_PID_FS),        // idProduct
    HIBYTE(USBD_PID_FS),        //
    LOBYTE(USBD_DEV_RELESE),    // bcdDevice
    HIBYTE(USBD_DEV_RELESE),    //
    USBD_IDX_MFC_STR,           // Index of manufacturer string
    USBD_IDX_PRODUCT_STR,       // Index of product string
    USBD_IDX_SERIAL_STR,        // Index of serial number string
    USBD_MAX_NUM_CONFIGURATION  // bNumConfigurations
};

// USB lang indentifier descriptor.
static const __ALIGN_BEGIN uint8_t langIDDesc[USB_LEN_LANGID_STR_DESC] __ALIGN_END = {
    USB_LEN_LANGID_STR_DESC,
    USB_DESC_TYPE_STRING,
    LOBYTE(USBD_LANGID_STRING),
    HIBYTE(USBD_LANGID_STRING),
};

// Internal string descriptor.
__ALIGN_BEGIN uint8_t stringDescBuffer[USBD_MAX_STR_DESC_SIZ] __ALIGN_END;

#define USB_SIZ_STRING_SERIAL 0x1A

static __ALIGN_BEGIN uint8_t serialStringDesc[USB_SIZ_STRING_SERIAL] __ALIGN_END = {
    USB_SIZ_STRING_SERIAL,
    USB_DESC_TYPE_STRING,
};

uint8_t *GetDeviceDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    *length = sizeof(deviceDesc);
    return (uint8_t *)deviceDesc;
}

uint8_t *GetLangIDStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    *length = sizeof(langIDDesc);
    return (uint8_t *)langIDDesc;
}

uint8_t *GetProductStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    USBD_GetString((uint8_t *)USBD_PRODUCT_STRING_FS, stringDescBuffer, length);
    return stringDescBuffer;
}

uint8_t *GetManufacturerStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    USBD_GetString((uint8_t *)USBD_MANUFACTURER_STRING, stringDescBuffer, length);
    return stringDescBuffer;
}

uint8_t *GetSerialStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    *length = USB_SIZ_STRING_SERIAL;
    GetSerialNumber();
    return serialStringDesc;
}

uint8_t *GetConfigurationStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    USBD_GetString((uint8_t *)USBD_CONFIGURATION_STRING_FS, stringDescBuffer, length);
    return stringDescBuffer;
}

uint8_t *GetInterfaceStrDescriptor(USBD_SpeedTypeDef speed, uint16_t *length) {
    UNUSED(speed);
    USBD_GetString((uint8_t *)USBD_INTERFACE_STRING_FS, stringDescBuffer, length);
    return stringDescBuffer;
}

static void GetSerialNumber(void) {
    uint32_t id0 = LL_GetUID_Word0();
    uint32_t id1 = LL_GetUID_Word1();
    uint32_t id2 = LL_GetUID_Word2();

    id0 += id2;

    if (id0 != 0) {
        IntToUnicode(id0, &serialStringDesc[2], 8);
        IntToUnicode(id1, &serialStringDesc[18], 4);
    }
}

const static char HEX_DIGITS[] = "0123456789ABCDEF";

static void IntToUnicode(uint32_t value, uint8_t *pbuf, uint8_t len) {
    uint8_t idx = 0;

    for (idx = 0; idx < len; idx++) {
        pbuf[2 * idx] = HEX_DIGITS[value >> 28];
        value = value << 4;
        pbuf[2 * idx + 1] = 0;
    }
}
