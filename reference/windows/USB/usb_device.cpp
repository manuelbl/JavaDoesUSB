//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#include "usb_device.hpp"
#include "usb_error.hpp"
#include "usb_iostream.hpp"
#include "device_info_set.h"
#include "scope.hpp"
#include "config_parser.hpp"
#include "usb_registry.hpp"

#include <devpkey.h>

#include <chrono>
#include <cstdio>
#include <regex>
#include <thread>

usb_device::usb_device(usb_registry* registry, std::wstring&& device_path, int vendor_id, int product_id, const std::vector<uint8_t>& config_desc, bool is_composite)
: registry_(registry), vendor_id_(vendor_id), product_id_(product_id), is_open_(false), device_path_(std::move(device_path)), is_composite_(is_composite) {

    config_parser parser{};
    parser.parse(config_desc.data(), static_cast<int>(config_desc.size()));
    interfaces_ = std::move(parser.interfaces);
    functions_ = std::move(parser.functions);

    build_handles(device_path_);
}

void usb_device::set_product_names(const std::string& manufacturer, const std::string& product, const std::string& serial_number) {
    manufacturer_ = manufacturer;
    product_ = product;
    serial_number_ = serial_number;
}

void usb_device::build_handles(const std::wstring& device_path) {
    for (const usb_interface& intf : interfaces_) {
        int intf_number = intf.number();
        auto function = get_function(intf_number);

        std::wstring path;
        if (intf_number == 0)
            path = device_path;

        interface_handles_.push_back(interface_handle(intf_number, function->first_interface(), std::move(path)));
    }
}

usb_device::~usb_device() {
    close();
}

std::string usb_device::description() const {
    
    const char* fmt = "VID: 0x%04x, PID: 0x%04x, manufacturer: %s, product: %s, serial: %s";
    int len = snprintf(nullptr, 0, fmt,
                       vendor_id_, product_id_, manufacturer_.c_str(), product_.c_str(), serial_number_.c_str());
    std::string desc;
    desc.resize((size_t) len + 1);
    
    snprintf(&desc[0], desc.size(), fmt,
             vendor_id_, product_id_, manufacturer_.c_str(), product_.c_str(), serial_number_.c_str());

    return desc;
}

const std::vector<usb_interface>& usb_device::interfaces() const {
    return interfaces_;
}

const usb_interface& usb_device::get_interface(int interface_number) const {
    for (const usb_interface& intf : interfaces_) {
        if (intf.number() == interface_number)
            return intf;
    }
    
    return usb_interface::invalid;
}

const usb_endpoint& usb_device::get_endpoint(usb_direction direction, int endpoint_number) const {
    for (const usb_interface& intf : interfaces_) {
        for (const usb_endpoint& ep : intf.alternate().endpoints()) {
            if (ep.direction() == direction && ep.number() == endpoint_number)
                return ep;
        }
    }
    
    return usb_endpoint::invalid;
}


bool usb_device::is_open() const {
    return is_open_;
}

void usb_device::open() {
    if (is_open())
        throw usb_error("USB device is already open", 0);
    
    is_open_ = true;
}

void usb_device::close() {
    if (!is_open())
        return;

    for (auto& itf : interfaces_)
        if (itf.is_claimed())
            release_interface(itf.number());
    
    is_open_ = false;
}

