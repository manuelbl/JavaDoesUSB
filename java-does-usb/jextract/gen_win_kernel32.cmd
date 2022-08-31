set JEXTRACT=..\..\..\jextract-19\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l Kernel32 ^
  --header-class-name Kernel32 ^
  --target-package net.codecrete.usb.windows.gen.kernel32 ^
  --include-function CreateFileW ^
  --include-function CloseHandle ^
  --include-function DeviceIoControl ^
  --include-function GetLastError ^
  --include-function GetModuleHandleW ^
  --include-macro ERROR_SUCCESS ^
  --include-macro ERROR_NO_MORE_ITEMS ^
  --include-macro ERROR_INSUFFICIENT_BUFFER ^
  --include-macro GENERIC_READ ^
  --include-macro GENERIC_WRITE ^
  --include-macro FILE_SHARE_READ ^
  --include-macro FILE_SHARE_WRITE ^
  --include-macro FILE_ATTRIBUTE_NORMAL ^
  --include-macro FILE_FLAG_OVERLAPPED ^
  --include-macro OPEN_EXISTING ^
  --include-struct _GUID ^
  --include-typedef GUID ^
  windows_headers.h
