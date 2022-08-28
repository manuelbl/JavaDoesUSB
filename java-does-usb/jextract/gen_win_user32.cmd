set JEXTRACT=..\..\..\jextract\build\jextract\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l User32 ^
  --header-class-name User32 ^
  --target-package net.codecrete.usb.windows.gen.user32 ^
  --include-function RegisterClassExW ^
  --include-function CreateWindowExW ^
  --include-function RegisterDeviceNotificationW ^
  --include-function GetMessageW ^
  --include-function DefWindowProcW ^
  --include-macro DEVICE_NOTIFY_WINDOW_HANDLE ^
  --include-macro HWND_MESSAGE ^
  --include-macro WM_DEVICECHANGE ^
  --include-macro DBT_DEVICEARRIVAL ^
  --include-macro DBT_DEVICEREMOVECOMPLETE ^
  --include-macro DBT_DEVTYP_DEVICEINTERFACE ^
  --include-struct tagMSG ^
  --include-struct tagWNDCLASSEXW ^
  --include-struct _DEV_BROADCAST_HDR ^
  --include-struct _DEV_BROADCAST_DEVICEINTERFACE_W ^
  windows_headers.h
