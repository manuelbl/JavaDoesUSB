set JEXTRACT=..\..\..\jextract\build\jextract\bin\jextract.bat
set SDK_DIR=C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0
%JEXTRACT% --source --output ../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  -I "%SDK_DIR%\um" ^
  -I "%SDK_DIR%\shared" ^
  -l Cfgmgr32 ^
  --header-class-name CfgMgr32 ^
  --target-package net.codecrete.usb.windows.gen.cfgmgr32 ^
  --include-function CM_Get_Parent ^
  --include-function CM_Get_Device_IDW ^
  windows_headers.h
