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
#include "device_info_set.h"
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
    is_device_list_ready(false), monitor_thread_id_(0), message_window(nullptr),
    async_io_completion_port(nullptr) {
}

usb_registry::~usb_registry() {
    SendMessage(message_window, WM_CLOSE, 0, 0);
    monitor_thread.join();
    
    if (async_io_completion_port != nullptr) {
        PostQueuedCompletionStatus(async_io_completion_port, 0, -1, nullptr);
        async_io_thread.join();
        CloseHandle(async_io_completion_port);
    }
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
    
    std::unique_lock wait_lock(monitor_mutex);
    monitor_condition.wait(wait_lock, [this] { return is_device_list_ready; });
}

static const LPCWSTR CLASS_NAME = L"USB_MONITOR";
static const LPCWSTR WINDOW_NAME = L"USB device monitor";

void usb_registry::monitor() {

    monitor_thread_id_ = GetCurrentThreadId();
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
    if (notify_handle == nullptr)
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

    // get device information set of all present USB devices
    auto dev_info_set = device_info_set::of_present_devices(GUID_DEVINTERFACE_USB_DEVICE);

    std::map<std::wstring, HANDLE> hub_handles{};

    auto hub_handle_guard = make_scope_exit([&hub_handles]() {
        for (auto& hub : hub_handles)
            CloseHandle(hub.second);
    });

    // iterate over the set
    while (dev_info_set.next()) {

        auto instance_id = dev_info_set.get_device_property_string(DEVPKEY_Device_InstanceId);
        auto device_path = device_info_set::get_device_path(instance_id, GUID_DEVINTERFACE_USB_DEVICE);

        std::wcerr << "Device present: InstanceId=" << instance_id << ", DevicePath=" << device_path << std::endl;

        // create new device
        auto device = create_device_from_device_info(dev_info_set, std::move(device_path), hub_handles);
        devices.push_back(device);
    }
}

std::shared_ptr<usb_device> usb_registry::create_device_from_device_info(device_info_set& dev_info_set, std::wstring&& device_path, std::map<std::wstring, HANDLE>& hub_handles) {

    DWORD usb_port_num = dev_info_set.get_device_property_int(DEVPKEY_Device_Address);
    std::wstring parent_instance_id = dev_info_set.get_device_property_string(DEVPKEY_Device_Parent);
    std::wstring hub_path = device_info_set::get_device_path(parent_instance_id, GUID_DEVINTERFACE_USB_HUB);

    // open parent (hub) if not open
    HANDLE hub_handle;
    auto it = hub_handles.find(hub_path);
    if (it != hub_handles.end()) {
        hub_handle = it->second;
    }
    else {
        hub_handle = CreateFileW(hub_path.c_str(), GENERIC_WRITE, FILE_SHARE_WRITE, nullptr, OPEN_EXISTING, 0, nullptr);
        if (hub_handle == INVALID_HANDLE_VALUE)
            usb_error::throw_error("Cannot open USB hub");
        hub_handles[hub_path] = hub_handle;
    }

    return create_device(std::move(device_path), dev_info_set.is_composite_device(), hub_handle, usb_port_num);
}

std::shared_ptr<usb_device> usb_registry::create_device(std::wstring&& device_path, bool is_composite, HANDLE hub_handle, DWORD usb_port_num) {

    // get device descriptor
    USB_NODE_CONNECTION_INFORMATION_EX conn_info = { 0 };
    conn_info.ConnectionIndex = usb_port_num;
    DWORD size = 0;
    if (!DeviceIoControl(hub_handle, IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX, &conn_info, sizeof(conn_info), &conn_info, sizeof(conn_info), &size, nullptr))
        usb_error::throw_error("Internal error (cannot get device descriptor)");

    int vendorId = conn_info.DeviceDescriptor.idVendor;
    int productId = conn_info.DeviceDescriptor.idProduct;

    // get configuration descriptor
    auto config_desc = get_descriptor(hub_handle, usb_port_num, USB_CONFIGURATION_DESCRIPTOR_TYPE, 0, 0);

    // Create new device
    // usb_registry* registry, std::wstring&& device_path, int vendor_id, int product_id, const std::vector<uint8_t>& config_desc, std::map<int, std::wstring>&& children
    std::shared_ptr<usb_device> device(new usb_device(this, std::move(device_path), vendorId, productId, config_desc, is_composite));
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
        LONG_PTR result = SetWindowLongPtrW(hWnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(self));
        break;
    }

    case WM_DESTROY: {
        LONG_PTR result = SetWindowLongPtrW(hWnd, GWLP_USERDATA, NULL);
        PostQuitMessage(0);
        break;
    }
    }

    if (self != nullptr && self->handle_message(hWnd, uMsg, wParam, lParam))
        return 0;

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
        std::wcerr << "Device added: DevicePath=" << broadcast->dbcc_name << std::endl;
        on_device_connected(broadcast->dbcc_name);
    } else {
        std::wcerr << "Device removed: DevicePath=" << broadcast->dbcc_name << std::endl;
        on_device_disconnected(broadcast->dbcc_name);
    }
    return true;
}

