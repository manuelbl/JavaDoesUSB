set JEXTRACT=..\..\..\..\jextract-19\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l Winusb ^
  --header-class-name WinUSB ^
  --target-package net.codecrete.usb.windows.gen.winusb ^
  --include-function WinUsb_Initialize ^
  --include-function WinUsb_Free ^
  --include-function WinUsb_GetDescriptor ^
  --include-function WinUsb_ControlTransfer ^
  --include-function WinUsb_WritePipe ^
  --include-function WinUsb_ReadPipe ^
  windows_headers.h