void usb_device::claim_interface(int interface_number) {
    // When a device is plugged in, a notification is sent. For composite devices, it is a notification
    // that the composite device is ready. Each composite function will be registered separately and
    // the related information will be available with a delay. So for composite functions, several
    // retries might be needed until the device path is available.
    int num_retries = 30; // 30 x 100ms
    while (true) {
        if (try_claim_interface(interface_number))
            return; // success

        num_retries -= 1;
        if (num_retries == 0)
            throw usb_error("claiming interface failed (function has no device interface GUID/path, might be missing WinUSB driver)");

        // sleep and retry
        std::cerr << "Sleeping for 100ms..." << std::endl;
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
}

bool usb_device::try_claim_interface(int interface_number) {

    if (!is_open())
        throw usb_error("USB device is not open");

    usb_interface* intf = get_intf_ptr(interface_number);
    if (intf == nullptr)
        throw usb_error("no such interface");
    if (intf->is_claimed())
        throw usb_error("interface has already been claimed");
    
    interface_handle* intf_handle = get_interface_handle(interface_number);
    interface_handle* first_intf_handle = get_interface_handle(intf_handle->first_interface_num);

    // both the device and the first interface must be opened for any interface belonging to the same function
    if (first_intf_handle->device_handle == nullptr) {
        auto device_path = get_interface_device_path(first_intf_handle->interface_num);
        if (device_path.empty())
            return false;

        std::wcerr << "opening device " << device_path << std::endl;

        // open device
        first_intf_handle->device_handle = CreateFileW(device_path.c_str(),
            GENERIC_WRITE | GENERIC_READ,
            FILE_SHARE_WRITE | FILE_SHARE_READ,
            nullptr,
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
            nullptr);
        if (first_intf_handle->device_handle == INVALID_HANDLE_VALUE)
            usb_error::throw_error("failed to claim interface (cannot open USB device)");

        // open first interface
        if (!WinUsb_Initialize(first_intf_handle->device_handle, &first_intf_handle->winusb_handle)) {
            auto err = GetLastError();
            CloseHandle(first_intf_handle->device_handle);
            first_intf_handle->device_handle = nullptr;
            throw usb_error("failed to claim interface (cannot open associated interface)", err);
        }

        registry_->add_to_completion_port(first_intf_handle->device_handle);
    }

    // open associated interface
    if (intf_handle != first_intf_handle) {
        if (!WinUsb_GetAssociatedInterface(first_intf_handle->winusb_handle, intf_handle->interface_num - first_intf_handle->interface_num - 1, &intf_handle->winusb_handle))
            throw usb_error("cannot open associated interface", GetLastError());
    }

    first_intf_handle->device_open_count += 1;
    intf->set_claimed(true);
    return true;
}

void usb_device::release_interface(int interface_number) {

    if (!is_open())
        throw usb_error("USB device is not open");

    usb_interface* intf = get_intf_ptr(interface_number);
    if (intf == nullptr)
        throw usb_error("no such interface");
    if (!intf->is_claimed())
        throw usb_error("interface has not been claimed");

    interface_handle* intf_handle = get_interface_handle(interface_number);
    interface_handle* first_intf_handle = get_interface_handle(intf_handle->first_interface_num);

    intf->set_claimed(false);

    if (intf_handle != first_intf_handle) {
        // close assicated interface
        if (!WinUsb_Free(intf_handle->winusb_handle))
            throw usb_error("failed to release associated interface", GetLastError());
        intf_handle->winusb_handle = nullptr;
    }

    // close device if needed
    first_intf_handle->device_open_count -= 1;
    if (first_intf_handle->device_open_count == 0) {
        WinUsb_Free(first_intf_handle->winusb_handle);
        CloseHandle(first_intf_handle->device_handle);
        first_intf_handle->device_handle = nullptr;
    }
}

std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int timeout) {

    auto winusb_handle = check_valid_endpoint(usb_direction::in, endpoint_number)->winusb_handle;
    UCHAR endpoint_address = ep_address(usb_direction::in, endpoint_number);
    auto endpoint = get_endpoint_ptr(usb_direction::in, endpoint_number);

    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(winusb_handle, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    std::vector<uint8_t> data(endpoint->packet_size());

    DWORD len = 0;
    if (!WinUsb_ReadPipe(winusb_handle, endpoint_address, static_cast<PUCHAR>(data.data()), endpoint->packet_size(), &len, nullptr))
        usb_error::throw_error("Cannot receive from USB endpoint");

    data.resize(len);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int len, int timeout) {
    if (len < 0 || len > data.size())
        len = static_cast<int>(data.size());

    auto winusb_handle = check_valid_endpoint(usb_direction::out, endpoint_number)->winusb_handle;
    UCHAR endpoint_address = ep_address(usb_direction::out, endpoint_number);

    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(winusb_handle, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    DWORD tlen = 0;
    if (!WinUsb_WritePipe(winusb_handle, endpoint_address, const_cast<PUCHAR>(data.data()), len, &tlen, nullptr))
        usb_error::throw_error("Failed to transmit to USB endpoint");
}

int usb_device::control_transfer_core(const usb_control_request &request, uint8_t* data, int timeout) {
    
    if (!is_open())
        throw usb_error("USB device is not open");

    auto winusb_handle = get_control_transfer_interface_handle(request)->winusb_handle;
    
    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(winusb_handle, 0, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    WINUSB_SETUP_PACKET setup_packet = { 0 };
    setup_packet.RequestType = request.bmRequestType;
    setup_packet.Request = request.bRequest;
    setup_packet.Value = request.wValue;
    setup_packet.Index = request.wIndex;
    setup_packet.Length = request.wLength;

    DWORD len = 0;
    if (!WinUsb_ControlTransfer(winusb_handle, setup_packet, data, request.wLength, &len, nullptr))
        usb_error::throw_error("Control transfer failed");

    return len;
}

usb_device::interface_handle* usb_device::get_control_transfer_interface_handle(const usb_control_request& request) {
    usb_request_type recipient = static_cast<usb_request_type>(static_cast<uint8_t>(request.bmRequestType) | 0x03);
    int recipient_index = request.wIndex & 0xff;

    int intf_num = -1;
    if (recipient == usb_request_type::recipient_interface) {

        intf_num = recipient_index;

    } else if (recipient == usb_request_type::recipient_endpoint) {

        int endpoint_number = recipient_index & 0x7f;
        usb_direction direction = static_cast<usb_direction>(recipient_index & 0x80);
        if (endpoint_number != 0) {
            usb_interface* intf = get_endpoint_interface(direction, endpoint_number);
            if (intf == nullptr )
                throw usb_error("invalid endpoint number for control request");
            intf_num = intf->number();
        }

    }

    // for control transfer to device, use any claimed interface
    if (intf_num < 0) {
        for (auto& intf : interfaces_) {
            if (intf.is_claimed()) {
                intf_num = intf.number();
                break;
            }
        }
    }

    if (intf_num >= 0) {
        usb_interface* intf = get_intf_ptr(intf_num);
        if (intf == nullptr)
            throw usb_error("invalid interface number for control request");
        if (!intf->is_claimed())
            throw usb_error("interface for control request has not been claimed");
        return get_interface_handle(intf_num);
    }

    throw usb_error("no interface has been claimed");
}

void usb_device::control_transfer(const usb_control_request& request, int timeout) {
    if (request.wLength != 0)
        throw usb_error("'control_transfer' only supports request without data phase but 'wLength' != 0");
    
    control_transfer_core(request, nullptr, timeout);
}

void usb_device::control_transfer_out(const usb_control_request& request, const std::vector<uint8_t>& data, int timeout) {
    if ((request.bmRequestType & 0x80) != 0)
        throw usb_error("direction mismatch between 'control_transfer_out' and direction bit in 'bmRequestType'");
    
    control_transfer_core(request, const_cast<uint8_t*>(data.data()), timeout);
}

std::vector<uint8_t> usb_device::control_transfer_in(const usb_control_request& request, int timeout) {
    if ((request.bmRequestType & 0x80) == 0)
        throw usb_error("direction mismatch between 'control_transfer_in' and direction bit in 'bmRequestType'");
    
    std::vector<uint8_t> data(request.wLength);
    control_transfer_core(request, data.data(), timeout);
    data.resize(request.wLength);
    return data;
}

usb_composite_function* usb_device::get_function(int intf_number) {
    auto iter = std::find_if(functions_.begin(), functions_.end(), [intf_number](const usb_composite_function& f) {
        return intf_number >= f.first_interface() && intf_number < f.first_interface() + f.num_interfaces();
    });
    
    if (iter == functions_.end())
        return nullptr;

    return &*iter;
}

usb_device::interface_handle* usb_device::get_interface_handle(int intf_number) {
    for (auto& intf : interface_handles_)
        if (intf.interface_num == intf_number)
            return &intf;

    return nullptr;
}

usb_interface* usb_device::get_intf_ptr(int intf_number) {
    for (auto& intf : interfaces_)
        if (intf.number() == intf_number)
            return &intf;

    return nullptr;
}

usb_interface* usb_device::get_endpoint_interface(usb_direction direction, int endpoint_number) {
    for (auto& intf : interfaces_)
        for (auto& ep : intf.alternate().endpoints())
            if (ep.number() == endpoint_number && ep.direction() == direction)
                return &intf;

    return nullptr;
}

const usb_endpoint* usb_device::get_endpoint_ptr(usb_direction direction, int endpoint_number) {
    for (auto& intf : interfaces_)
        for (auto& ep : intf.alternate().endpoints())
            if (ep.number() == endpoint_number && ep.direction() == direction)
                return &ep;

    return nullptr;
}

usb_device::interface_handle* usb_device::check_valid_endpoint(usb_direction direction, int endpoint_number) {

    if (!is_open())
        throw usb_error("USB device is not open");

    const usb_endpoint* ep = get_endpoint_ptr(direction, endpoint_number);
    if (ep == nullptr)
        throw usb_error("no such endpoint");
    if (ep->transfer_type() != usb_transfer_type::bulk && ep->transfer_type() != usb_transfer_type::interrupt)
        throw usb_error("invalid transfer type for operation");

    usb_interface* intf = get_endpoint_interface(direction, endpoint_number);
    if (!intf->is_claimed())
        throw usb_error("endpoint's interface has not been claimed");

    return get_interface_handle(intf->number());
}

std::unique_ptr<std::istream> usb_device::open_input_stream(int endpoint_number) {
    return std::unique_ptr<std::istream>(new usb_istream(registry_->get_shared_ptr(this), endpoint_number));
}

std::unique_ptr<std::ostream> usb_device::open_output_stream(int endpoint_number) {
    return std::unique_ptr<std::ostream>(new usb_ostream(registry_->get_shared_ptr(this), endpoint_number));
}

void usb_device::add_completion_handler(OVERLAPPED* overlapped, usb_io_callback* completion_handler) {
    registry_->add_completion_handler(overlapped, completion_handler);
}

void usb_device::remove_completion_handler(OVERLAPPED* overlapped) {
    registry_->remove_completion_handler(overlapped);
}

void usb_device::configure_for_async_io(usb_direction direction, int endpoint_number) {
    auto winusb_handle = check_valid_endpoint(direction, endpoint_number)->winusb_handle;
    UCHAR endpoint_address = ep_address(direction, endpoint_number);

    ULONG timeout = 0;
    if (!WinUsb_SetPipePolicy(winusb_handle, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(timeout), &timeout))
        usb_error::throw_error("Failed to set endpoint timeout");

    UCHAR raw_io = 1;
    if (!WinUsb_SetPipePolicy(winusb_handle, endpoint_address, RAW_IO, sizeof(raw_io), &raw_io))
        usb_error::throw_error("Failed to set endpoint for raw IO");
}

void usb_device::submit_transfer_in(int endpoint_number, uint8_t* buffer, int buffer_len, OVERLAPPED* overlapped) {

    auto winusb_handle = check_valid_endpoint(usb_direction::in, endpoint_number)->winusb_handle;
    UCHAR endpoint_address = ep_address(usb_direction::in, endpoint_number);

    if (!WinUsb_ReadPipe(winusb_handle, endpoint_address, buffer, buffer_len, nullptr, overlapped)) {
        DWORD err = GetLastError();
        if (err == ERROR_IO_PENDING)
            return;
        throw usb_error("Failed to submit transfer IN", err);
    }
}

void usb_device::submit_transfer_out(int endpoint_number, uint8_t* data, int data_len, OVERLAPPED* overlapped) {

    auto winusb_handle = check_valid_endpoint(usb_direction::out, endpoint_number)->winusb_handle;
    UCHAR endpoint_address = ep_address(usb_direction::out, endpoint_number);

    if (!WinUsb_WritePipe(winusb_handle, endpoint_address, data, data_len, nullptr, overlapped)) {
        DWORD err = GetLastError();
        if (err == ERROR_IO_PENDING)
            return;
        throw usb_error("Failed to submit transfer OUT", err);
    }
}

void usb_device::cancel_transfer(usb_direction direction, int endpoint_number, OVERLAPPED* overlapped) {
    auto handle_info = check_valid_endpoint(direction, endpoint_number);

    if (!CancelIoEx(handle_info->device_handle, overlapped))
        usb_error::throw_error("Error on cancelling transfer");
}

std::wstring usb_device::get_interface_device_path(int interface_num) {
    if (!is_composite_)
        return device_path_;

    auto it = interface_device_paths_.find(interface_num);
    if (it != interface_device_paths_.end())
        return it->second;

    auto dev_info_set = device_info_set::of_path(device_path_);

    auto children_instance_ids = dev_info_set.get_device_property_string_list(DEVPKEY_Device_Children);

    std::wcerr << "children IDs: ";
    for (auto it = children_instance_ids.begin(); it < children_instance_ids.end(); it++) {
        if (it != children_instance_ids.begin())
            std::wcerr << ", ";
        std::wcerr << *it;
    }
    std::wcerr << std::endl;

    std::wstring child_path;
    for (auto& child_id : children_instance_ids) {
        child_path = get_child_device_path(child_id, interface_num);
        if (!child_path.empty())
            return child_path;
    }

    return {}; // retry later
}

std::wstring usb_device::get_child_device_path(const std::wstring& child_id, int interface_num) {

    auto dev_info_set = device_info_set::of_instance(child_id);

    auto hardware_ids = dev_info_set.get_device_property_string_list(DEVPKEY_Device_HardwareIds);
    if (hardware_ids.empty()) {
        std::wcerr << "child device " << child_id << " has no hardware IDs" << std::endl;
        return {}; // continue with next child
    }

    auto intf_num = extract_interface_number(hardware_ids);
    if (intf_num == -1) {
        std::wcerr << "child device " << child_id << " has no interface number" << std::endl;
        return {}; // continue with next child
    }

    if (intf_num != interface_num)
        return {}; // continue with next child

    auto device_path = dev_info_set.get_device_path_by_guid(child_id);
    if (device_path.empty()) {
        std::wcerr << "child device " << child_id << " has no device path" << std::endl;
        throw usb_error("claiming interface failed (function has no device interface GUID/path, might be missing WinUSB driver)");
    }

    std::wcerr << "child device: interface=" << intf_num << ", device path=" << device_path << std::endl;
    interface_device_paths_[interface_num] = device_path;
    return device_path;  // success
}

static const std::wregex multiple_interface_id_pattern(L"USB\\\\VID_[0-9A-Fa-f]{4}&PID_[0-9A-Fa-f]{4}&MI_([0-9A-Fa-f]{2})");

int usb_device::extract_interface_number(const std::vector<std::wstring>& hardware_ids) {
    // Also see https://docs.microsoft.com/en-us/windows-hardware/drivers/install/standard-usb-identifiers#multiple-interface-usb-devices

    for (auto& id : hardware_ids) {
        auto matches = std::wsmatch{};
        if (std::regex_search(id, matches, multiple_interface_id_pattern))
            return std::stoul(matches[1].str(), nullptr, 16);
    }

    return -1;
}


// --- interface_handle

usb_device::interface_handle::interface_handle(int intf_num, int first_num, std::wstring&& path)
    : interface_num(intf_num), first_interface_num(first_num),
        device_handle(nullptr), winusb_handle(nullptr), device_open_count(0) { }
