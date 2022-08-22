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
  /usr/include/errno.h

# ioctl.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name ioctl \
  --target-package net.codecrete.usb.linux.gen.ioctl \
  /usr/include/x86_64-linux-gnu/sys/ioctl.h

# fcntl.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name fcntl \
  --target-package net.codecrete.usb.linux.gen.fcntl \
  /usr/include/fcntl.h

# unistd.h
$JEXTRACT --source --output ../src/main/java \
  --header-class-name unistd \
  --target-package net.codecrete.usb.linux.gen.unistd \
  /usr/include/unistd.h

# usbdevice_fs.h
# Missing constants like USBDEVFS_CLAIMINTERFACE
$JEXTRACT --source --output ../src/main/java \
  --header-class-name usbdevice_fs \
  --target-package net.codecrete.usb.linux.gen.usbdevice_fs \
  /usr/include/linux/usbdevice_fs.h
