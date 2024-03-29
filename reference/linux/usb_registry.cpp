//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Linux
//

#include "usb_registry.hpp"
#include "usb_device.hpp"
#include "usb_error.hpp"
#include "scope.hpp"

#include <libudev.h>
#include <poll.h>
#include <sys/epoll.h>
#include <unistd.h>
#include <sys/eventfd.h>
#include <linux/usbdevice_fs.h>
#include <sys/ioctl.h>

#include <algorithm>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <mutex>
#include <string>
#include <thread>

usb_registry::usb_registry()
: monitor_wake_event_fd(-1),
    on_connected_callback(nullptr), on_disconnected_callback(nullptr), is_device_list_ready(false),
    async_io_epoll_fd(-1), async_io_exit_event_fd(-1) {
}

usb_registry::~usb_registry() {
    eventfd_write(monitor_wake_event_fd, 1);
    monitor_thread.join();
    ::close(monitor_wake_event_fd);

    if (async_io_exit_event_fd != -1) {
        eventfd_write(async_io_exit_event_fd, 999999);
        async_io_thread.join();
        ::close(async_io_exit_event_fd);
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
    monitor_wake_event_fd = eventfd(0, 0);
    if (monitor_wake_event_fd < 0)
        usb_error::throw_error("internal error(eventfd)");

    monitor_thread = std::thread(&usb_registry::monitor, this);
    
    std::unique_lock<std::mutex> wait_lock(monitor_mutex);
    monitor_condition.wait(wait_lock, [this] { return is_device_list_ready; });
}

void usb_registry::monitor() {

    udev* udev = udev_new();
    if (udev == nullptr)
        throw usb_error("internal error (udev_new)");

    auto udev_guard = make_scope_exit([udev]() { udev_unref(udev); });
    
    udev_monitor* monitor = udev_monitor_new_from_netlink(udev, "udev");
    if (monitor == nullptr)
        throw usb_error("internal error (udev_monitor_new_from_netlink)");

    auto monitor_guard = make_scope_exit([monitor]() { udev_monitor_unref(monitor); });

    if (udev_monitor_filter_add_match_subsystem_devtype(monitor, "usb", "usb_device") < 0)
        throw usb_error("internal error (udev_monitor_filter_add_match_subsystem_devtype)");

    if (udev_monitor_enable_receiving(monitor) < 0)
        throw usb_error("internal error (udev_monitor_enable_receiving)");

    int monitor_fd = udev_monitor_get_fd(monitor);
    if (monitor_fd < 0)
        throw usb_error("internal error (udev_monitor_get_fd)");

    enumerate_present_devices(udev);

    is_device_list_ready = true;
    monitor_condition.notify_all();

    while (true) {

        pollfd fds[2];
        fds[0].fd = monitor_fd;
        fds[0].events = POLLIN;
        fds[1].fd = monitor_wake_event_fd;
        fds[1].events = POLLIN;

        int ret = poll(fds, 2, -1);
        if (ret < 0)
            usb_error::throw_error("internal error (poll)");
        
        if ((fds[1].revents & POLLIN) != 0)
            break;
        
        // get affected device
        udev_device* device = udev_monitor_receive_device(monitor);
        if (device == nullptr)
            continue; // shouldn't happen

        auto device_guard = make_scope_exit([device]() { udev_device_unref(device); });

        const char* action = udev_device_get_action(device);
        if (strcmp("add", action) == 0) {
            on_device_connected(device);
        } else if (strcmp("remove", action) == 0) {
            on_device_disconnected(device);
        }
    }
}

void usb_registry::on_device_connected(udev_device* udev_dev) {

    usb_device_ptr device = create_device(udev_dev);
    if (!device)
        return;
    
    devices.push_back(device);

    if (on_connected_callback != nullptr) {
        try {
            on_connected_callback(device);
        } catch (usb_error& e) {
            std::cerr << "Unhandled exception on device connect: " << e.what() << std::endl;
        } catch (...) {
            std::cerr << "Unhandled exception on device connect." << std::endl;
        }
    }
}

void usb_registry::on_device_disconnected(udev_device* udev_dev) {

    const char* path = udev_device_get_devnode(udev_dev);
    if (path == nullptr)
        throw usb_error("internal error (udev_device_get_devnode)");
    
    // find device in device list
    auto it = std::find_if(devices.cbegin(), devices.cend(), [path](auto dev) { return strcmp(dev->path(), path) == 0; });
    if (it == devices.cend())
        return; // not part of the device list
        
    // remove from device list
    usb_device_ptr device = *it;
    devices.erase(it);
    
    // call callback function
    if (on_disconnected_callback != nullptr) {
        try {
            on_disconnected_callback(device);
        } catch (usb_error& e) {
            std::cerr << "Unhandled exception on device disconnect: " << e.what() << std::endl;
        } catch (...) {
            std::cerr << "Unhandled exception on device disconnect." << std::endl;
        }
    }
}

void usb_registry::enumerate_present_devices(udev* udev) {

    udev_enumerate* enumerate = udev_enumerate_new(udev);
    if (enumerate == nullptr)
        throw usb_error("internal error (udev_enumerate_new)");

    auto enumerate_guard = make_scope_exit([enumerate]() { udev_enumerate_unref(enumerate); });

    if (udev_enumerate_add_match_subsystem(enumerate, "usb") < 0)
        throw usb_error("internal error (udev_enumerate_add_match_subsystem)");
    
    if (udev_enumerate_scan_devices(enumerate) < 0)
        throw usb_error("internal error (udev_enumerate_scan_devices)");

    for (udev_list_entry* entry = udev_enumerate_get_list_entry(enumerate);
            entry != nullptr;
            entry = udev_list_entry_get_next(entry)) {

        const char* path = udev_list_entry_get_name(entry);
        if (path == nullptr)
            continue;

        // get device handle
        udev_device* udev_dev = udev_device_new_from_syspath(udev, path);
        if (udev_dev == nullptr)
            continue;

        auto udev_dev_guard = make_scope_exit([udev_dev]() { udev_device_unref(udev_dev); });

        // create device
        usb_device_ptr device = create_device(udev_dev);
        if (device)
            devices.push_back(device);
    }
}

std::shared_ptr<usb_device> usb_registry::create_device(udev_device* udev_dev) {
    int vendor_id = 0;
    int product_id = 0;

    const char* vendor_id_str = udev_device_get_sysattr_value(udev_dev, "idVendor");
    if (vendor_id_str == nullptr)
        return nullptr;
    
    const char* product_id_str = udev_device_get_sysattr_value(udev_dev, "idProduct");
    if (product_id_str == nullptr)
        return nullptr;
    
    const char* path = udev_device_get_devnode(udev_dev);
    if (path == nullptr)
        return nullptr;

    vendor_id = strtol(vendor_id_str, nullptr, 16);
    product_id = strtol(product_id_str, nullptr, 16);

    if (vendor_id == 0 || product_id == 0)
        return nullptr;
    
    std::shared_ptr<usb_device> device(new usb_device(this, path, vendor_id, product_id));
    device->set_product_strings(
        udev_device_get_sysattr_value(udev_dev, "manufacturer"),
        udev_device_get_sysattr_value(udev_dev, "product"),
        udev_device_get_sysattr_value(udev_dev, "serial")
    );
    return device;
}

std::shared_ptr<usb_device> usb_registry::get_shared_ptr(usb_device* device) {
    auto it = std::find_if(devices.cbegin(), devices.cend(), [device](auto dev) { return dev.get() == device; });
    if (it == devices.cend())
        return nullptr;

    return *it;
}

void usb_registry::async_io_run() {

    while (true) {
        struct epoll_event events[5];
        int ret = epoll_wait(async_io_epoll_fd, &events[0], 5, -1);
        if (ret < 0) {
            if (errno == EINTR)
                continue;
            usb_error::throw_error("internal error (epoll)");
        }
        
        for (int i = 0; i < ret; i++) {
            int fd = events[i].data.fd;
            if (fd == async_io_exit_event_fd)
                return;
            reap_urbs(fd);
        }
    }
}

void usb_registry::reap_urbs(int fd) {
    while (true) {
        usbdevfs_urb* urb = nullptr;
        int ret = ioctl(fd, USBDEVFS_REAPURB, &urb);
        if (ret < 0) {
            if (errno == EAGAIN)
                return; // no more pending URBs
            if (errno == ENODEV)
                return; // ignore, device might have been closed
            usb_error::throw_error("internal error (reap URB)");            
        }
        
        auto completion = reinterpret_cast<usb_io_callback*>(urb->usercontext);
        (*completion)();
    }
}

void usb_registry::add_async_fd(int fd) {
    int expected_request;

    {
        std::lock_guard lock(async_io_mutex);

        // start background thread if needed
        if (async_io_exit_event_fd == -1) {
            async_io_exit_event_fd = eventfd(0, 0);
            if (async_io_exit_event_fd < 0)
                usb_error::throw_error("internal error(eventfd)");

            async_io_epoll_fd = epoll_create(4);
            if (async_io_epoll_fd < 0)
                usb_error::throw_error("internal error(epoll_create)");

            epoll_event event = {0};
            event.events = EPOLLIN;
            event.data.fd = async_io_exit_event_fd;
            int ret = epoll_ctl(async_io_epoll_fd, EPOLL_CTL_ADD, async_io_exit_event_fd, &event);
            if (ret < 0)
                usb_error::throw_error("internal error(epoll_ctl)");

            async_io_thread = std::thread(&usb_registry::async_io_run, this);
        }
    }

    epoll_event event = {0};
    event.events = EPOLLOUT;
    event.data.fd = fd;
    int ret = epoll_ctl(async_io_epoll_fd, EPOLL_CTL_ADD, fd, &event);
    if (ret < 0)
        usb_error::throw_error("internal error(epoll_ctl)");
}


void usb_registry::remove_async_fd(int fd) {
    epoll_event event = {0};
    int ret = epoll_ctl(async_io_epoll_fd, EPOLL_CTL_DEL, fd, &event);
    if (ret < 0)
        usb_error::throw_error("internal error(epoll_ctl)");
}
