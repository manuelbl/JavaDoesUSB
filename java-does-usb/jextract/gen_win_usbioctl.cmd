set JEXTRACT=..\..\..\jextract-19\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  --header-class-name USBIoctl ^
  --target-package net.codecrete.usb.windows.gen.usbioctl ^
  --include-macro IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX ^
  --include-macro IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION ^
  windows_headers.h
