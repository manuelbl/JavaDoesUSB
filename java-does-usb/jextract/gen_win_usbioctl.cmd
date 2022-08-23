set JEXTRACT=..\..\..\jextract\build\jextract\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  --header-class-name USBIoctl ^
  --target-package net.codecrete.usb.windows.gen.usbioctl ^
  --include-struct _USB_NODE_CONNECTION_INFORMATION_EX ^
  windows_usbioctl.h

:: Generated code is invalid: No instance of type _USB_NODE_CONNECTION_INFORMATION_EX can be created.0
:: They crash with:
::   java.lang.UnsupportedOperationException: Invalid alignment requirements for layout s16(DeviceAddress)
