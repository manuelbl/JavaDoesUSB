#!/bin/sh

JEXTRACT=../../../../jextract/build/jextract/bin/jextract

# errno.h
$JEXTRACT --output ../../src/main/java \
  --header-class-name errno \
  --target-package net.codecrete.usb.linux.gen.errno \
  --include-constant EPIPE \
  --include-constant EAGAIN \
  --include-constant EINVAL \
  --include-constant ENODEV \
  --include-constant EINTR \
  --include-constant ENOENT \
  /usr/include/errno.h

# string.h
$JEXTRACT --output ../../src/main/java \
  --header-class-name string \
  --target-package net.codecrete.usb.linux.gen.string \
  --include-function strerror \
  /usr/include/string.h

# fcntl.h
$JEXTRACT --output ../../src/main/java \
  --header-class-name fcntl \
  --target-package net.codecrete.usb.linux.gen.fcntl \
  --include-constant O_CLOEXEC \
  --include-constant O_RDWR \
  --include-constant FD_CLOEXEC \
  /usr/include/fcntl.h

# unistd.h
$JEXTRACT --output ../../src/main/java \
  --header-class-name unistd \
  --target-package net.codecrete.usb.linux.gen.unistd \
  --include-function close \
  /usr/include/unistd.h

# usbdevice_fs.h
# Missing constants like USBDEVFS_CLAIMINTERFACE
$JEXTRACT --output ../../src/main/java \
  --header-class-name usbdevice_fs \
  --target-package net.codecrete.usb.linux.gen.usbdevice_fs \
  --include-struct usbdevfs_bulktransfer \
  --include-struct usbdevfs_ctrltransfer \
  --include-struct usbdevfs_setinterface \
  --include-struct usbdevfs_urb \
  --include-struct usbdevfs_disconnect_claim \
  --include-struct usbdevfs_ioctl \
  --include-struct usbdevfs_iso_packet_desc \
  --include-constant USBDEVFS_URB_TYPE_INTERRUPT \
  --include-constant USBDEVFS_URB_TYPE_CONTROL \
  --include-constant USBDEVFS_URB_TYPE_BULK \
  --include-constant USBDEVFS_URB_TYPE_ISO \
  --include-constant USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER \
  /usr/include/linux/usbdevice_fs.h

# libudev.h
# (install libudev-dev if file is missing)
$JEXTRACT --output ../../src/main/java \
  --header-class-name udev \
  --target-package net.codecrete.usb.linux.gen.udev \
  -l udev \
  --include-function udev_new \
  --include-function udev_enumerate_new \
  --include-function udev_enumerate_add_match_subsystem \
  --include-function udev_enumerate_scan_devices \
  --include-function udev_enumerate_get_list_entry \
  --include-function udev_list_entry_get_next \
  --include-function udev_list_entry_get_name \
  --include-function udev_device_new_from_syspath \
  --include-function udev_device_get_devnode \
  --include-function udev_device_get_sysattr_value \
  --include-function udev_device_unref \
  --include-function udev_enumerate_unref \
  --include-function udev_monitor_new_from_netlink \
  --include-function udev_monitor_enable_receiving \
  --include-function udev_monitor_filter_add_match_subsystem_devtype \
  --include-function udev_monitor_receive_device \
  --include-function udev_device_get_action \
  --include-function udev_device_get_devtype \
  --include-function udev_monitor_get_fd \
  /usr/include/libudev.h

# epoll.h
$JEXTRACT --output ../../src/main/java \
  --header-class-name epoll \
  --target-package net.codecrete.usb.linux.gen.epoll \
  --include-struct epoll_event \
  --include-union epoll_data \
  --include-constant EPOLL_CTL_ADD \
  --include-constant EPOLL_CTL_DEL \
  --include-constant EPOLLIN \
  --include-constant EPOLLOUT \
  --include-constant EPOLLWAKEUP \
  epoll.h

