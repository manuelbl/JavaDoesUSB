//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#include "usb_device_info.hpp"
#include "scope.hpp"
#include "usb_error.hpp"
#include <regex>
#include <usbiodef.h>
#include <usbioctl.h>
#include <devpkey.h>


std::wstring usb_device_info::get_device_path(const std::wstring& instance_id, const GUID* interface_guid) {

    // get device info set for instance
    HDEVINFO dev_info_set_hdl = SetupDiGetClassDevsW(interface_guid, instance_id.c_str(), nullptr, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if (dev_info_set_hdl == INVALID_HANDLE_VALUE)
        usb_error::throw_error("internal error (SetupDiGetClassDevsW)");

    // ensure the result is destroyed when the scope is left
    auto dev_info_set_guard = make_scope_exit([dev_info_set_hdl]() {
        SetupDiDestroyDeviceInfoList(dev_info_set_hdl);
    });

    // retrieve first element of enumeration
    SP_DEVICE_INTERFACE_DATA dev_intf_data = { sizeof(dev_intf_data) };
    if (!SetupDiEnumDeviceInterfaces(dev_info_set_hdl, nullptr, interface_guid, 0, &dev_intf_data))
        usb_error::throw_error("internal error (SetupDiEnumDeviceInterfaces)");

    // retrieve path
    uint8_t dev_path_buf[MAX_PATH * sizeof(WCHAR) + sizeof(DWORD)];
    memset(dev_path_buf, 0, sizeof(dev_path_buf));
    PSP_DEVICE_INTERFACE_DETAIL_DATA_W intf_detail_data = reinterpret_cast<PSP_DEVICE_INTERFACE_DETAIL_DATA_W>(dev_path_buf);
    intf_detail_data->cbSize = sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA_W);
    if (!SetupDiGetDeviceInterfaceDetailW(dev_info_set_hdl, &dev_intf_data, intf_detail_data, sizeof(dev_path_buf), nullptr, nullptr))
        throw usb_error("Internal error (SetupDiGetDeviceInterfaceDetailA)", GetLastError());

    return intf_detail_data->DevicePath;
}

uint32_t usb_device_info::get_device_property_int(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key) {
    // query property value
    DEVPROPTYPE property_type;
    uint32_t property_value = -1;
    if (!SetupDiGetDevicePropertyW(dev_info, dev_info_data, prop_key, &property_type, reinterpret_cast<PBYTE>(&property_value), sizeof(property_value), nullptr, 0))
        usb_error::throw_error("internal error (SetupDiGetDevicePropertyW)");

    // check property type
    if (property_type != DEVPROP_TYPE_UINT32)
        throw usb_error("internal error (SetupDiGetDevicePropertyW)");

    return property_value;
}

std::vector<uint8_t> usb_device_info::get_device_property_variable_length(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key, DEVPROPTYPE expected_type) {

    // query length
    DWORD required_size = 0;
    DEVPROPTYPE property_type;
    if (!SetupDiGetDevicePropertyW(dev_info, dev_info_data, prop_key, &property_type, nullptr, 0, &required_size, 0)) {
        DWORD err = GetLastError();
        if (err != ERROR_INSUFFICIENT_BUFFER)
            throw usb_error("internal error (SetupDiGetDevicePropertyW)", err);
    }

    // check property type
    if (property_type != expected_type)
        throw usb_error("internal error (SetupDiGetDevicePropertyW)");

    // query property value
    std::vector<uint8_t> property_value;
    property_value.resize(required_size);
    if (!SetupDiGetDevicePropertyW(dev_info, dev_info_data, prop_key, &property_type, &property_value[0], required_size, nullptr, 0))
        usb_error::throw_error("internal error (SetupDiGetDevicePropertyW)");

    return property_value;
}

std::wstring usb_device_info::get_device_property_string(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key) {

    auto property_value = get_device_property_variable_length(dev_info, dev_info_data, prop_key, DEVPROP_TYPE_STRING);
    return std::wstring(reinterpret_cast<const WCHAR*>(&property_value[0]));
}

std::vector<std::wstring> usb_device_info::get_device_property_string_list(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key) {
    auto property_value = get_device_property_variable_length(dev_info, dev_info_data, prop_key, DEVPROP_TYPE_STRING | DEVPROP_TYPEMOD_LIST);
    return split_string_list(reinterpret_cast<const WCHAR*>(&property_value[0]));
}

std::vector<std::wstring> usb_device_info::split_string_list(const wchar_t* str_list_raw) {
    std::vector<std::wstring> str_list;
    int offset = 0;
    while (str_list_raw[offset] != L'\0') {
        str_list.push_back(str_list_raw + offset);
        offset += static_cast<int>(str_list.back().length()) + 1;
    }

    return str_list;
}

