//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Linux
//

#include "usb_device.hpp"
#include "usb_iostream.hpp"
#include "usb_registry.hpp"
#include "usb_error.hpp"
#include "scope.hpp"
#include "config_parser.hpp"

#include <fcntl.h>
#include <linux/usbdevice_fs.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <cstdio>
#include <fstream>


usb_device::usb_device(usb_registry* registry, const char* path, int vendor_id, int product_id)
: registry_(registry), path_(path), fd_(-1), uses_urbs_(false), vendor_id_(vendor_id), product_id_(product_id) {
    read_descriptor();
}

usb_device::~usb_device() {
    if (is_open())
        close();
}

void usb_device::set_product_strings(const char* manufacturer, const char* product, const char* serial_number) {
    manufacturer_ = manufacturer != nullptr ? manufacturer : "";
    product_ = product != nullptr ? product : "";
    serial_number_ = serial_number != nullptr ? serial_number : "";
}

void usb_device::read_descriptor() {
    // read device and configuration descriptor
    std::vector<uint8_t> descriptors{};
    {
        std::ifstream file(path(), std::ios::binary);
        uint8_t buf[256];
        while (!file.eof()) {
            file.read(reinterpret_cast<char*>(buf), sizeof(buf));
            if (file.bad())
                throw usb_error("failed to read device and configuration descriptor");
            descriptors.insert(descriptors.end(), buf, buf + file.gcount());
        }
    }

    int config_desc_offset = descriptors[0];
    config_parser parser{};
    parser.parse(descriptors.data() + config_desc_offset, descriptors.size() - config_desc_offset);
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
    return fd_ >= 0;
}

void usb_device::open() {
    if (is_open())
        throw usb_error("USB device is already open", 0);
    
    fd_ = ::open(path_.c_str(), O_RDWR | O_CLOEXEC);
    if (fd_ == -1)
        usb_error::throw_error("Cannot open USB device");
}

void usb_device::close() {
    if (!is_open())
        return;

    if (uses_urbs_) {
        registry_->remove_async_fd(fd_);
        uses_urbs_ = false;
    }
    
    for (auto& intf : interfaces_)
        intf.set_claimed(false);

    claimed_interfaces_.clear();
    
    int ret = ::close(fd_);
    fd_ = -1;
    if (ret != 0)
        usb_error::throw_error("unable to close USB device");
}

void usb_device::claim_interface(int interface_number) {

    if (!is_open())
        throw usb_error("device is not open");

    usb_interface* intf = get_intf_ptr(interface_number);
    if (intf == nullptr)
        throw usb_error("no such interface");

    if (intf->is_claimed())
        throw usb_error("interface has already been claimed");

    usbdevfs_disconnect_claim dc = {
        .interface = static_cast<unsigned int>(interface_number),
        .flags = USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER,
        .driver = "usbfs"
    };
    
    int result = ioctl(fd_, USBDEVFS_DISCONNECT_CLAIM, &dc);
    if (result < 0)
        usb_error::throw_error("Failed to claim interface");

    claimed_interfaces_.insert(interface_number);
    intf->set_claimed(true);
}

void usb_device::release_interface(int interface_number) {

    if (!is_open())
        throw usb_error("device is not open");

    usb_interface* intf = get_intf_ptr(interface_number);
    if (intf == nullptr)
        throw usb_error("no such interface");

    if (!intf->is_claimed())
        throw usb_error("interface has not been claimed");

    int result = ioctl(fd_, USBDEVFS_RELEASEINTERFACE, &interface_number);
    if (result < 0)
        usb_error::throw_error("Failed to release interface");

    intf->set_claimed(false);
    claimed_interfaces_.erase(interface_number);

    usbdevfs_ioctl cmd = {
        .ifno = interface_number,
        .ioctl_code = USBDEVFS_CONNECT,
        .data = nullptr
    };
    ioctl(fd_, USBDEVFS_IOCTL, &cmd);
}

std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int timeout) {
    
    auto ep = check_endpoint(usb_direction::in, endpoint_number);

    std::vector<uint8_t> data(ep->packet_size());

    usbdevfs_bulktransfer transfer = {0};
    transfer.ep = endpoint_number + 128;
    transfer.len = ep->packet_size();
    transfer.timeout = timeout;
    transfer.data = data.data();

    int result = ioctl(fd_, USBDEVFS_BULK, &transfer);
    if (result < 0)
        usb_error::throw_error("error receiving from USB endpoint");

    data.resize(result);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int len, int timeout) {
    if (len < 0 || len > data.size())
        len = static_cast<int>(data.size());

    check_endpoint(usb_direction::out, endpoint_number);

    usbdevfs_bulktransfer transfer = {0};
    transfer.ep = endpoint_number;
    transfer.len = data.size();
    transfer.timeout = timeout;
    transfer.data = const_cast<uint8_t*>(data.data());

    int result = ioctl(fd_, USBDEVFS_BULK, &transfer);
    if (result < 0)
        usb_error::throw_error("error transmitting to USB endpoint");
}

int usb_device::control_transfer_core(const usb_control_request &request, uint8_t* data, int timeout) {
    
    if (!is_open())
        throw usb_error("USB device is not open");
    
    usbdevfs_ctrltransfer ctrl_request = {0};
    ctrl_request.bRequestType = request.bmRequestType;
    ctrl_request.bRequest = request.bRequest;
    ctrl_request.wValue = request.wValue;
    ctrl_request.wIndex = request.wIndex;
    ctrl_request.wLength = request.wLength;
    ctrl_request.timeout = timeout;
    ctrl_request.data = data;

    int result = ioctl(fd_, USBDEVFS_CONTROL, &ctrl_request);
    if (result < 0)
        usb_error::throw_error("error sending control request");

    return result;
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

usb_interface* usb_device::get_intf_ptr(int number) {
    for (auto& intf : interfaces_)
        if (intf.number() == number)
            return &intf;
    return nullptr;
}

const usb_endpoint* usb_device::check_endpoint(usb_direction direction, int endpoint_number) {

    if (!is_open())
        throw usb_error("device is not open");

    for (auto& intf : interfaces_) {
        for (auto& ep : intf.alternate().endpoints()) {
            if (ep.direction() == direction && ep.number() == endpoint_number) {
                if (!intf.is_claimed())
                    throw usb_error("interface has not been claimed");
                if (ep.transfer_type() != usb_transfer_type::bulk && ep.transfer_type() != usb_transfer_type::interrupt)
                    throw usb_error("invalid endpoint transfer type for operation");
                return &ep;
            }
        }
    }

    throw usb_error("no such endpoint");
}

std::unique_ptr<std::istream> usb_device::open_input_stream(int endpoint_number) {
    return std::unique_ptr<std::istream>(new usb_istream(registry_->get_shared_ptr(this), endpoint_number));
}

std::unique_ptr<std::ostream> usb_device::open_output_stream(int endpoint_number) {
    return std::unique_ptr<std::ostream>(new usb_ostream(registry_->get_shared_ptr(this), endpoint_number));
}

void usb_device::submit_urb(usbdevfs_urb* urb) {
    if (!uses_urbs_) {
        uses_urbs_ = true;
        registry_->add_async_fd(fd_);
    }

    int result = ioctl(fd_, USBDEVFS_SUBMITURB, urb);
    if (result < 0)
        usb_error::throw_error("Failed to submit URB");
}

void usb_device::cancel_urb(usbdevfs_urb* urb) {
    int result = ioctl(fd_, USBDEVFS_DISCARDURB, urb);
    if (result < 0)
        usb_error::throw_error("Failed to submit URB");
}
