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
#include "scope.hpp"
#include "config_parser.hpp"

#include <cstdio>

usb_device::usb_device(std::wstring&& device_path, int vendor_id, int product_id, const std::vector<uint8_t>& config_desc, std::map<int, std::wstring>&& children)
: vendor_id_(vendor_id), product_id_(product_id), is_open_(false), device_path_(std::move(device_path)) {

    config_parser parser{};
    parser.parse(config_desc.data(), static_cast<int>(config_desc.size()));
    interfaces_ = std::move(parser.interfaces);
    functions_ = std::move(parser.functions);

    build_handles(device_path_, std::move(children));
}

void usb_device::set_product_names(const std::string& manufacturer, const std::string& product, const std::string& serial_number) {
    manufacturer_ = manufacturer;
    product_ = product;
    serial_number_ = serial_number;
}

void usb_device::build_handles(const std::wstring& device_path, std::map<int, std::wstring>&& children) {
    for (const usb_interface& intf : interfaces_) {
        int intf_number = intf.number();
        auto function = get_function(intf_number);

        std::wstring path;
        if (function->first_interface() == intf_number) {
            if (children.size() > 0) {
                path = std::move(children[intf_number]);
            } else {
                path = device_path;
            }
        }

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
        throw new usb_error("USB device is already open", 0);
    
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

    if (!is_open())
        throw usb_error("USB device is not open");

    usb_interface* intf = get_intf_ptr(interface_number);
    if (intf == nullptr)
        throw usb_error("no such interface");
    if (intf->is_claimed())
        throw usb_error("interface has already been claimed");
    
    interface_handle* intf_handle = get_interface_handle(interface_number);
    interface_handle* first_intf_handle = get_interface_handle(intf_handle->first_interface_num);

    // open device if needed
    if (first_intf_handle->device_handle == nullptr) {
        first_intf_handle->device_handle = CreateFileW(first_intf_handle->device_path.c_str(),
            GENERIC_WRITE | GENERIC_READ,
            FILE_SHARE_WRITE | FILE_SHARE_READ,
            nullptr,
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
            nullptr);
        if (first_intf_handle->device_handle == INVALID_HANDLE_VALUE)
            usb_error::throw_error("Cannot open USB device");
    }

    // open interface
    if (!WinUsb_Initialize(first_intf_handle->device_handle, &intf_handle->intf_handle)) {
        if (first_intf_handle->device_open_count == 0) {
            CloseHandle(first_intf_handle->device_handle);
            first_intf_handle->device_handle = nullptr;
        }
        usb_error::throw_error("Cannot open USB device");
        return;
    }

    first_intf_handle->device_open_count += 1;
    intf->set_claimed(true);
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

    // close interface
    WinUsb_Free(intf_handle->intf_handle);
    intf_handle->intf_handle = nullptr;
    intf->set_claimed(false);

    // close device if needed
    first_intf_handle->device_open_count -= 1;
    if (first_intf_handle->device_open_count == 0) {
        CloseHandle(first_intf_handle->device_handle);
        first_intf_handle->device_handle = nullptr;
    }
}

std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int timeout) {

    auto intf_handle = check_valid_endpoint(usb_direction::in, endpoint_number)->intf_handle;
    UCHAR endpoint_address = ep_address(usb_direction::in, endpoint_number);
    auto endpoint = get_endpoint_ptr(usb_direction::in, endpoint_number);

    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(intf_handle, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    std::vector<uint8_t> data(endpoint->packet_size());

    DWORD len = 0;
    if (!WinUsb_ReadPipe(intf_handle, endpoint_address, static_cast<PUCHAR>(data.data()), endpoint->packet_size(), &len, nullptr))
        usb_error::throw_error("Cannot receive from USB endpoint");

    data.resize(len);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int len, int timeout) {
    if (len < 0 || len > data.size())
        len = static_cast<int>(data.size());

    auto intf_handle = check_valid_endpoint(usb_direction::out, endpoint_number)->intf_handle;
    UCHAR endpoint_address = ep_address(usb_direction::out, endpoint_number);

    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(intf_handle, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    DWORD tlen = 0;
    if (!WinUsb_WritePipe(intf_handle, endpoint_address, const_cast<PUCHAR>(data.data()), len, &tlen, nullptr))
        usb_error::throw_error("Failed to transmit to USB endpoint");
}

int usb_device::control_transfer_core(const usb_control_request &request, uint8_t* data, int timeout) {
    
    if (!is_open())
        throw usb_error("USB device is not open");

    auto handle = get_control_transfer_interface_handle(request)->intf_handle;
    
    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(handle, 0, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    WINUSB_SETUP_PACKET setup_packet = { 0 };
    setup_packet.RequestType = request.bmRequestType;
    setup_packet.Request = request.bRequest;
    setup_packet.Value = request.wValue;
    setup_packet.Index = request.wIndex;
    setup_packet.Length = request.wLength;

    DWORD len = 0;
    if (!WinUsb_ControlTransfer(handle, setup_packet, data, request.wLength, &len, nullptr))
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


// --- interface_handle

usb_device::interface_handle::interface_handle(int intf_num, int first_num, std::wstring&& path)
    : interface_num(intf_num), first_interface_num(first_num), device_path(std::move(path)),
        device_handle(nullptr), intf_handle(nullptr), device_open_count(0) { }
