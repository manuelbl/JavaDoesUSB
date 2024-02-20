#!/bin/sh

JEXTRACT=../../../../jextract/build/jextract/bin/jextract
# If SDK_DIR is changed, it needs to be changed in compile_flags.txt as well.
SDK_DIR=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk

# CoreFoundation
$JEXTRACT --output ../../src/main/java \
  -I $SDK_DIR/usr/include \
  -l :/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation \
  --header-class-name CoreFoundation \
  --target-package net.codecrete.usb.macos.gen.corefoundation \
  --include-struct CFRange \
  --include-struct CFUUIDBytes \
  --include-function CFUUIDCreateFromUUIDBytes \
  --include-function CFRelease \
  --include-function CFStringGetLength \
  --include-function CFStringGetCharacters \
  --include-function CFStringCreateWithCharacters \
  --include-function CFGetTypeID \
  --include-function CFNumberGetTypeID \
  --include-function CFStringGetTypeID \
  --include-function CFNumberGetValue \
  --include-function CFRunLoopGetCurrent \
  --include-function CFRunLoopAddSource \
  --include-function CFRunLoopRemoveSource \
  --include-function CFRunLoopRun \
  --include-function CFUUIDGetUUIDBytes \
  --include-constant kCFNumberSInt32Type \
  cf_helper.h

# IOKit
$JEXTRACT --output ../../src/main/java \
  -I $SDK_DIR/usr/include \
  -l :/System/Library/Frameworks/IOKit.framework/IOKit \
  --header-class-name IOKit \
  --target-package net.codecrete.usb.macos.gen.iokit \
  --include-var kIOMasterPortDefault \
  --include-constant kIOUSBDeviceClassName \
  --include-constant kIOFirstMatchNotification \
  --include-constant kIOTerminatedNotification \
  --include-constant kIOReturnExclusiveAccess \
  --include-var kCFRunLoopDefaultMode \
  --include-struct IOCFPlugInInterfaceStruct \
  --include-function IOObjectRelease \
  --include-function IOIteratorNext \
  --include-function IOCreatePlugInInterfaceForService \
  --include-function IORegistryEntryCreateCFProperty \
  --include-function IONotificationPortCreate \
  --include-function IONotificationPortGetRunLoopSource \
  --include-function IORegistryEntryGetRegistryEntryID \
  --include-function IOServiceAddMatchingNotification \
  --include-function IOServiceMatching \
  --include-struct IOUSBDeviceStruct187 \
  --include-constant kIOUSBFindInterfaceDontCare \
  --include-struct IOUSBFindInterfaceRequest \
  --include-struct IOUSBDevRequest \
  --include-struct IOUSBInterfaceStruct190 \
  --include-constant kIOUSBTransactionTimeout \
  --include-constant kIOReturnAborted \
  --include-constant kIOUSBPipeStalled \
  --include-constant kUSBReEnumerateCaptureDeviceMask \
  --include-constant kUSBReEnumerateReleaseDeviceMask \
  --include-struct CFUUIDBytes \
  iokit_helper.h

# mach.h
$JEXTRACT --output ../../src/main/java \
  -I $SDK_DIR/usr/include \
  --header-class-name mach \
  --target-package net.codecrete.usb.macos.gen.mach \
  --include-function mach_error_string \
  $SDK_DIR/usr/include/mach/mach.h
