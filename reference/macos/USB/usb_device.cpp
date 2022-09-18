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
#include "iokit_helper.hpp"

#include <IOKit/usb/IOUSBLib.h>

#include <cstdio>

usb_device::usb_device(io_service_t service, IOUSBDeviceInterface** device, uint64_t entry_id, int vendor_id, int product_id)
: entry_id_(entry_id), device_(device), interface_(nullptr), vendor_id_(vendor_id), product_id_(product_id), is_open_(false) {
    
    (*device)->AddRef(device);
    
    manufacturer_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBVendorString));
    product_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBProductString));
    serial_number_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBSerialNumberString));
}

usb_device::~usb_device() {
    close();
}

std::string usb_device::description() const {
    
    const char* fmt = "VID: 0x%04x, PID: 0x%04x, manufacturer: %s, product: %s, serial: %s";
    int len = snprintf(nullptr, 0, fmt,
                       vendor_id_, product_id_, manufacturer_.c_str(), product_.c_str(), serial_number_.c_str());
    std::string desc;
    desc.resize(len + 1);
    
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
    
    IOReturn ret = (*device_)->USBDeviceOpen(device_);
    usb_error::check(ret, "unable to open USB device");
    
    ret = (*device_)->SetConfiguration(device_, 1);
    usb_error::check(ret, "failed to set USB device configuration");
    
    is_open_ = true;
}

void usb_device::close() {
    if (!is_open())
        return;
    
    IOReturn ret = (*device_)->USBDeviceClose(device_);
    if (ret != kIOReturnSuccess)
        throw usb_error("unable to close USB device", ret);
    
    is_open_ = false;
}

void usb_device::claim_interface(int interface_number) {
    
    if (interface_ != nullptr)
        throw usb_error("an interface has already been claimed");
    
    // find interface
    IOUSBFindInterfaceRequest request;
    request.bInterfaceClass = kIOUSBFindInterfaceDontCare;
    request.bInterfaceSubClass = kIOUSBFindInterfaceDontCare;
    request.bInterfaceProtocol = kIOUSBFindInterfaceDontCare;
    request.bAlternateSetting = kIOUSBFindInterfaceDontCare;

    io_iterator_t iter;
    IOReturn ret = (*device_)->CreateInterfaceIterator(device_, &request, &iter);
    usb_error::check(ret, "internal error (CreateInterfaceIterator)");
    auto iter_guard = make_scope_exit([iter]() { IOObjectRelease(iter); });

    io_service_t service;
    IOUSBInterfaceInterface** interface = nullptr;
    while ((service = IOIteratorNext(iter)) != 0) {
        
        auto service_guard = make_scope_exit([service]() { IOObjectRelease(service); });

        IOUSBInterfaceInterface** intf = iokit_helper::get_interface<IOUSBInterfaceInterface>(service, kIOUSBInterfaceUserClientTypeID, kIOUSBInterfaceInterfaceID);
        if (intf == nullptr)
            throw usb_error("internal error (failed to create interface interface)");
        
        auto intf_guard = make_scope_exit([intf]() { (*intf)->Release(intf); });

        UInt8 num;
        ret = (*intf)->GetInterfaceNumber(intf, &num);
        usb_error::check(ret, "internal error (GetInterfaceNumber)");
        
        if (interface_number == num) {
            interface = intf;
            (*interface)->AddRef(interface);
            break;
        }
    }

    if (interface == nullptr)
        throw usb_error("no USB interface found for given number");

    auto interface_guard = make_scope_exit([interface]() { (*interface)->Release(interface); });

    ret = (*interface)->USBInterfaceOpen(interface);
    usb_error::check(ret, "failed to open USB interface");
    
    UInt8 num_pipes = 0;
    ret = (*interface)->GetNumEndpoints(interface, &num_pipes);
    usb_error::check(ret, "internal error (GetNumEndpoints)");

    endpoint_addresses_.clear();
    for (int i = 1; i <= num_pipes; i++) {
        UInt8 direction = 0;
        UInt8 number = 0;
        UInt8 ignore = 0;
        UInt16 ignore2 = 0;
        ret = (*interface)->GetPipeProperties(interface, i, &direction, &number, &ignore, &ignore2, &ignore);
        usb_error::check(ret, "internal error (GetPipeProperties)");
        endpoint_addresses_.push_back((direction << 7) | number);
    }

    interface_ = interface;
    (*interface_)->AddRef(interface_);
}

