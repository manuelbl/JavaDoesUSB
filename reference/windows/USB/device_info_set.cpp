//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#include "device_info_set.h"
#include "usb_error.hpp"
#include "scope.hpp"
#include <devpkey.h>

device_info_set::device_info_set(HDEVINFO dev_info_set)
    : dev_info_set_(dev_info_set), dev_info_data_({ sizeof(dev_intf_data_) }),
    has_dev_intf_data_(false), dev_intf_data_({ sizeof(dev_intf_data_) }), iteration_index(-1)
{
}

device_info_set device_info_set::of_present_devices(const GUID& interface_guid, const std::wstring& instance_id) {
    auto dev_info_set = SetupDiGetClassDevsW(&interface_guid, !instance_id.empty() ? instance_id.c_str() : nullptr, nullptr, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if (dev_info_set == INVALID_HANDLE_VALUE)
        usb_error::throw_error("internal error (SetupDiGetClassDevsW)");
    return device_info_set(dev_info_set);
}

device_info_set device_info_set::of_instance(const std::wstring& instance_id) {
    auto instance = of_empty();
    instance.add_instance(instance_id);
    return instance;
}

device_info_set device_info_set::of_path(const std::wstring& device_path) {
    auto instance = of_empty();
    instance.add_device_path(device_path);
    return instance;
}

device_info_set device_info_set::of_empty() {
    auto dev_info_set = SetupDiCreateDeviceInfoList(nullptr, nullptr);
    if (dev_info_set == INVALID_HANDLE_VALUE)
        usb_error::throw_error("internal error (SetupDiCreateDeviceInfoList)");
    return device_info_set(dev_info_set);
}

device_info_set::device_info_set(device_info_set&& info_set) noexcept
        : dev_info_set_(info_set.dev_info_set_), dev_info_data_(info_set.dev_info_data_),
        has_dev_intf_data_(info_set.has_dev_intf_data_), dev_intf_data_(info_set.dev_intf_data_),
        iteration_index(info_set.iteration_index) {
    info_set.dev_info_set_ = INVALID_HANDLE_VALUE;
    info_set.has_dev_intf_data_ = false;
}

device_info_set::~device_info_set() {
    if (dev_info_set_ == INVALID_HANDLE_VALUE)
        return;

    if (has_dev_intf_data_)
        SetupDiDeleteDeviceInterfaceData(dev_info_set_, &dev_intf_data_);
    SetupDiDestroyDeviceInfoList(dev_info_set_);
}

void device_info_set::add_instance(const std::wstring& instance_id) {
    if (SetupDiOpenDeviceInfoW(dev_info_set_, instance_id.c_str(), nullptr, 0, &dev_info_data_) == 0)
        throw usb_error("internal error (SetupDiOpenDeviceInfoW)", GetLastError());
}

void device_info_set::add_device_path(const std::wstring& device_path) {
    if (has_dev_intf_data_)
        throw usb_error("calling add_device_path() multiple times is not implemented");

    // load device information into dev info set
    if (SetupDiOpenDeviceInterfaceW(dev_info_set_, device_path.c_str(), 0, &dev_intf_data_) == 0)
        usb_error::throw_error("internal error (SetupDiOpenDeviceInterfaceW)");
    has_dev_intf_data_ = true;

    if (SetupDiGetDeviceInterfaceDetailW(dev_info_set_, &dev_intf_data_, nullptr, 0, nullptr, &dev_info_data_) == 0) {
        auto err = GetLastError();
        if (err != ERROR_INSUFFICIENT_BUFFER)
            throw usb_error("internal error (SetupDiGetDeviceInterfaceDetailW)", err);
    }
}

bool device_info_set::next() {
    iteration_index += 1;

    if (SetupDiEnumDeviceInfo(dev_info_set_, iteration_index, &dev_info_data_) == 0) {
        auto err = GetLastError();
        if (err == ERROR_NO_MORE_ITEMS)
            return false;
        throw usb_error("internal error (SetupDiEnumDeviceInfo)", err);
    }

    return true;
}


uint32_t device_info_set::get_device_property_int(const DEVPROPKEY& prop_key) {
    // query property value
    DEVPROPTYPE property_type;
    uint32_t property_value = -1;
    if (!SetupDiGetDevicePropertyW(dev_info_set_, &dev_info_data_, &prop_key, &property_type, reinterpret_cast<PBYTE>(&property_value), sizeof(property_value), nullptr, 0))
        usb_error::throw_error("internal error (SetupDiGetDevicePropertyW)");

    // check property type
    if (property_type != DEVPROP_TYPE_UINT32)
        throw usb_error("internal error (SetupDiGetDevicePropertyW)");

    return property_value;
}

std::vector<uint8_t> device_info_set::get_device_property_variable_length(const DEVPROPKEY& prop_key, DEVPROPTYPE expected_type) {

    // query length
    DWORD required_size = 0;
    DEVPROPTYPE property_type;
    if (!SetupDiGetDevicePropertyW(dev_info_set_, &dev_info_data_, &prop_key, &property_type, nullptr, 0, &required_size, 0)) {
        DWORD err = GetLastError();
        if (err == ERROR_NOT_FOUND)
            return {};
        if (err != ERROR_INSUFFICIENT_BUFFER)
            throw usb_error("internal error (SetupDiGetDevicePropertyW)", err);
    }

    // check property type
    if (property_type != expected_type)
        throw usb_error("internal error (SetupDiGetDevicePropertyW)");

    // query property value
    std::vector<uint8_t> property_value;
    property_value.resize(required_size);
    if (!SetupDiGetDevicePropertyW(dev_info_set_, &dev_info_data_, &prop_key, &property_type, &property_value[0], required_size, nullptr, 0))
        usb_error::throw_error("internal error (SetupDiGetDevicePropertyW)");

    return property_value;
}

std::wstring device_info_set::get_device_property_string(const DEVPROPKEY& prop_key) {

    auto property_value = get_device_property_variable_length(prop_key, DEVPROP_TYPE_STRING);
    if (property_value.size() == 0)
        return L"";
    return std::wstring(reinterpret_cast<const WCHAR*>(&property_value[0]));
}

std::vector<std::wstring> device_info_set::get_device_property_string_list(const DEVPROPKEY& prop_key) {
    auto property_value = get_device_property_variable_length(prop_key, DEVPROP_TYPE_STRING | DEVPROP_TYPEMOD_LIST);
    if (property_value.size() == 0)
        return {};
    return split_string_list(reinterpret_cast<const WCHAR*>(&property_value[0]));
}

std::vector<std::wstring> device_info_set::split_string_list(const wchar_t* str_list_raw) {
    std::vector<std::wstring> str_list;
    int offset = 0;
    while (str_list_raw[offset] != L'\0') {
        str_list.push_back(str_list_raw + offset);
        offset += static_cast<int>(str_list.back().length()) + 1;
    }

    return str_list;
}

bool device_info_set::is_composite_device() {
    std::wstring device_service = get_device_property_string(DEVPKEY_Device_Service);
    return lstrcmpiW(device_service.c_str(), L"usbccgp") == 0;
}

std::wstring device_info_set::get_device_path(const std::wstring& instance_id, const GUID& interface_guid) {

    // get device info set for instance
    HDEVINFO dev_info_set = SetupDiGetClassDevsW(&interface_guid, instance_id.c_str(), nullptr, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if (dev_info_set == INVALID_HANDLE_VALUE)
        usb_error::throw_error("internal error (SetupDiGetClassDevsW)");

    // ensure the result is destroyed when the scope is left
    auto dev_info_set_guard = make_scope_exit([dev_info_set]() {
        SetupDiDestroyDeviceInfoList(dev_info_set);
    });

    // retrieve first element of enumeration
    SP_DEVICE_INTERFACE_DATA dev_intf_data = { sizeof(dev_intf_data) };
    if (!SetupDiEnumDeviceInterfaces(dev_info_set, nullptr, &interface_guid, 0, &dev_intf_data))
        usb_error::throw_error("internal error (SetupDiEnumDeviceInterfaces)");

    // retrieve path
    uint8_t dev_path_buf[MAX_PATH * sizeof(WCHAR) + sizeof(DWORD)];
    memset(dev_path_buf, 0, sizeof(dev_path_buf));
    PSP_DEVICE_INTERFACE_DETAIL_DATA_W intf_detail_data = reinterpret_cast<PSP_DEVICE_INTERFACE_DETAIL_DATA_W>(dev_path_buf);
    intf_detail_data->cbSize = sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA_W);
    if (!SetupDiGetDeviceInterfaceDetailW(dev_info_set, &dev_intf_data, intf_detail_data, sizeof(dev_path_buf), nullptr, nullptr))
        throw usb_error("Internal error (SetupDiGetDeviceInterfaceDetailA)", GetLastError());

    return intf_detail_data->DevicePath;
}

std::wstring device_info_set::get_device_path_by_guid(const std::wstring& instance_id) {
    auto device_guids = find_device_interface_guids();

    CLSID clsid{};
    // use GUIDs to get device path
    for (const std::wstring& guid : device_guids) {
        if (CLSIDFromString(guid.c_str(), &clsid) != NOERROR)
            continue;

        try {
            return get_device_path(instance_id.c_str(), clsid);
        }
        catch (usb_error&) {
            // ignore and try next one
        }
    }

    return {};
}

std::vector<std::wstring> device_info_set::find_device_interface_guids() {
    HKEY reg_key = SetupDiOpenDevRegKey(dev_info_set_, &dev_info_data_, DICS_FLAG_GLOBAL, 0, DIREG_DEV, KEY_READ);
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
