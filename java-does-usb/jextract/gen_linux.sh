#!/bin/sh

JEXTRACT=../../../jextract/build/jextract/bin/jextract

# sd-device.h (install libsystemd-dev if file is missing)
# Error: /usr/include/inttypes.h:290:8: error: unknown type name 'intmax_t'
#$JEXTRACT --source --output ../src/main/java \
#  --header-class-name sd_device \
#  --target-package net.codecrete.usb.linux.gen.sd-device
#  /usr/include/systemd/sd-device.h

# errno.h
$JEXTRACT --source --output ../src/main/java \
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