std::vector<uint8_t> usb_device_info::get_descriptor(HANDLE hub_handle, ULONG usb_port_num, uint16_t descriptor_type, int index, int language_id, int request_size) {
    int size = sizeof(USB_DESCRIPTOR_REQUEST) + (request_size != 0 ? request_size : 256);
    uint8_t* descriptor_request_buffer = new uint8_t[size];
    auto dev_info_set_guard = make_scope_exit([descriptor_request_buffer]() {
        delete[] descriptor_request_buffer;
    });

    // setup request data structure
    USB_DESCRIPTOR_REQUEST* descriptor_request = reinterpret_cast<USB_DESCRIPTOR_REQUEST*>(descriptor_request_buffer);
    descriptor_request->ConnectionIndex = usb_port_num;
    descriptor_request->SetupPacket.bmRequest = 0x80; // device-to-host / type standard / recipient device
    descriptor_request->SetupPacket.bRequest = 0x06; // GET_DESCRIPTOR
    descriptor_request->SetupPacket.wValue = (descriptor_type << 8) | index;
    descriptor_request->SetupPacket.wIndex = language_id;
    descriptor_request->SetupPacket.wLength = static_cast<USHORT>(size - sizeof(USB_DESCRIPTOR_REQUEST));

    // get descriptor
    DWORD bytesReturned = 0;
    if (!DeviceIoControl(hub_handle, IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION, descriptor_request, size, descriptor_request, size, &bytesReturned, nullptr))
        throw usb_error("Cannot retrieve descriptor (DeviceIoControl)", GetLastError());

    // determine expected size of descriptor
    int expected_size;
    if (descriptor_type != USB_CONFIGURATION_DESCRIPTOR_TYPE) {
        expected_size = descriptor_request->Data[0];
    }
    else {
        auto config_desc = reinterpret_cast<USB_CONFIGURATION_DESCRIPTOR*>(descriptor_request->Data);
        expected_size = config_desc->wTotalLength;
    }

    // check against effective size
    int effective_size = bytesReturned - sizeof(USB_DESCRIPTOR_REQUEST);
    if (effective_size < expected_size) {
        if (request_size != 0)
            throw usb_error("Unexpected descriptor size");

        // repeat with larger size
        return get_descriptor(hub_handle, usb_port_num, descriptor_type, index, language_id, expected_size);
    }

    return std::vector<uint8_t>(descriptor_request->Data, descriptor_request->Data + effective_size);
}

std::string usb_device_info::get_string(HANDLE hub_handle, ULONG usb_port_num, int index) {
    if (index == 0)
        return "";

    std::vector<uint8_t> str_desc_raw = get_descriptor(hub_handle, usb_port_num, USB_STRING_DESCRIPTOR_TYPE, index, 0x0409);
    USB_STRING_DESCRIPTOR* str_desc = reinterpret_cast<USB_STRING_DESCRIPTOR*>(str_desc_raw.data());

    // required length of UTF-8 string
    int len = WideCharToMultiByte(CP_UTF8, 0, str_desc->bString, str_desc->bLength / 2 - 1, nullptr, 0, nullptr, nullptr);

    // convert to UTF-8
    std::string result;
    result.resize(len, 'x');
    WideCharToMultiByte(CP_UTF8, 0, str_desc->bString, str_desc->bLength / 2 - 1, &result[0], len, nullptr, nullptr);
    return result;
}

bool usb_device_info::is_composite_device(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data) {
    std::wstring device_service = usb_device_info::get_device_property_string(dev_info, dev_info_data, &DEVPKEY_Device_Service);
    return lstrcmpiW(device_service.c_str(), L"usbccgp") == 0;
}

static const std::wregex multiple_interface_pattern(L"USB\\\\VID_[0-9A-Fa-f]{4}&PID_[0-9A-Fa-f]{4}&MI_([0-9A-Fa-f]{2})");

int usb_device_info::get_first_interface(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data) {
    // Also see https://docs.microsoft.com/en-us/windows-hardware/drivers/install/standard-usb-identifiers#multiple-interface-usb-devices

    auto hardware_ids = usb_device_info::get_device_property_string_list(dev_info, dev_info_data, &DEVPKEY_Device_HardwareIds);

    for (const std::wstring& id : hardware_ids) {
        auto matches = std::wsmatch{};
        if (std::regex_search(id, matches, multiple_interface_pattern))
            return std::stoul(matches[1].str(), nullptr, 16);
    }

    return -1;
}

std::vector<std::wstring> usb_device_info::find_device_interface_guids(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data) {
    HKEY reg_key = SetupDiOpenDevRegKey(dev_info, dev_info_data, DICS_FLAG_GLOBAL, 0, DIREG_DEV, KEY_READ);
    if (reg_key == INVALID_HANDLE_VALUE)
        throw usb_error("Cannot open device registry key", GetLastError());

    auto reg_key_guard = make_scope_exit([reg_key]() {
        RegCloseKey(reg_key);
    });

    // read registry value (without buffer, to query length)
    DWORD value_type = 0;
    DWORD value_size = 0;
    LSTATUS res = RegQueryValueExW(reg_key, L"DeviceInterfaceGUIDs", nullptr, &value_type, nullptr, &value_size);
    if (res == ERROR_FILE_NOT_FOUND)
        return std::vector<std::wstring>();
    if (res != 0 && res != ERROR_MORE_DATA)
        throw usb_error("Internal error (RegQueryValueExW)", res);

    std::vector<uint8_t> str_list_raw;
    str_list_raw.resize(value_size);

    // read registry value (with buffer)
    res = RegQueryValueExW(reg_key, L"DeviceInterfaceGUIDs", nullptr, &value_type, &str_list_raw[0], &value_size);
    if (res != 0)
        throw usb_error("Internal error (RegQueryValueExW)", res);

    return split_string_list(reinterpret_cast<const wchar_t*>(&str_list_raw[0]));
}
