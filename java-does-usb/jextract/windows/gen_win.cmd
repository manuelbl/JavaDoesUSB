set JEXTRACT=..\..\..\..\jextract-19\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0

call %JEXTRACT% --source --output ../../src/main/java ^
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
  --include-function FormatMessageW ^
  --include-function LocalFree ^
  --include-macro ERROR_SUCCESS ^
  --include-macro ERROR_NO_MORE_ITEMS ^
  --include-macro ERROR_MORE_DATA ^
  --include-macro ERROR_INSUFFICIENT_BUFFER ^
  --include-macro ERROR_FILE_NOT_FOUND ^
  --include-macro ERROR_SEM_TIMEOUT ^
  --include-macro GENERIC_READ ^
  --include-macro GENERIC_WRITE ^
  --include-macro FILE_SHARE_READ ^
  --include-macro FILE_SHARE_WRITE ^
  --include-macro FILE_ATTRIBUTE_NORMAL ^
  --include-macro FILE_FLAG_OVERLAPPED ^
  --include-macro OPEN_EXISTING ^
  --include-macro FORMAT_MESSAGE_ALLOCATE_BUFFER ^
  --include-macro FORMAT_MESSAGE_FROM_SYSTEM ^
  --include-macro FORMAT_MESSAGE_IGNORE_INSERTS ^
  --include-struct _GUID ^
  --include-typedef GUID ^
  windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l SetupAPI ^
  --header-class-name SetupAPI ^
  --target-package net.codecrete.usb.windows.gen.setupapi ^
  --include-function SetupDiGetClassDevsW ^
  --include-function SetupDiDestroyDeviceInfoList ^
  --include-function SetupDiEnumDeviceInfo ^
  --include-function SetupDiEnumDeviceInterfaces ^
  --include-function SetupDiGetDeviceInterfaceDetailW ^
  --include-function SetupDiGetDeviceRegistryPropertyW ^
  --include-function SetupDiGetDevicePropertyW ^
  --include-function SetupDiOpenDeviceInterfaceW ^
  --include-function SetupDiCreateDeviceInfoList ^
  --include-function SetupDiOpenDeviceInfoW ^
  --include-function SetupDiOpenDevRegKey ^
  --include-function SetupDiDeleteDeviceInterfaceData ^
  --include-struct _SP_DEVINFO_DATA ^
  --include-typedef SP_DEVINFO_DATA ^
  --include-struct _SP_DEVICE_INTERFACE_DATA ^
  --include-typedef SP_DEVICE_INTERFACE_DATA ^
  --include-struct _SP_DEVICE_INTERFACE_DETAIL_DATA_W ^
  --include-typedef SP_DEVICE_INTERFACE_DETAIL_DATA_W ^
  --include-macro DIGCF_PRESENT ^
  --include-macro DIGCF_DEVICEINTERFACE ^
  --include-macro SPDRP_ADDRESS ^
  --include-macro DEVPROP_TYPE_UINT32 ^
  --include-macro DEVPROP_TYPE_STRING ^
  --include-macro DEVPROP_TYPEMOD_LIST ^
  --include-macro DICS_FLAG_GLOBAL ^
  --include-macro DIREG_DEV ^
  windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  --header-class-name StdLib ^
  --target-package net.codecrete.usb.windows.gen.stdlib ^
  --include-function wcslen ^
  windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  --header-class-name USBIoctl ^
  --target-package net.codecrete.usb.windows.gen.usbioctl ^
  --include-macro IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX ^
  --include-macro IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION ^
  windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
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
       --include-typedef MSG ^
       --include-struct tagWNDCLASSEXW ^
       --include-typedef WNDCLASSEXW ^
       --include-struct _DEV_BROADCAST_HDR ^
       --include-typedef DEV_BROADCAST_HDR ^
       --include-struct _DEV_BROADCAST_DEVICEINTERFACE_W ^
       --include-typedef DEV_BROADCAST_DEVICEINTERFACE_W ^
       windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
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
       --include-function WinUsb_GetAssociatedInterface ^
       --include-function WinUsb_SetPipePolicy ^
       --include-function WinUsb_SetCurrentAlternateSetting ^
       --include-function WinUsb_ResetPipe ^
       --include-macro PIPE_TRANSFER_TIMEOUT ^
       windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       -l Advapi32 ^
       --header-class-name Advapi32 ^
       --target-package net.codecrete.usb.windows.gen.advapi32 ^
       --include-function RegQueryValueExW ^
       --include-function RegCloseKey ^
       --include-macro REG_MULTI_SZ ^
       --include-macro KEY_READ ^
       windows_headers.h

call %JEXTRACT% --source --output ../../src/main/java ^
       -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
       -I "%SDK_DIR%\um" ^
       -I "%SDK_DIR%\shared" ^
       -l Ole32 ^
       --header-class-name Ole32 ^
       --target-package net.codecrete.usb.windows.gen.ole32 ^
       --include-function CLSIDFromString ^
       windows_headers.h
