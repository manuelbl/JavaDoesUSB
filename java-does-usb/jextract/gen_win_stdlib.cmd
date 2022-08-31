set JEXTRACT=..\..\..\jextract-19\bin\jextract.bat
%JEXTRACT% --source --output ../src/main/java ^
  -D _AMD64_ -D _M_AMD64=100 -D UNICODE -D _UNICODE ^
  --header-class-name StdLib ^
  --target-package net.codecrete.usb.windows.gen.stdlib ^
  --include-function wcslen ^
  windows_headers.h
