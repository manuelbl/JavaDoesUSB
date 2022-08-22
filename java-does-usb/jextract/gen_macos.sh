#!/bin/sh

JEXTRACT=../../../jextract/build/jextract/bin/jextract

# CoreFoundation
#$JEXTRACT --source --target-package net.codecrete.usb.macos.gen --output ../src/main/java \
#  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
#  --include-function CFUUIDCreateFromUUIDBytes \
#  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CoreFoundation.h

## mach_error
$JEXTRACT --source --target-package net.codecrete.usb.macos.gen --output ../src/main/java \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
  --header-class-name mach \
  --include-function mach_error \
  /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/mach/mach.h
