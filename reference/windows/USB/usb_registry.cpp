//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#include "usb_registry.hpp"
#include "usb_device.hpp"
#include "usb_error.hpp"
#include "scope.hpp"

#include <algorithm>
#include <iostream>
#include <string>

#include <initguid.h>
#include <Dbt.h>
#include <devpkey.h>
#include <usbiodef.h>
#include <usbioctl.h>

#pragma comment (lib, "SetupAPI.lib")
#pragma comment (lib, "Winusb.lib")


usb_registry::usb_registry()
: on_connected_callback(nullptr), on_disconnected_callback(nullptr),
    is_device_list_ready(false), background_thread_id_(0), message_window(nullptr) {
}

usb_registry::~usb_registry() {

    SendMessage(message_window, WM_CLOSE, 0, 0);
    monitor_thread.join();
    
}

std::vector<usb_device_ptr> usb_registry::get_devices() {
    return devices;
}

void usb_registry::set_on_device_connected(std::function<void(usb_device_ptr device)> callback) {
    on_connected_callback = callback;
}

void usb_registry::set_on_device_disconnected(std::function<void(usb_device_ptr device)> callback) {
    on_disconnected_callback = callback;
}

void usb_registry::start() {
    monitor_thread = std::thread(&usb_registry::monitor, this);
    
    std::unique_lock<std::mutex> wait_lock(monitor_mutex);
    monitor_condition.wait(wait_lock, [this] { return is_device_list_ready; });
}

static const LPCWSTR CLASS_NAME = L"USB_MONITOR";
static const LPCWSTR WINDOW_NAME = L"USB device monitor";

void usb_registry::monitor() {

    background_thread_id_ = GetCurrentThreadId();
    HMODULE instance = GetModuleHandleW(nullptr);

    WNDCLASSEXW wx = { 0 };
    wx.cbSize = sizeof(WNDCLASSEX);
    wx.lpfnWndProc = handle_windows_message;
    wx.hInstance = instance;
    wx.lpszClassName = CLASS_NAME;

    ATOM atom = RegisterClassExW(&wx);
    if (atom == 0)
        usb_error::throw_error("internal error (RegisterClassExW)");

    auto atom_gard = make_scope_exit([instance]() { UnregisterClassW(CLASS_NAME, instance); });

    message_window = CreateWindowExW(
        0,
        CLASS_NAME,
        WINDOW_NAME,
        0,
        0,
        0,
        0,
        0,
        HWND_MESSAGE,
        nullptr,
        instance,
        this
    );
    if (message_window == nullptr)
        usb_error::throw_error("internal error (CreateWindowExW)");

    DEV_BROADCAST_DEVICEINTERFACE_W notification_filter = { 0 };

    notification_filter.dbcc_size = sizeof(notification_filter);
    notification_filter.dbcc_devicetype = DBT_DEVTYP_DEVICEINTERFACE;
    notification_filter.dbcc_classguid = GUID_DEVINTERFACE_USB_DEVICE;

    HDEVNOTIFY notify_handle = RegisterDeviceNotificationW(message_window, &notification_filter, DEVICE_NOTIFY_WINDOW_HANDLE /* | DEVICE_NOTIFY_ALL_INTERFACE_CLASSES */);
    if (notify_handle == NULL)
        usb_error::throw_error("internal error (RegisterDeviceNotificationW)");

    auto notify_handle_guard = make_scope_exit([notify_handle]() { UnregisterDeviceNotification(notify_handle); });

    detect_present_devices();

    is_device_list_ready = true;
    monitor_condition.notify_all();

    MSG msg = { 0 };
    while (GetMessageW(&msg, message_window, 0, 0) > 0) {
        std::cout << "GetMessageW" << std::endl;
    }
}

