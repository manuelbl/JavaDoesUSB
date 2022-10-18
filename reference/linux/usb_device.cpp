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

#include <fcntl.h>
#include <linux/usbdevice_fs.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <iostream>

#include <cstdio>

usb_device::usb_device(const char* path, int vendor_id, int product_id)
: path_(path), fd_(-1), claimed_interface_(-1), vendor_id_(vendor_id), product_id_(product_id) {
}

usb_device::~usb_device() {
    if (is_open())
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
    return fd_ >= 0;
}

void usb_device::open() {
    std::cout << USBDEVFS_SETINTERFACE << std::endl;
    if (is_open())
        throw usb_error("USB device is already open", 0);
    
    fd_ = ::open(path_.c_str(), O_RDWR | O_CLOEXEC);
    if (fd_ == -1)
        usb_error::throw_error("Cannot open USB device");
}

void usb_device::close() {
    if (!is_open())
        return;

    if (claimed_interface_ >= 0)
        release_interface();

    int ret = ::close(fd_);
    fd_ = -1;
    if (ret != 0)
        usb_error::throw_error("unable to close USB device");
}

void usb_device::claim_interface(int interface_number) {
    
    if (claimed_interface_ >= 0)
        throw usb_error("an interface has already been claimed");
    
    int result = ioctl(fd_, USBDEVFS_CLAIMINTERFACE, &interface_number);
    if (result < 0)
        usb_error::throw_error("Failed to claim interface 0");

    claimed_interface_ = interface_number;
}

void usb_device::release_interface() {
    if (claimed_interface_ < 0)
        throw usb_error("no interface has been claimed");
    
    int result = ioctl(fd_, USBDEVFS_RELEASEINTERFACE, &claimed_interface_);
    if (result < 0)
        usb_error::throw_error("Failed to release interface");

    claimed_interface_ = -1;
}

std::vector<uint8_t> usb_device::transfer_in(int endpoint_number, int data_len, int timeout) {
    if (claimed_interface_ < 0)
        throw usb_error("no interface has been claimed");

    std::vector<uint8_t> data(data_len);

    struct usbdevfs_bulktransfer transfer = {0};
    transfer.ep = endpoint_number + 128;
    transfer.len = data_len;
    transfer.timeout = timeout;
    transfer.data = data.data();

    int result = ioctl(fd_, USBDEVFS_BULK, &transfer);
    if (result < 0)
        usb_error::throw_error("error receiving from USB endpoint");

    data.resize(result);
    return data;
}

void usb_device::transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int timeout) {
    if (claimed_interface_ < 0)
        throw usb_error("no interface has been claimed");

    struct usbdevfs_bulktransfer transfer = {0};
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
    
    struct usbdevfs_ctrltransfer ctrl_request = {0};
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
