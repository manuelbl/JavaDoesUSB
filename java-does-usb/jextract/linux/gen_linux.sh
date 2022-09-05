#!/bin/sh

JEXTRACT=../../../../jextract-19/bin/jextract

# sd-device.h (install libsystemd-dev if file is missing)
# Error: /usr/include/inttypes.h:290:8: error: unknown type name 'intmax_t'
#$JEXTRACT --source --output ../src/main/java \
#$JEXTRACT --source --output ../src/main/java \
#  --header-class-name sd_device \
#  --target-package net.codecrete.usb.linux.gen.sd-device
#  /usr/include/systemd/sd-device.h

# errno.h
$JEXTRACT --source --output ../../src/main/java \
  --header-class-name errno \
  --target-package net.codecrete.usb.linux.gen.errno \
  --include-function __errno_location \
  /usr/include/errno.h

# ioctl.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name ioctl \
  --target-package net.codecrete.usb.linux.gen.ioctl \
  --include-function ioctl \
  /usr/include/x86_64-linux-gnu/sys/ioctl.h

# fcntl.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name fcntl \
  --target-package net.codecrete.usb.linux.gen.fcntl \
  --include-function open \
  --include-macro O_CLOEXEC \
  --include-macro O_RDWR \
  /usr/include/fcntl.h

# unistd.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name unistd \
  --target-package net.codecrete.usb.linux.gen.unistd \
  --include-function close \
  /usr/include/unistd.h

# usbdevice_fs.h
# Missing constants like USBDEVFS_CLAIMINTERFACE
$JEXTRACT --source --output ../src/main/java \
  --header-class-name usbdevice_fs \
  --target-package net.codecrete.usb.linux.gen.usbdevice_fs \
  --include-struct usbdevfs_bulktransfer \
  --include-struct usbdevfs_ctrltransfer \
  --include-macro USBDEVFS_CONTROL \
  --include-macro USBDEVFS_BULK \
  --include-macro USBDEVFS_CLAIMINTERFACE \
  --include-macro USBDEVFS_RELEASEINTERFACE \
  /usr/include/linux/usbdevice_fs.h

# libudev.h
$JEXTRACT --source --output ../src/main/java \
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

# select.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name select \
  --target-package net.codecrete.usb.linux.gen.select \
  --include-function select \
  --include-typedef fd_set \
  /usr/include/x86_64-linux-gnu/sys/select.h
