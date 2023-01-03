//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#include "usb_registry.hpp"
#include "usb_device.hpp"
#include "usb_device_info.hpp"
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
    HDEVINFO dev_info_set = SetupDiGetClassDevsW(&GUID_DEVINTERFACE_USB_DEVICE, NULL, NULL, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if (dev_info_set == INVALID_HANDLE_VALUE)
        throw usb_error("internal error (SetupDiGetClassDevsA)", GetLastError());

    // ensure the result id destroyed when the scope is left
    auto dev_info_set_guard = make_scope_exit([dev_info_set]() {
        SetupDiDestroyDeviceInfoList(dev_info_set);
    });

    SP_DEVINFO_DATA dev_info_data = { sizeof(dev_info_data) };

    // iterate over the set
    for (int i = 0; ; i++) {
        if (!SetupDiEnumDeviceInfo(dev_info_set, i, &dev_info_data)) {
            DWORD err = GetLastError();
            if (err == ERROR_NO_MORE_ITEMS)
                break;
            throw usb_error("Internal error (SetupDiEnumDeviceInfo)", err);
        }

        // create new device
        auto device = create_device(dev_info_set, &dev_info_data);
        devices.push_back(device);
    }
}

std::shared_ptr<usb_device> usb_registry::create_device(HDEVINFO dev_info_set, SP_DEVINFO_DATA* dev_info_data) {

    DWORD usb_port_num = usb_device_info::get_device_property_int(dev_info_set, dev_info_data, &DEVPKEY_Device_Address);
    std::wstring instance_id = usb_device_info::get_device_property_string(dev_info_set, dev_info_data, &DEVPKEY_Device_InstanceId);
    std::wstring parent_instance_id = usb_device_info::get_device_property_string(dev_info_set, dev_info_data, &DEVPKEY_Device_Parent);

    std::wstring hub_path = usb_device_info::get_device_path(parent_instance_id, &GUID_DEVINTERFACE_USB_HUB);

    // open parent (hub)
    HANDLE hub_handle = CreateFileW(hub_path.c_str(), GENERIC_WRITE, FILE_SHARE_WRITE, nullptr, OPEN_EXISTING, 0, nullptr);
    if (hub_handle == INVALID_HANDLE_VALUE)
        usb_error::throw_error("Cannot open USB hub");

    auto hub_handle_guard = make_scope_exit([hub_handle]() {
        CloseHandle(hub_handle);
    });

    // check for composite device
    std::map<int, std::wstring> children{};
    if (usb_device_info::is_composite_device(dev_info_set, dev_info_data))
        children = enumerate_child_devices(usb_device_info::get_device_property_string_list(dev_info_set, dev_info_data, &DEVPKEY_Device_Children));

    auto path = usb_device_info::get_device_path(instance_id, &GUID_DEVINTERFACE_USB_DEVICE);

    // get device descriptor
    USB_NODE_CONNECTION_INFORMATION_EX conn_info = { 0 };
    conn_info.ConnectionIndex = usb_port_num;
    DWORD size = 0;
    if (!DeviceIoControl(hub_handle, IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX, &conn_info, sizeof(conn_info), &conn_info, sizeof(conn_info), &size, nullptr))
        usb_error::throw_error("Internal error (cannot get device descriptor)");

    // get configuration descriptor
    auto config_desc = usb_device_info::get_descriptor(hub_handle, usb_port_num, USB_CONFIGURATION_DESCRIPTOR_TYPE, 0, 0);

    // Create new device
    std::shared_ptr<usb_device> device(new usb_device(std::move(path), conn_info.DeviceDescriptor.idVendor, conn_info.DeviceDescriptor.idProduct, config_desc, std::move(children)));
    device->set_product_names(
        usb_device_info::get_string(hub_handle, usb_port_num, conn_info.DeviceDescriptor.iManufacturer),
        usb_device_info::get_string(hub_handle, usb_port_num, conn_info.DeviceDescriptor.iProduct),
        usb_device_info::get_string(hub_handle, usb_port_num, conn_info.DeviceDescriptor.iSerialNumber)
    );
    return device;
}

