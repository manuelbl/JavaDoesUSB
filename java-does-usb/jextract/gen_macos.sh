#!/bin/sh

JEXTRACT=../../../jextract-19/bin/jextract
SDK_DIR=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk

# CoreFoundation
#$JEXTRACT --source --output ../src/main/java \
#  -I $SDK_DIR/usr/include \
#  --target-package net.codecrete.usb.macos.gen.corefoundation
#  --include-function CFUUIDCreateFromUUIDBytes \
#  $SDK_DIR/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CoreFoundation.h

# mach.h
$JEXTRACT --source --output ../src/main/java \
  -I $SDK_DIR/usr/include \
  --header-class-name mach \
  --target-package net.codecrete.usb.macos.gen.mach \
  --include-function mach_error_string \
  $SDK_DIR/usr/include/mach/mach.h
