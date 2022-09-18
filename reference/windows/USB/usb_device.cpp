//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#include "usb_device.hpp"
#include "usb_error.hpp"
#include "scope.hpp"

#include <cstdio>

usb_device::usb_device(const std::wstring& device_path, int vendor_id, int product_id)
: device_path_(device_path), device_handle_(nullptr), interface_handle_(nullptr),
    vendor_id_(vendor_id), product_id_(product_id), is_open_(false) {
    
    //manufacturer_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBVendorString));
    //product_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBProductString));
    //serial_number_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBSerialNumberString));
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
    
    if (interface_handle_ != nullptr)
        release_interface();
    
    is_open_ = false;
}

void usb_device::claim_interface(int interface_number) {
    
    if (interface_handle_ != nullptr)
        throw usb_error("an interface has already been claimed");

    auto handle = CreateFileW(device_path_.c_str(),
        GENERIC_WRITE | GENERIC_READ,
        FILE_SHARE_WRITE | FILE_SHARE_READ,
        nullptr,
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
        nullptr);
    if (handle == INVALID_HANDLE_VALUE)
        usb_error::throw_error("Cannot open USB device");


    if (!WinUsb_Initialize(handle, &interface_handle_)) {
        CloseHandle(handle);
        usb_error::throw_error("Cannot open USB device");
        return;
    }

    device_handle_ = handle;
}

void usb_device::release_interface() {
    if (interface_handle_ == nullptr)
        throw usb_error("no interface has been claimed");

    if (interface_handle_ != nullptr) {
        WinUsb_Free(interface_handle_);
        interface_handle_ = nullptr;
    }
    if (device_handle_ != nullptr) {
        CloseHandle(device_handle_);
        device_handle_ = nullptr;
    }
}

std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int data_len, int timeout) {
    if (interface_handle_ == nullptr)
        throw usb_error("no interface has been claimed");

    UCHAR endpoint_address = endpoint_number + 128;

    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(interface_handle_, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    std::vector<uint8_t> data(data_len);

    DWORD len = 0;
    if (!WinUsb_ReadPipe(interface_handle_, endpoint_address, static_cast<PUCHAR>(data.data()), data_len, &len, nullptr))
        usb_error::throw_error("Cannot receive from USB endpoint");

    data.resize(len);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int timeout) {
    if (interface_handle_ == nullptr)
        throw usb_error("no interface has been claimed");

    UCHAR endpoint_address = endpoint_number;

    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(interface_handle_, endpoint_address, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    DWORD len = 0;
    if (!WinUsb_WritePipe(interface_handle_, endpoint_address, const_cast<PUCHAR>(data.data()), static_cast<ULONG>(data.size()), &len, nullptr))
        usb_error::throw_error("Failed to transmit to USB endpoint");
}

int usb_device::control_transfer_core(const usb_control_request &request, uint8_t* data, int timeout) {
    
    if (!is_open())
        throw usb_error("USB device is not open");
    
    ULONG value = timeout;
    if (!WinUsb_SetPipePolicy(interface_handle_, 0, PIPE_TRANSFER_TIMEOUT, sizeof(value), &value))
        usb_error::throw_error("Failed to set endpoint timeout");

    WINUSB_SETUP_PACKET setup_packet = { 0 };
    setup_packet.RequestType = request.bmRequestType;
    setup_packet.Request = request.bRequest;
    setup_packet.Value = request.wValue;
    setup_packet.Index = request.wIndex;
    setup_packet.Length = request.wLength;

    DWORD len = 0;
    if (!WinUsb_ControlTransfer(interface_handle_, setup_packet, data, request.wLength, &len, nullptr))
        usb_error::throw_error("Control transfer failed");

    return len;
}

void usb_device::control_transfer(const usb_control_request& request, int timeout ) {
    if (request.wLength != 0)
        throw usb_error("'control_transfer' only supports request without data phase but 'wLength' != 0");
    
    control_transfer_core(request, nullptr, timeout);
}

void usb_device::control_transfer_out(const usb_control_request& request, const std::vector<uint8_t>& data, int timeout ) {
    if ((request.bmRequestType & 0x80) != 0)
        throw usb_error("direction mismatch between 'control_transfer_out' and direction bit in 'bmRequestType'");
    
    control_transfer_core(request, const_cast<uint8_t*>(data.data()), timeout);
}

std::vector<uint8_t> usb_device::control_transfer_in(const usb_control_request& request, int timeout ) {
    if ((request.bmRequestType & 0x80) == 0)
        throw usb_error("direction mismatch between 'control_transfer_in' and direction bit in 'bmRequestType'");
    
    std::vector<uint8_t> data(request.wLength);
    control_transfer_core(request, data.data(), timeout);
    data.resize(request.wLength);
    return data;
}
