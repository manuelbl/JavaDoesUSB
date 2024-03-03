set JEXTRACT=..\..\..\..\jextract\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0

del /s /q ..\..\src\main\java\net\codecrete\usb\windows\gen
rmdir /s /q ..\..\src\main\java\net\codecrete\usb\windows\gen

call %JEXTRACT% --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l Kernel32 ^
 --header-class-name Kernel32 ^
  --target-package net.codecrete.usb.windows.gen.kernel32 ^
  --include-function CloseHandle ^
  --include-function GetModuleHandleW ^
  --include-function FormatMessageW ^
  --include-function LocalFree ^
  --include-constant ERROR_NO_MORE_ITEMS ^
  --include-constant ERROR_MORE_DATA ^
  --include-constant ERROR_INSUFFICIENT_BUFFER ^
  --include-constant ERROR_FILE_NOT_FOUND ^
  --include-constant ERROR_GEN_FAILURE ^
  --include-constant ERROR_NOT_FOUND ^
  --include-constant ERROR_IO_PENDING ^
  --include-constant GENERIC_READ ^
  --include-constant GENERIC_WRITE ^
  --include-constant FILE_SHARE_READ ^
  --include-constant FILE_SHARE_WRITE ^
  --include-constant FILE_ATTRIBUTE_NORMAL ^
  --include-constant FILE_FLAG_OVERLAPPED ^
  --include-constant OPEN_EXISTING ^
  --include-constant FORMAT_MESSAGE_ALLOCATE_BUFFER ^
  --include-constant FORMAT_MESSAGE_FROM_SYSTEM ^
  --include-constant FORMAT_MESSAGE_IGNORE_INSERTS ^
  --include-constant FORMAT_MESSAGE_FROM_HMODULE ^
  --include-constant INFINITE ^
  --include-struct _GUID ^
  --include-struct _OVERLAPPED ^
  windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l SetupAPI ^
  --header-class-name SetupAPI ^
  --target-package net.codecrete.usb.windows.gen.setupapi ^
  --include-function SetupDiDestroyDeviceInfoList ^
  --include-function SetupDiDeleteDeviceInterfaceData ^
  --include-struct _SP_DEVINFO_DATA ^
  --include-struct _SP_DEVICE_INTERFACE_DATA ^
  --include-struct _SP_DEVICE_INTERFACE_DETAIL_DATA_W ^
  --include-struct _DEVPROPKEY ^
  --include-struct _GUID ^
  --include-constant DIGCF_PRESENT ^
  --include-constant DIGCF_DEVICEINTERFACE ^
  --include-constant DEVPROP_TYPE_UINT32 ^
  --include-constant DEVPROP_TYPE_STRING ^
  --include-constant DEVPROP_TYPEMOD_LIST ^
  --include-constant DICS_FLAG_GLOBAL ^
  --include-constant DIREG_DEV ^
  windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  --header-class-name USBIoctl ^
  --target-package net.codecrete.usb.windows.gen.usbioctl ^
  --include-struct _USB_NODE_CONNECTION_INFORMATION_EX ^
  --include-struct _USB_DESCRIPTOR_REQUEST ^
  --include-struct _USB_DEVICE_DESCRIPTOR ^
  --include-struct _USB_ENDPOINT_DESCRIPTOR ^
  --include-struct _USB_PIPE_INFO ^
  --include-constant IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX ^
  --include-constant IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION ^
  windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       -l User32 ^
       --header-class-name User32 ^
       --target-package net.codecrete.usb.windows.gen.user32 ^
       --include-function DefWindowProcW ^
       --include-constant DEVICE_NOTIFY_WINDOW_HANDLE ^
       --include-constant HWND_MESSAGE ^
       --include-constant WM_DEVICECHANGE ^
       --include-constant DBT_DEVICEARRIVAL ^
       --include-constant DBT_DEVICEREMOVECOMPLETE ^
       --include-constant DBT_DEVTYP_DEVICEINTERFACE ^
       --include-struct tagMSG ^
       --include-struct tagPOINT ^
       --include-struct tagWNDCLASSEXW ^
       --include-struct _DEV_BROADCAST_HDR ^
       --include-struct _DEV_BROADCAST_DEVICEINTERFACE_W ^
       --include-struct _GUID ^
       windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       -l Winusb ^
       --header-class-name WinUSB ^
       --target-package net.codecrete.usb.windows.gen.winusb ^
       --include-function WinUsb_Free ^
       --include-constant PIPE_TRANSFER_TIMEOUT ^
       --include-constant RAW_IO ^
       windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       -l Advapi32 ^
       --header-class-name Advapi32 ^
       --target-package net.codecrete.usb.windows.gen.advapi32 ^
       --include-function RegQueryValueExW ^
       --include-function RegCloseKey ^
       --include-constant KEY_READ ^
       windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       -l Ole32 ^
       --header-class-name Ole32 ^
       --target-package net.codecrete.usb.windows.gen.ole32 ^
       --include-function CLSIDFromString ^
       windows_headers.h

call %JEXTRACT% --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       --header-class-name NtDll ^
       --target-package net.codecrete.usb.windows.gen.ntdll ^
       --include-constant STATUS_UNSUCCESSFUL ^
       windows_headers.h
