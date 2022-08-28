set JEXTRACT=..\..\..\jextract\build\jextract\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../src/main/java ^
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
  --include-struct _SP_DEVINFO_DATA ^
  --include-struct _SP_DEVICE_INTERFACE_DATA ^
  --include-struct _SP_DEVICE_INTERFACE_DETAIL_DATA_W ^
  --include-macro DIGCF_PRESENT ^
  --include-macro DIGCF_DEVICEINTERFACE ^
  --include-macro SPDRP_ADDRESS ^
  --include-macro DEVPROP_TYPE_UINT32 ^
  --include-macro DEVPROP_TYPE_STRING ^
  windows_headers.h
