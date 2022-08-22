#!/bin/sh

set JEXTRACT=..\..\..\jextract\build\jextract\bin\jextract

:: handleapi.h
:: Generates more than 1000 files
:: Functions like CloseHandle() are generated multiple times but none of them in a public class
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name handleapi ^
::  --target-package net.codecrete.usb.windows.gen.handleapi ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\handleapi.h"

:: Generates code that produces run-time error:
::    java.lang.UnsatisfiedLinkError: unresolved symbol: CloseHandle
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name handleapi ^
::  --target-package net.codecrete.usb.windows.gen.handleapi ^
::  --include-function CloseHandle ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\handleapi.h"

:: Generates code that produces run-time error:
::    java.lang.UnsatisfiedLinkError: unresolved symbol: CloseHandle
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name handleapi ^
::  --target-package net.codecrete.usb.windows.gen.handleapi ^
::  --include-function CloseHandle ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\Windows.h"

:: winusb.h
:: Fails with error:
::   C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\shared\usb.h:32:9: error: unknown type name 'LARGE_INTEGER'
::   winusb.h requires that certain other header files (like Windows.h) are processed first
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name WinUSB ^
::  --target-package net.codecrete.usb.windows.gen.winusb ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\winusb.h"

:: cfgmgr32.h
:: Fails with error:
::   C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\shared\devpropdef.h:33:9: error: unknown type name 'ULONG'
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name CfgMgr32 ^
::  --target-package net.codecrete.usb.windows.gen.cfgmgr32 ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\cfgmgr32.h"

:: setupapi.h
:: Fails with error:
::   C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\setupapi.h:65:9: error: unknown type name 'GUID'
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name SetupApi ^
::  --target-package net.codecrete.usb.windows.gen.setupapi ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\setupapi.h"

:: usbioctl.h
:: Fails with error:
::   C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\setupapi.h:65:9: error: unknown type name 'GUID'
::%JEXTRACT% --source --output ../src/main/java ^
::  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
::  --header-class-name USBIOCtl ^
::  --target-package net.codecrete.usb.windows.gen.usbioctl ^
::  "C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\shared\usbioctl.h"