void usb_registry::on_device_connected(const WCHAR* path) {

    usb_device_ptr device;
    try {
        // create device information set
        auto dev_info_set = device_info_set::of_path(path);

        std::map<std::wstring, HANDLE> hub_handles{};
        auto hub_handle_guard = make_scope_exit([&hub_handles]() {
            for (auto& hub : hub_handles)
                CloseHandle(hub.second);
        });

        // create new device
        device = create_device_from_device_info(dev_info_set, path, hub_handles);
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

std::shared_ptr<usb_device> usb_registry::get_shared_ptr(usb_device* device) {
    auto it = std::find_if(devices.cbegin(), devices.cend(), [device](auto dev) { return dev.get() == device; });
    if (it == devices.cend())
        return nullptr;

    return *it;
}

void usb_registry::async_io_run() {

    while (true) {
        OVERLAPPED* overlapped = nullptr;
        DWORD num_bytes = 0;
        ULONG_PTR completion_key = 0;
        if (!GetQueuedCompletionStatus(async_io_completion_port, &num_bytes, &completion_key, &overlapped, INFINITE) && overlapped == nullptr)
            usb_error::throw_error("internal error (GetQueuedCompletionStatus)");

        if (overlapped == nullptr)
            return; // registry is closing

        usb_io_callback* completion_handler = get_completion_handler(overlapped);
        if (overlapped == nullptr)
            continue; // might be completion from synchronous operation

        (*completion_handler)();
    }
}

void usb_registry::add_to_completion_port(HANDLE handle) {
    HANDLE port_handle = CreateIoCompletionPort(handle, async_io_completion_port, 0xd03fbc01, 0);
    if (port_handle == nullptr)
        usb_error::throw_error("internal error (CreateIoCompletionPort)");

    if (async_io_completion_port == nullptr) {
        async_io_completion_port = port_handle;
        async_io_thread = std::thread(&usb_registry::async_io_run, this);
    }
}

void usb_registry::add_completion_handler(OVERLAPPED* overlapped, usb_io_callback* completion_handler) {
    std::lock_guard lock(async_io_mutex);

    async_io_completion_handlers.insert(std::pair(overlapped, completion_handler));
}

void usb_registry::remove_completion_handler(OVERLAPPED* overlapped) {
    std::lock_guard lock(async_io_mutex);

    async_io_completion_handlers.erase(overlapped);
}

usb_io_callback* usb_registry::get_completion_handler(OVERLAPPED* overlapped) {
    std::lock_guard lock(async_io_mutex);

    auto it = async_io_completion_handlers.find(overlapped);
    if (it == async_io_completion_handlers.end())
        return nullptr;

    return it->second;
}

std::vector<uint8_t> usb_registry::get_descriptor(HANDLE hub_handle, ULONG usb_port_num, uint16_t descriptor_type, int index, int language_id, int request_size) {
    int size = sizeof(USB_DESCRIPTOR_REQUEST) + (request_size != 0 ? request_size : 255);
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
    int data_size = bytesReturned - sizeof(USB_DESCRIPTOR_REQUEST);

    if (data_size <= 2)
        throw usb_error("invalid descriptor");

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
    if (data_size < expected_size) {
        if (request_size != 0)
            throw usb_error("Unexpected descriptor size");

        // repeat with larger size
        return get_descriptor(hub_handle, usb_port_num, descriptor_type, index, language_id, expected_size);
    }

    return std::vector<uint8_t>(descriptor_request->Data, descriptor_request->Data + data_size);
}

std::string usb_registry::get_string(HANDLE hub_handle, ULONG usb_port_num, int index) {
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
