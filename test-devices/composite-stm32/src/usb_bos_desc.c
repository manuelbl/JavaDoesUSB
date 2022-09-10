//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// USB BOS descriptor to install WinUSB for loopback test interface
//

#include "usb_bos_desc.h"

#define DEV_INTF_GUID_NAME u"DeviceInterfaceGUIDs"
#define DEV_INTF_GUID_DATA u"{049CD59E-33EE-4CB2-B0BB-1C49F3CB6358}"

const struct msos20_desc_set {
    usb_msos20_desc_set_header descSetHeader;
    usb_msos20_desc_subset_header_config descSubsetConfig;
    usb_msos20_desc_subset_header_function descSubsetFunction;
    usb_msos20_desc_compatible_id descCompId;
    struct {
        uint16_t wLength;
        uint16_t wDescriptorType;
        uint16_t wPropertyDataType;
        uint16_t wPropertyNameLength;
        uint16_t propertyName[sizeof(DEV_INTF_GUID_NAME) / 2];
        uint16_t wPropertyDataLength;
        uint16_t propertyData[sizeof(DEV_INTF_GUID_DATA) / 2 + 1];
    } devIntfGuid;

} msos_set = {

    .descSetHeader = {
        .wLength = sizeof(usb_msos20_desc_set_header),
        .wDescriptorType = USB_MSOS20_DT_SET_HEADER_DESCRIPTOR,
        .dwWindowsVersion = USB_MSOS20_WIN_VER_8_1,
        .wTotalLength = sizeof(msos_set)
    },
    .descSubsetConfig = {
        .wLength = sizeof(usb_msos20_desc_subset_header_config),
        .wDescriptorType = USB_MSOS20_DT_SUBSET_HEADER_CONFIGURATION,
        .bConfigurationValue = 0,
        .wTotalLength = sizeof(msos_set.descSubsetConfig) + sizeof(msos_set.descSubsetFunction) +
                        sizeof(msos_set.descCompId) + sizeof(msos_set.devIntfGuid),
    },
    .descSubsetFunction = {
        .wLength = sizeof(usb_msos20_desc_subset_header_function),
        .wDescriptorType = USB_MSOS20_DT_SUBSET_HEADER_FUNCTION,
        .bFirstInterface = 2,
        .wTotalLength = sizeof(msos_set.descSubsetFunction) + sizeof(msos_set.descCompId) +
                        sizeof(msos_set.devIntfGuid),
    },
    .descCompId = {
        .wLength = sizeof(usb_msos20_desc_compatible_id),
        .wDescriptorType = USB_MSOS20_DT_FEATURE_COMPATBLE_ID,
        .compatibleID = "WINUSB\0\0",
        .subCompatibleID = "\0\0\0\0\0\0\0\0"
    },
    .devIntfGuid = {
        .wLength = sizeof(msos_set.devIntfGuid),
        .wDescriptorType = USB_MSOS20_DT_FEATURE_REG_PROPERTY,
        .wPropertyDataType = USB_MSOS20_PROP_DATA_TYPE_STRING_MULTI,
        .wPropertyNameLength = sizeof(msos_set.devIntfGuid.propertyName),
        .propertyName = DEV_INTF_GUID_NAME,
        .wPropertyDataLength = sizeof(msos_set.devIntfGuid.propertyData),
        .propertyData = DEV_INTF_GUID_DATA "\0"
    }
};

const usb_msos20_desc_set_header* msos_desc_set = &msos_set.descSetHeader;

const usb_msos20_platform_desc msos_desc = {
    .bLength = sizeof(usb_msos20_platform_desc),
    .bDescriptorType = USB_DT_DEVICE_CAPABILITY,
    .bDevCapabilityType = USB_DEV_CAPA_PLATFORM,
    .bReserved = 0,
    .platformCapabilityUUID = USB_PLATFORM_CAPABILITY_MICROSOFT_OS20_UUID,
    .dwWindowsVersion = USB_MSOS20_WIN_VER_8_1,
    .wMSOSDescriptorSetTotalLength = sizeof(msos_set),
    .bMS_VendorCode = MSOS_VENDOR_CODE,
    .bAltEnumCode = 0
};

// BOS device capability descriptors
const usb_bos_device_capability_desc* const bos_descs[] = {
    // Microsoft OS 2.0 descriptor (for autmatic WinUSB installation)
    (const usb_bos_device_capability_desc*)&msos_desc
};
