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
#include "config_parser.hpp"

#include <IOKit/usb/IOUSBLib.h>

#include <cstdio>

usb_device::usb_device(io_service_t service, IOUSBDeviceInterface** device, uint64_t entry_id, int vendor_id, int product_id)
: entry_id_(entry_id), device_(device), vendor_id_(vendor_id), product_id_(product_id), is_open_(false) {
    
    manufacturer_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBVendorString));
    product_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBProductString));
    serial_number_ = iokit_helper::ioreg_get_property_as_string(service, CFSTR(kUSBSerialNumberString));

    load_configuration(device);
    
    (*device)->AddRef(device);
}

usb_device::~usb_device() {
    close();
}

void usb_device::load_configuration(IOUSBDeviceInterface** device) {
    IOUSBConfigurationDescriptorPtr desc = nullptr;
    (*device)->GetConfigurationDescriptorPtr(device, 0, &desc);
    
    config_parser parser{};
    parser.parse(reinterpret_cast<const uint8_t*>(desc), desc->wTotalLength);
    interfaces_ = std::move(parser.interfaces);
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

const std::vector<usb_interface>& usb_device::interfaces() const {
    return interfaces_;
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
    
    if (claimed_interfaces_.find(interface_number) != claimed_interfaces_.end())
        throw usb_error("interface has already been claimed");
    
    usb_interface* uintf = get_interface(interface_number);
    if (uintf == nullptr)
        throw usb_error("no such interface");
    
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
        throw usb_error("internal error");

    auto interface_guard = make_scope_exit([interface]() { (*interface)->Release(interface); });

    ret = (*interface)->USBInterfaceOpen(interface);
    usb_error::check(ret, "failed to open USB interface");
    
    (*interface)->AddRef(interface);
    claimed_interfaces_[interface_number] = interface;
    uintf->set_claimed(true);
    build_pipe_info();
}

void usb_device::release_interface(int interface_number) {
    
    usb_interface* uintf = get_interface(interface_number);
    if (uintf == nullptr)
        throw usb_error("no such interface");
    
    auto iter = claimed_interfaces_.find(interface_number);
    if (iter == claimed_interfaces_.end())
        throw usb_error("interface has been claimed");
    
    IOUSBInterfaceInterface** interface = (*iter).second;
    claimed_interfaces_.erase(iter);
    uintf->set_claimed(false);
    
    (*interface)->USBInterfaceClose(interface);
    (*interface)->Release(interface);
    
    build_pipe_info();
}

void usb_device::select_alternate_interface(int interface_number, int alternate_setting) {

    usb_interface* uintf = get_interface(interface_number);
    if (uintf == nullptr)
        throw usb_error("no such interface");
    
    int alt_index = get_alternate_index(interface_number, alternate_setting);
    if (alt_index == -1)
        throw usb_error("no such alternate setting");

    auto iter = claimed_interfaces_.find(interface_number);
    if (iter == claimed_interfaces_.end())
        throw usb_error("interface has not been claimed");
    
    IOUSBInterfaceInterface** interface = iter->second;
    (*interface)->SetAlternateInterface(interface, (UInt8) alternate_setting);
    uintf->set_alternate(alt_index);
    
    build_pipe_info();
}

void usb_device::build_pipe_info() {
    pipes_.clear();

    for (auto& intf : claimed_interfaces_) {
        IOUSBInterfaceInterface** interface = intf.second;
        
        UInt8 num_pipes = 0;
        IOReturn ret = (*interface)->GetNumEndpoints(interface, &num_pipes);
        usb_error::check(ret, "internal error (GetNumEndpoints)");
        
        for (int i = 1; i <= num_pipes; i++) {
            UInt8 direction = 0;
            UInt8 number = 0;
            UInt8 transfer_type = 0;
            UInt8 ignore = 0;
            UInt16 ignore2 = 0;
            ret = (*interface)->GetPipeProperties(interface, i, &direction, &number, &transfer_type, &ignore2, &ignore);
            usb_error::check(ret, "internal error (GetPipeProperties)");
            
            uint8_t addr = static_cast<uint8_t>((direction << 7) | number);
            usb_transfer_type type = static_cast<usb_transfer_type>(transfer_type); // both enumeration use the same value as the USB standard
            pipe_info pipe{static_cast<uint8_t>(i), addr, type, intf.first};
            pipes_.push_back(std::move(pipe));
        }
    }
}


std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int data_len, int timeout) {
    auto pipe = ep_in_pipe(endpoint_number);
    IOUSBInterfaceInterface** interface = claimed_interfaces_[pipe->interface_number];

    UInt32 size = data_len;
    std::vector<uint8_t> data(data_len);
    IOReturn ret;
    if (timeout != 0) {
        ret = (*interface)->ReadPipeTO(interface, pipe->pipe_index, data.data(), &size, timeout, timeout);
    } else {
        ret = (*interface)->ReadPipe(interface, pipe->pipe_index, data.data(), &size);
    }        
    
    if (ret != kIOReturnSuccess) {
        if (ret == kIOUSBTransactionTimeout)
            throw usb_error("time-out reading from USB endpoint", ret);
        
        throw usb_error("error reading from USB endpoint", ret);
    }
    
    data.resize(size);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int timeout) {
    auto pipe = ep_out_pipe(endpoint_number);
    IOUSBInterfaceInterface** interface = claimed_interfaces_[pipe->interface_number];

    IOReturn ret;
    if (timeout != 0)
        ret = (*interface)->WritePipeTO(interface, pipe->pipe_index, const_cast<uint8_t*>(data.data()), static_cast<UInt32>(data.size()), timeout, timeout);
    else
        ret = (*interface)->WritePipe(interface, pipe->pipe_index, const_cast<uint8_t*>(data.data()), static_cast<UInt32>(data.size()));

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

const usb_device::pipe_info* usb_device::get_pipe(int endpoint_address) {
    auto it = std::find_if(pipes_.begin(), pipes_.end(),
                           [endpoint_address](const pipe_info& pipe){ return pipe.endpoint_address == endpoint_address; });
    if (it != pipes_.end()) {
        const pipe_info* pi = &*it;
        if (pi->transfer_type != usb_transfer_type::bulk && pi->transfer_type != usb_transfer_type::interrupt)
            throw usb_error("invalid transfer type for endpoint");
        return pi;
    }
    
    // good error message
    for (usb_interface& intf : interfaces_) {
        for (const usb_endpoint& ep : intf.alternate().endpoints()) {
            int addr = ep.number();
            if (ep.direction() == usb_direction::in)
                addr += 128;
            if (addr == endpoint_address)
                throw usb_error("endpoint's interface has not been claimed");
        }
    }
    
    throw usb_error("no such endpoint");
}

 const usb_device::pipe_info* usb_device::ep_out_pipe(int endpoint_number) {
    return get_pipe(endpoint_number);
}

 const usb_device::pipe_info* usb_device::ep_in_pipe(int endpoint_number) {
    return get_pipe(endpoint_number + 128);
}

usb_interface* usb_device::get_interface(int interface_number) {
    for (usb_interface& intf : interfaces_)
        if (intf.number() == interface_number)
            return &intf;
    
    return nullptr;
}

int usb_device::get_alternate_index(int interface_number, int alternate_setting) {
    for (usb_interface& intf : interfaces_) {
        if (intf.number() == interface_number) {
            for (int i = 0; i < intf.alternates().size(); i++) {
                if (intf.alternates()[i].number() == alternate_setting)
                    return i;
            }
        }
    }
    
    return -1;
}