std::map<int, std::wstring> usb_registry::enumerate_child_devices(const std::vector<std::wstring> child_ids) {

    std::map<int, std::wstring> children{};

    for (const std::wstring& child_instance_id : child_ids) {
        HDEVINFO dev_info_set = SetupDiCreateDeviceInfoList(NULL, NULL);
        if (dev_info_set == INVALID_HANDLE_VALUE)
            throw usb_error("internal error (SetupDiCreateDeviceInfoList)", GetLastError());

        // ensure the result id destroyed when the scope is left
        auto dev_info_set_guard = make_scope_exit([dev_info_set]() {
            SetupDiDestroyDeviceInfoList(dev_info_set);
        });

        // get device info for child
        SP_DEVINFO_DATA dev_info_data = { sizeof(dev_info_data) };
        if (!SetupDiOpenDeviceInfoW(dev_info_set, child_instance_id.c_str(), nullptr, 0, &dev_info_data))
            throw usb_error("internal error (SetupDiOpenDeviceInfoW)", GetLastError());
        
        // get first interface number
        int interface_number = usb_device_info::get_first_interface(dev_info_set, &dev_info_data);
        if (interface_number == -1)
            continue;

        // get device interface GUIDs
        auto device_guids = usb_device_info::find_device_interface_guids(dev_info_set, &dev_info_data);

        CLSID clsid{};
        // use GUIDs to get device path
        for (const std::wstring& guid : device_guids) {
            if (CLSIDFromString(guid.c_str(), &clsid) != NOERROR)
                continue;

            try {
                auto device_path = usb_device_info::get_device_path(child_instance_id.c_str(), &clsid);
                children[interface_number] = device_path;
                break;

            } catch (usb_error&) {
                // ignore and try next one
            }
        }
    }

    return children;
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
    } else {
        on_device_disconnected(broadcast->dbcc_name);
    }
    return true;
}

void usb_registry::on_device_connected(const WCHAR* path) {

    usb_device_ptr device;
    try {
        // create empty device information set
        HDEVINFO dev_info_set = SetupDiCreateDeviceInfoList(NULL, NULL);
        if (dev_info_set == INVALID_HANDLE_VALUE)
            throw usb_error("internal error (SetupDiCreateDeviceInfoList)", GetLastError());

        // ensure the result is destroyed when the scope is left
        auto dev_info_set_guard = make_scope_exit([dev_info_set]() {
            SetupDiDestroyDeviceInfoList(dev_info_set);
        });

        // load device information into dev info set
        SP_DEVICE_INTERFACE_DATA dev_intf_data = { sizeof(dev_intf_data) };
        if (!SetupDiOpenDeviceInterfaceW(dev_info_set, path, 0, &dev_intf_data))
            usb_error::throw_error("internal error (SetupDiOpenDeviceInterfaceW)");

        auto dev_intf_data_guard = make_scope_exit([dev_info_set, &dev_intf_data]() {
            SetupDiDeleteDeviceInterfaceData(dev_info_set, &dev_intf_data);
        });

        // load device info data
        SP_DEVINFO_DATA dev_info_data = { sizeof(dev_info_data) };
        if (!SetupDiGetDeviceInterfaceDetailW(dev_info_set, &dev_intf_data, nullptr, 0, nullptr, &dev_info_data)) {
            DWORD err = GetLastError();
            if (err != ERROR_INSUFFICIENT_BUFFER)
                throw usb_error("internal error (SetupDiGetDeviceInterfaceDetailW)", err);
        }

        // create new device
        device = create_device(dev_info_set, &dev_info_data);
        devices.push_back(device);
    }
    catch (const std::exception& e) {
        std::cerr << "Exception while connecting device: " << e.what() << std::endl;
        std::cerr << "Ignoring." << std::endl;
        return;
    }

    // Call callback function
    if (on_connected_callback != nullptr) {
        try {
            on_connected_callback(device);
        }
        catch (const std::exception& e) {
            std::cerr << "Unhandled exception in callback: " << e.what() << std::endl;
        }
        catch (...) {
            std::cerr << "Unhandled exception in callback (not derived from std::exception)" << std::endl;
        }
    }
}

void usb_registry::on_device_disconnected(const WCHAR* path) {

    usb_device_ptr device;
    try {
        // find device in device list
        auto it = std::find_if(devices.cbegin(), devices.cend(), [path](auto device) { return lstrcmpiW(path, device->device_path_.c_str()) == 0; });
        if (it == devices.cend())
            return; // not part of the device list

        // remove from device list
        device = *it;
        devices.erase(it);
    }
    catch (const std::exception& e) {
        std::cerr << "Exception while disconnecting device: " << e.what() << std::endl;
        std::cerr << "Ignoring." << std::endl;
        return;
    }
        
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