void usb_device::release_interface() {
    if (interface_ == nullptr)
        throw usb_error("no interface has been claimed");
    
    (*interface_)->USBInterfaceClose(interface_);
    (*interface_)->Release(interface_);
    interface_ = nullptr;
    endpoint_addresses_.clear();
}

std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int data_len, int timeout) {
    if (interface_ == nullptr)
        throw usb_error("no interface has been claimed");

    UInt32 size = data_len;
    std::vector<uint8_t> data(data_len);
    IOReturn ret;
    if (timeout != 0)
        ret = (*interface_)->ReadPipeTO(interface_, ep_in_pipe(endpoint_number), data.data(), &size, timeout, timeout);
    else
        ret = (*interface_)->ReadPipe(interface_, ep_in_pipe(endpoint_number), data.data(), &size);
    
    if (ret != kIOReturnSuccess) {
        if (ret == kIOUSBTransactionTimeout)
            throw usb_error("time-out reading from USB endpoint", ret);
        
        throw usb_error("error reading from USB endpoint", ret);
    }
    
    data.resize(size);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int timeout) {
    if (interface_ == nullptr)
        throw usb_error("no interface has been claimed");

    IOReturn ret;
    if (timeout != 0)
        ret = (*interface_)->WritePipeTO(interface_, ep_out_pipe(endpoint_number), const_cast<uint8_t*>(data.data()), static_cast<UInt32>(data.size()), timeout, timeout);
    else
        ret = (*interface_)->WritePipe(interface_, ep_out_pipe(endpoint_number), const_cast<uint8_t*>(data.data()), static_cast<UInt32>(data.size()));

    if (ret != kIOReturnSuccess) {
        if (ret == kIOUSBTransactionTimeout)
            throw usb_error("time-out writing to USB endpoint", ret);
        
        throw usb_error("error writing to USB endpoint", ret);
    }
}

int usb_device::control_transfer_core(const usb_control_request &request, uint8_t* data, int timeout) {
    
    if (!is_open())
        throw usb_error("USB device is not open");
    
    IOUSBDevRequestTO io_request = {
        .bmRequestType = request.bmRequestType,
        .bRequest = request.bRequest,
        .wValue = request.wValue,
        .wIndex = request.wIndex,
        .wLength = request.wLength,
        .pData = data,
        .wLenDone = 0,
        .noDataTimeout = (UInt32)timeout,
        .completionTimeout = (UInt32)timeout
    };
    
    IOReturn ret;
    if (timeout != 0)
        ret = (*device_)->DeviceRequestTO(device_, &io_request);
    else
        ret = (*device_)->DeviceRequest(device_, (IOUSBDevRequest*) &io_request);
    
    if (ret != kIOReturnSuccess) {
        if (ret == kIOUSBTransactionTimeout)
            throw usb_error("time-out sending control request", ret);
        
        throw usb_error("error sending control request", ret);
    }

    return io_request.wLenDone;
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

UInt8 usb_device::ep_to_pipe(int endpoint_address) {
    auto it = std::find (endpoint_addresses_.begin(), endpoint_addresses_.end(), endpoint_address);
    if (it != endpoint_addresses_.end())
        return std::distance(endpoint_addresses_.begin(), it) + 1;
    
    throw usb_error("invalid endpoint number");
}

UInt8 usb_device::ep_out_pipe(int endpoint_number) {
    return ep_to_pipe(endpoint_number);
}

UInt8 usb_device::ep_in_pipe(int endpoint_number) {
    return ep_to_pipe(endpoint_number + 128);
}
