set JEXTRACT=..\..\..\jextract\build\jextract\bin\jextract.bat

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