void usb_registry::detect_present_devices() {

    // get device information set of all USB devices present
    HDEVINFO dev_info_set_hdl = SetupDiGetClassDevsW(&GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if (dev_info_set_hdl == INVALID_HANDLE_VALUE)
        throw usb_error("internal error (SetupDiGetClassDevsA)", GetLastError());

    // ensure the result id destroyed when the scope is left
    auto dev_info_set_guard = make_scope_exit([dev_info_set_hdl]() {
        SetupDiDestroyDeviceInfoList(dev_info_set_hdl);
    });

    SP_DEVINFO_DATA dev_info = { sizeof(dev_info) };

    // iterate over the set
    for (int i = 0; ; i++) {
        if (!SetupDiEnumDeviceInfo(dev_info_set_hdl, i, &dev_info)) {
            DWORD err = GetLastError();
            if (err == ERROR_NO_MORE_ITEMS)
                break;
            throw usb_error("Internal error (SetupDiEnumDeviceInfo)", err);
        }

        // create new device
        auto device = create_device(dev_info_set_hdl, &dev_info);
        devices.push_back(device);
    }
}

std::shared_ptr<usb_device> usb_registry::create_device(HDEVINFO dev_info_set_hdl, SP_DEVINFO_DATA* dev_info) {

    DWORD usb_port_num = get_device_property_int(dev_info_set_hdl, dev_info, &DEVPKEY_Device_Address);
    std::wstring instance_id = get_device_property_string(dev_info_set_hdl, dev_info, &DEVPKEY_Device_InstanceId);
    std::wstring parent_instance_id = get_device_property_string(dev_info_set_hdl, dev_info, &DEVPKEY_Device_Parent);

    std::wstring hub_path = get_device_path(parent_instance_id, &GUID_DEVINTERFACE_USB_HUB);

    // open parent (hub)
    HANDLE hub_handle = CreateFileW(hub_path.c_str(), GENERIC_WRITE, FILE_SHARE_WRITE, nullptr, OPEN_EXISTING, 0, nullptr);
    if (hub_handle == INVALID_HANDLE_VALUE)
        usb_error::throw_error("Cannot open USB hub");

    auto hub_handle_guard = make_scope_exit([hub_handle]() {
        CloseHandle(hub_handle);
    });

    auto path = get_device_path(instance_id, &GUID_DEVINTERFACE_USB_DEVICE);

    // get device descriptor
    USB_NODE_CONNECTION_INFORMATION_EX conn_info = { 0 };
    conn_info.ConnectionIndex = usb_port_num;
    DWORD size = 0;
    if (!DeviceIoControl(hub_handle, IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX, &conn_info, sizeof(conn_info), &conn_info, sizeof(conn_info), &size, nullptr))
        usb_error::throw_error("Internal error (cannot get device descriptor)");

    // Create new device
    std::shared_ptr<usb_device> device(new usb_device(path, conn_info.DeviceDescriptor.idVendor, conn_info.DeviceDescriptor.idProduct));
    device->set_product_names(
        get_string(hub_handle, usb_port_num, conn_info.DeviceDescriptor.iManufacturer),
        get_string(hub_handle, usb_port_num, conn_info.DeviceDescriptor.iProduct),
        get_string(hub_handle, usb_port_num, conn_info.DeviceDescriptor.iSerialNumber)
    );
    return device;
}

LRESULT usb_registry::handle_windows_message(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {

    usb_registry* self = reinterpret_cast<usb_registry*>(GetWindowLongPtr(hWnd, GWLP_USERDATA));

    switch (uMsg) {
    case WM_CREATE: {
        CREATESTRUCT* cs = reinterpret_cast<CREATESTRUCT*>(lParam);
        self = reinterpret_cast<usb_registry*>(cs->lpCreateParams);
        SetLastError(ERROR_SUCCESS);
        LONG_PTR result = SetWindowLongPtr(hWnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(self));
        break;
    }

    case WM_DESTROY: {
        LONG_PTR result = SetWindowLongPtr(hWnd, GWLP_USERDATA, NULL);
        PostQuitMessage(0);
        break;
    }
    }

    if (self != nullptr && self->handle_message(hWnd, uMsg, wParam, lParam))
        return NULL;

    return DefWindowProcW(hWnd, uMsg, wParam, lParam);
}

bool usb_registry::handle_message(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
    if (uMsg != WM_DEVICECHANGE)
        return false;
    if (wParam != DBT_DEVICEARRIVAL && wParam != DBT_DEVICEREMOVECOMPLETE)
        return false;
    DEV_BROADCAST_HDR* header = reinterpret_cast<DEV_BROADCAST_HDR*>(lParam);
    if (header->dbch_devicetype != DBT_DEVTYP_DEVICEINTERFACE)
        return false;

    DEV_BROADCAST_DEVICEINTERFACE_W* broadcast = reinterpret_cast<DEV_BROADCAST_DEVICEINTERFACE_W*>(lParam);
    if (wParam == DBT_DEVICEARRIVAL) {
        on_device_connected(broadcast->dbcc_name);
    }
    else {
        on_device_disconnected(broadcast->dbcc_name);
    }
    return true;
}

void usb_registry::on_device_connected(const WCHAR* path) {
    // get as set of all USB devices present
    HDEVINFO dev_info_set_hdl = SetupDiGetClassDevsW(&GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if (dev_info_set_hdl == INVALID_HANDLE_VALUE)
        usb_error::throw_error("internal error (SetupDiGetClassDevsW)");

    // ensure the result is destroyed when the scope is left
    auto dev_info_set_guard = make_scope_exit([dev_info_set_hdl]() {
        SetupDiDestroyDeviceInfoList(dev_info_set_hdl);
    });

    SP_DEVICE_INTERFACE_DATA dev_intf_data = { sizeof(dev_intf_data) };
    if (!SetupDiOpenDeviceInterfaceW(dev_info_set_hdl, path, 0, &dev_intf_data))
        usb_error::throw_error("internal error (SetupDiOpenDeviceInterfaceW)");

    auto dev_intf_data_guard = make_scope_exit([dev_info_set_hdl, &dev_intf_data]() {
        SetupDiDeleteDeviceInterfaceData(dev_info_set_hdl, &dev_intf_data);
    });

    SP_DEVINFO_DATA dev_info = { sizeof(dev_info) };
    if (!SetupDiGetDeviceInterfaceDetailW(dev_info_set_hdl, &dev_intf_data, nullptr, 0, nullptr, &dev_info)) {
        DWORD err = GetLastError();
        if (err != ERROR_INSUFFICIENT_BUFFER)
            throw usb_error("internal error (SetupDiGetDeviceInterfaceDetailW)", err);
    }

    // create new device
    auto device = create_device(dev_info_set_hdl, &dev_info);
    devices.push_back(device);

    // Call callback function
    if (on_connected_callback != nullptr) {
        try {
            on_connected_callback(device);
        }
        catch (const std::exception& e) {
            std::cerr << "Unhandled exception: " << e.what() << std::endl;
        }
        catch (...) {
            std::cerr << "Unhandled exception (not derived from std::exception)" << std::endl;
        }
    }
}

void usb_registry::on_device_disconnected(const WCHAR* path) {

    // find device in device list
    auto it = std::find_if(devices.cbegin(), devices.cend(), [path](auto device) { return lstrcmpiW(path, device->device_path()) == 0; });
    if (it == devices.cend())
        return; // not part of the device list

    // remove from device list
    usb_device_ptr device = *it;
    devices.erase(it);
        
    // call callback function
    if (on_disconnected_callback != nullptr) {
        try {
            on_disconnected_callback(device);
        }
        catch (const std::exception& e) {
            std::cerr << "Unhandled exception: " << e.what() << std::endl;
        }
        catch (...) {
            std::cerr << "Unhandled exception (not derived from std::exception)" << std::endl;
        }
    }
}

uint32_t usb_registry::get_device_property_int(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key) {
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

std::wstring usb_registry::get_device_property_string(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key) {

    // query length
    DWORD required_size = 0;
    DEVPROPTYPE property_type;
    if (!SetupDiGetDevicePropertyW(dev_info, dev_info_data, prop_key, &property_type, nullptr, 0, &required_size, 0)) {
        DWORD err = GetLastError();
        if (err != ERROR_INSUFFICIENT_BUFFER)
            throw usb_error("internal error (SetupDiGetDevicePropertyW)", err);
    }

    // check property type
    if (property_type != DEVPROP_TYPE_STRING)
        throw usb_error("internal error (SetupDiGetDevicePropertyW)");

    // query property value
    std::wstring property_value;
    property_value.resize(required_size / sizeof(WCHAR));
    if (!SetupDiGetDevicePropertyW(dev_info, dev_info_data, prop_key, &property_type, reinterpret_cast<PBYTE>(&property_value[0]), required_size, nullptr, 0))
        usb_error::throw_error("internal error (SetupDiGetDevicePropertyW)");

    return property_value;
}

std::wstring usb_registry::get_device_path(const std::wstring& instance_id, const GUID* interface_guid) {

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

std::vector<uint8_t> usb_registry::get_descriptor(HANDLE hub_handle, ULONG usb_port_num, uint16_t descriptor_type, int index, int language_id, int request_size) {
    int size = sizeof(USB_DESCRIPTOR_REQUEST) + (request_size != 0 ? request_size : 256);
    uint8_t* descriptor_request_buffer = new uint8_t[size];
    auto dev_info_set_guard = make_scope_exit([descriptor_request_buffer]() {
        delete [] descriptor_request_buffer;
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
    } else {
        expected_size = *reinterpret_cast<uint16_t*>(descriptor_request->Data);
    }

    // check against effective size
    int effective_size = bytesReturned - sizeof(USB_DESCRIPTOR_REQUEST);
    if (effective_size != expected_size) {
        if (request_size != 0)
            throw usb_error("Unexpected descriptor size");

        // repeat with correct size
        return get_descriptor(hub_handle, usb_port_num, descriptor_type, index, language_id, expected_size);
    }

    return std::vector<uint8_t>(descriptor_request->Data, descriptor_request->Data + effective_size);
}

std::string usb_registry::get_string(HANDLE hub_handle, ULONG usb_port_num, int index) {
    if (index == 0)
        return "";

    std::vector<uint8_t> str_desc_raw = get_descriptor(hub_handle, usb_port_num, USB_STRING_DESCRIPTOR_TYPE, index, 0x0409);
    USB_STRING_DESCRIPTOR* str_desc = reinterpret_cast<USB_STRING_DESCRIPTOR*>(str_desc_raw.data());

    // required length of UTF-8 string
    int len = WideCharToMultiByte(CP_UTF8, 0, str_desc->bString, str_desc->bLength / 2 - 1, nullptr, 0, nullptr, nullptr);

    // conver to UTF-8
    std::string result;
    result.resize(len, 'x');
    WideCharToMultiByte(CP_UTF8, 0, str_desc->bString, str_desc->bLength / 2 - 1, &result[0], len, nullptr, nullptr);
    return result;
}
