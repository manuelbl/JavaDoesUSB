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

#pragma once

#include <libopencm3/usb/usbd.h>

#ifdef __cplusplus
extern "C" {
#endif

/// Descriptor type Binary Device Object Store (BOS)

static const uint8_t USB_DT_BOS = 15;
/// Descriptor type Device Capability
#define USB_DT_DEVICE_CAPABILITY 16

/// Microsoft WCID string index
static const uint8_t USB_WIN_MSFT_WCID_STR_IDX = 0xee;

/// Microsoft compatible ID feature descriptor request index (wIndex)
static const uint16_t USB_WIN_COMP_ID_REQ_INDEX = 0x0004;

#define USB_WIN_WCID_DEFAULT_VENDOR_CODE 0xf0



/// USB BOS device capability types
typedef enum {
    /// USB BOS device capability type for Wireless USB-specific device level capabilities
    USB_DEV_CAPA_WIRELESS_USB = 0x01,
    /// USB BOS device capability type for USB 2.0 extension descriptor
    USB_DEV_CAPA_USB_2_0_EXTENSION = 0x02,
    /// USB BOS device capability type for SuperSpeed USB specific device level capabilities
    USB_DEV_CAPA_SUPERSPEED_USB = 0x03,
    /// USB BOS device capability type for instance unique ID used to identify the instance across all operating modes
    USB_DEV_CAPA_CONTAINER_ID = 0x04,
    /// USB BOS device capability type for device capability specific to a particular platform/operating system
    USB_DEV_CAPA_PLATFORM = 0x05,
    /// USB BOS device capability type for various PD capabilities of this device
    USB_DEV_CAPA_POWER_DELIVERY_CAPABILITY = 0x06,
    /// USB BOS device capability type for information on each battery supported by the device
    USB_DEV_CAPA_BATTERY_INFO_CAPABILITY = 0x07,
    /// USB BOS device capability type for consumer characteristics of a port on the device
    USB_DEV_CAPA_PD_CONSUMER_PORT_CAPABILITY = 0x08,
    /// USB BOS device capability type for provider characteristics of a port on the device
    USB_DEV_CAPA_PD_PROVIDER_PORT_CAPABILITY = 0x09,
    /// USB BOS device capability type for SuperSpeed Plus USB specific device level capabilities
    USB_DEV_CAPA_SUPERSPEED_PLUS = 0x0a,
    /// USB BOS device capability type for precision time measurement (PTM) capability descriptor
    USB_DEV_CAPA_PRECISION_TIME_MEASUREMENT = 0x0b,
    /// USB BOS device capability type for wireless USB 1.1-specific device level capabilities
    USB_DEV_CAPA_WIRELESS_USB_EXT = 0x0c,
    /// USB BOS device capability type for billboard capability
    USB_DEV_CAPA_BILLBOARD = 0x0d,
    /// USB BOS device capability type for authentication capability descriptor
    USB_DEV_CAPA_AUTHENTICATION = 0x0e,
    /// USB BOS device capability type billboard ex capability
    USB_DEV_CAPA_BILLBOARD_EX = 0x0f,
    /// USB BOS device capability type for summarizing configuration information for a function implemented by the device
    USB_DEV_CAPA_CONFIGURATION_SUMMARY = 0x10,
} usb_dev_capa_type_e;

/// USB BOS descriptor
typedef struct usb_bos_desc {
    /// Size of this descriptor
    uint8_t bLength;
    /// Type of this descriptor (use USB_DT_BOS)
    uint8_t bDescriptorType;
    /// Length of this descriptor and all of its sub descriptors
    uint16_t wTotalLength;
    /// The number of separate device capability descriptors in the BOS
    uint8_t bNumDeviceCaps;
} __attribute__((packed)) usb_bos_desc;

/// USB BOS device capability descriptor (generic)
typedef struct usb_bos_device_capability_desc {
    /// Size of this descriptor
    uint8_t bLength;
    /// Type of this descriptor (use USB_DT_DEVICE_CAPABILITY)
    uint8_t bDescriptorType;
    /// Device capability type (see usb_dev_capa_type_e)
    uint8_t bDevCapabilityType;
    /// Capability-specific data
    uint8_t data[];
} __attribute__((packed)) usb_bos_device_capability_desc;

/// USB BOS device capability platform descriptor
typedef struct usb_bos_platform_desc {
    /// Size of this descriptor
    uint8_t bLength;
    /// Type of this descriptor (use USB_DT_DEVICE_CAPABILITY)
    uint8_t bDescriptorType;
    /// Device capability type (use USB_DEV_CAPA_PLATFORM)
    uint8_t bDevCapabilityType;
    /// Reserved. Set to 0.
    uint8_t bReserved;
    /// A 128-bit number (UUID) that uniquely identifies a platform specific capability of the device
    uint8_t platformCapabilityUUID[16];
    /// Platform-specific capability data
    uint8_t capabilityData[];
} __attribute__((packed)) usb_bos_platform_desc;


/// Microsoft OS 2.0 request `wIndex` value to retrieve MS OS 2.0 vendor-specific descriptor
static const uint8_t USB_MSOS20_CTRL_INDEX_DESC = 0x07;
/// Microsoft OS 2.0 request `wIndex` value to set alternate enumeration
static const uint8_t USB_MSOS20_CTRL_INDEX_SET_ALT_ENUM = 0x08;

/// UUID for Microsoft OS 2.0 platform capability: {d8dd60df-4589-4cc7-9cd2-659d9e648a9f}
#define USB_PLATFORM_CAPABILITY_MICROSOFT_OS20_UUID {0xDF, 0x60, 0xDD, 0xD8, 0x89, 0x45, 0xC7, 0x4C, 0x9C, 0xD2, 0x65, 0x9D, 0x9E, 0x64, 0x8A, 0x9F}

/// Microsoft OS 2.0 descriptor types
typedef enum {
    /// Microsoft OS 2.0 descriptor type for set header
    USB_MSOS20_DT_SET_HEADER_DESCRIPTOR	= 0x00,
    /// Microsoft OS 2.0 descriptor type for configuration subset header
    USB_MSOS20_DT_SUBSET_HEADER_CONFIGURATION = 0x01,
    /// Microsoft OS 2.0 descriptor type for function subset header
    USB_MSOS20_DT_SUBSET_HEADER_FUNCTION = 0x02,
    /// Microsoft OS 2.0 feature descriptor type for compatible ID descriptor
    USB_MSOS20_DT_FEATURE_COMPATBLE_ID = 0x03,
    /// Microsoft OS 2.0 feature descriptor type for registry propery descriptor
    USB_MSOS20_DT_FEATURE_REG_PROPERTY = 0x04,
    /// Microsoft OS 2.0 feature descriptor type for minimum USB resume time descriptor
    USB_MSOS20_DT_FEATURE_MIN_RESUME_TIME = 0x05,
    /// Microsoft OS 2.0 feature descriptor type for model ID descriptor
    USB_MSOS20_DT_FEATURE_MODEL_ID = 0x06,
    /// Microsoft OS 2.0 feature descriptor type for CCGP device descriptor
    USB_MSOS20_DT_FEATURE_CCGP_DEVICE = 0x07,
    /// Microsoft OS 2.0 feature descriptor type for vendor revision descriptor
    USB_MSOS20_DT_FEATURE_VENDOR_REVISION = 0x08,
} usb_msos20_desc_type_e;

/// Microsoft OS 2.0 property types
typedef enum {
    /// A NULL-terminated Unicode String (REG_SZ)
    USB_MSOS20_PROP_DATA_TYPE_STRING = 1,
    /// A NULL-terminated Unicode String that includes environment variables (REG_EXPAND_SZ)
    USB_MSOS20_PROP_DATA_TYPE_STRING_EXPAND = 2,
    /// Free-form binary (REG_BINARY)
    USB_MSOS20_PROP_DATA_TYPE_BINARY = 3,
    /// A little-endian 32-bit integer (REG_DWORD_LITTLE_ENDIAN)
    USB_MSOS20_PROP_DATA_TYPE_INT32LE = 4,
    /// A big-endian 32-bit integer (REG_DWORD_BIG_ENDIAN)
    USB_MSOS20_PROP_DATA_TYPE_INT32BE = 5,
    /// A NULL-terminated Unicode string that contains a symbolic link (REG_LINK)
    USB_MSOS20_PROP_DATA_TYPE_STRING_LINK = 6,
    /// Multiple NULL-terminated Unicode strings (REG_MULTI_SZ)
    USB_MSOS20_PROP_DATA_TYPE_STRING_MULTI = 7,
} usb_msos20_prop_data_type_e;

/// Microsoft OS 2.0 descriptor Windows version
typedef enum {
    /// Windows version 8.1
    USB_MSOS20_WIN_VER_8_1 = 0x06030000,
    /// Windows version 10
    USB_MSOS20_WIN_VER_10  = 0x0a000000,
} usb_msos20_win_ver_e;

/// USB BOS device capability platform descriptor for Microsoft OS 2.0
typedef struct usb_msos20_platform_desc {
    /// Size of this descriptor
    uint8_t bLength;
    /// Type of this descriptor (use USB_DT_DEVICE_CAPABILITY)
    uint8_t bDescriptorType;
    /// Device capability type (use USB_DEV_CAPA_PLATFORM)
    uint8_t bDevCapabilityType;
    /// Reserved. Set to 0.
    uint8_t bReserved;
    /// A 128-bit number / UUID (use USB_PLATFORM_CAPABILITY_MICROSOFT_OS20_UUID)
    uint8_t platformCapabilityUUID[16];
    /// Minimum Windows version (see usb_msos20_win_ver_e)
    uint32_t dwWindowsVersion;
    /// The length, in bytes, of the MS OS 2.0 descriptor set
    uint16_t wMSOSDescriptorSetTotalLength;
    /// Vendor defined code to use to retrieve this version of the MS OS 2.0 descriptor and also to set alternate enumeration behavior on the device
    uint8_t bMS_VendorCode;
    /// A non-zero value to send to the device to indicate that the device may return non-default USB descriptors for enumeration. If the device does not support alternate enumeration, this value shall be 0.
    uint8_t bAltEnumCode;
} __attribute__((packed)) usb_msos20_platform_desc;

/// Microsoft OS 2.0 descriptor set header
typedef struct usb_msos20_desc_set_header
{
    /// The length, in bytes, of this header
    uint16_t wLength;
    /// The type of this descriptor (use USB_MSOS20_DT_SET_HEADER_DESCRIPTOR)
    uint16_t wDescriptorType;
    /// Windows version (see usb_msos20_win_ver_e)
    uint32_t dwWindowsVersion;
    /// The size of entire MS OS 2.0 descriptor set. The value shall match the value in the descriptor set information structure.
    uint16_t wTotalLength;
} __attribute__((packed)) usb_msos20_desc_set_header;

/// Microsoft OS 2.0 descriptor configuration subset header
typedef struct usb_msos20_desc_subset_header_config
{
    /// The length, in bytes, of this header
    uint16_t wLength;
    /// The type of this descriptor (use USB_MSOS20_DT_SUBSET_HEADER_CONFIGURATION)
    uint16_t wDescriptorType;
    /// The configuration value for the USB configuration to which this subset applies
    uint8_t bConfigurationValue;
    /// Reserved. Set to 0.
    uint8_t bReserved;
    /// The size of entire configuration subset including this header.
    uint16_t wTotalLength;
} __attribute__((packed)) usb_msos20_desc_subset_header_config;

/// Microsoft OS 2.0 descriptor function subset header
typedef struct usb_msos20_desc_subset_header_function
{
    /// The length, in bytes, of this header
    uint16_t wLength;
    /// The type of this descriptor (use USB_MSOS20_DT_SUBSET_HEADER_FUNCTION)
    uint16_t wDescriptorType;
    /// The interface number for the first interface of the function to which this subset applies.
    uint8_t bFirstInterface;
    /// Reserved. Set to 0.
    uint8_t bReserved;
    /// The size of entire function subset including this header.
    uint16_t wTotalLength;
} __attribute__((packed)) usb_msos20_desc_subset_header_function;

/// Microsoft OS 2.0 compatible ID descriptor
typedef struct usb_msos20_desc_compatible_id
{
    /// The length, in bytes, of this header
    uint16_t wLength;
    /// The type of this descriptor (use USB_MSOS20_DT_FEATURE_COMPATBLE_ID)
    uint16_t wDescriptorType;
    /// Compatible ID string
    char compatibleID[8];
    /// Sub-compatible ID string
    char subCompatibleID[8];
} __attribute__((packed)) usb_msos20_desc_compatible_id;

extern const usb_msos20_platform_desc msos_desc;

/**
 * Register the control request handler to respond to BOS request (for automatic WinUSB installation)
 * 
 * @param device USB device
 * @param bos_descs array of BOS descriptors
 * @param num_bos_descs number of BOS descriptors
 * @param msos_desc_set MS OS 2.0 descriptor set
 */
void usb_dev_register_bos(usbd_device* device,
        const usb_bos_device_capability_desc* const * bos_descs, int num_bos_descs,
        const usb_msos20_desc_set_header* msos_desc_set, uint8_t msos_vendor_code);

#ifdef __cplusplus
}
#endif 
