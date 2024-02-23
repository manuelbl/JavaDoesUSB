# Code Generation with *jextract*

Some of the binding code for accessing native functions and data structures is generated with [jextract](https://jdk.java.net/jextract/). The tool is still under construction and has its limitations. 

In order to generate the code, the scripts in the subdirectories have to be run (`linux/gen_linux.sh`, `macos/gen_macos.sh` and `/windowsgen_win.cmd`). Each script has to be run on the particular operating system.

The code is generated in directories below `gen`, i.e. `main/java/net/codecrete/usb/linux/gen` and similar for the other operating systems. For each library (`xxx.so` or `xxx.dll`) and each macOS framework, a separate package is created.

The scripts explicitly specify the functions, structs etc. to include as generating code for entire operating system header files can result in an excessive amount of Java source files and classes.

The resulting code is then committed to the source code repository. Before the commit, imports are cleaned up to get rid of superfluous imports. Most IDEs provide a convenient command to execute this on entire directories.


## General limitations

- According to the jextract mailing list, it would be required to create separate code for Intel x64 and ARM64 architecture. And jextract would need to be run on each architecture separately (no cross-compilation). Fortunately, this doesn't seem to be the case. Linux code generated on Intel x64 also runs on ARM64 without change. The same holds for macOS. However, jextract needs to be run on each operating system separately.

- JDK 20 introduced a new feature for saving the thread-specific error values (`GetLastError()` on Windows, `errno` on Linux). To use it, an additional parameter must be added to function calls. Unfortunately, this is not yet supported by jextract. So a good number of function bindings have to be written manually.

- *jextract* is not really transparent about what it does. It often skips elements without providing any information. In particular, it will silently skip a requested element in these cases:

  - `--include-var myvar` if `myvar` is declared as `static`.
  - `--include-var myvar` if `myvar` is an `enum` constant. `enum` constants must be requested with `--include-constant`.
  - `--include-constant MYCONSTANT` if `MYCONSTANT` is function-like, even if it evaluates to a constant.
  - `--include-struct mystruct` if `mystruct` is actually a `typedef` to a `struct`.
  - `--include-typedef mystruct` if `mystruct` is actually a `struct`.
  - `--include-typedef mytypedef` if `mytypedef` is a `typedef` for a primitive type.

- *jextract* resolves all _typedef_s to their actual types. So this library does not use any _--include-typedef_ option. And there does not seem any obvious use for it.


## Linux

To run the script, the header files for *libudev* must be present. In most cases, they aren't install by default (in contrast to the library itself):

```
sudo apt-get install libudev-dev
```

On Linux, the limitations are:

- `usbdevice_fs.h`: The macro `USBDEVFS_CONTROL` and all similar ones are not generated. They are probably considered function-like macros. *jextract* does not generate code for function-like macros. `USBDEVFS_CONTROL` would evaluate to a constant.

- `sd-device.h` (header file for *libsystemd*): *jextract* fails with *"Error: /usr/include/inttypes.h:290:8: error: unknown type name 'intmax_t'"*. The reason is yet unknown. This code is currently not needed as *libudev* is used instead of *libsystemd*. They are related, *libsystemd* is the future solution, but it is missing support for monitoring devices.



## MacOS

Most of the required native functions on macOS are part of a framework. Frameworks internally have a more complex file organization of header and binary files than appears from the outside. Thus, they require a special logic to locate framework header files. *clang* supports it with the `-F`. *jextract* allows to specify the options via `compiler_flags.txt` file. Since the file must be in the local directory and since it does not apply to Linux and Windows, separate directories must be used for the operating systems.



## Windows

Most Windows SDK header files are not independent. They require `Windows.h` to be included first. So instead of specifying the target header files directly, a helper header file (`windows_headers.h` in this directory) is specified.

Compared to Linux and macOS, the code generation on Windows is very slow (about 1 min vs 3 seconds). And jextract crashes sometimes.

The known limitations are:

- Variable size `struct`: Several Windows struct are of variable size. The last member is an array. The `struct` definition specifies array length 1. But you are expected to allocate more space depending on the actual array size you need. *jextract* generates code for array length 1 and checks the length when the members are accessed. So the generated code is difficult to use. Variable size `struct`s are a pain - in any language.

- GUID constants like `GUID_DEVINTERFACE_USB_DEVICE` do not work. While code is generated, the code fails at run-time as it is unable to locate the symbol. This is due to the fact that `GUID_DEVINTERFACE_USB_DEVICE` actually resolve to a variable definition and not to a variable declaration. The GUID constant is not contained in any library; instead the header files use linkage options to generate the constant in the callers code, which does not work with FFM. Such constants should be skipped by *jextract*.

- *jextract* is a batch script and turns off *echo mode*. If a single batch scripts has multiple calls of *jextract*, two things need to be considered:

    - If the regular command interpreter `cmd.exe` is used, *jextract* must be called using `call`, i.e. `call jextract header.h`.
    - If *PowerShell* is used instead, `call` is not needed but *PowerShell* must be configured to allow the execution of scripts.
    - *jextract* turns off *echo mode*. So the first call will behave differently than the following calls.


## Code Size

*jextract* generates a comprehensive set of methods for each function, struct, struct member etc. Most of it will not be used as a typical application just uses a subset of struct members, might only read or write them etc. So a considerable amount of code is generated. For some types, it's a bit excessive.

The worst example is [`IOUSBInterfaceStruct190`](https://github.com/manuelbl/JavaDoesUSB/blob/main/java-does-usb/src/main/java/net/codecrete/usb/macos/gen/iokit/IOUSBInterfaceStruct190.java) (macOS). This is a `struct` consisting of about 50 member functions. It's basically a vtable of a C++ class. For this single `struct`, *jextract* generates codes resulting in 100 class files with a total size of 213kByte.

The table below shows class file size statistics for version 1.0.0 of the library:

| Operating Systems | Manually Created |     % | Generated |     % |     Total |       % |
|-------------------|-----------------:|------:|----------:|------:|----------:|--------:|
| Linux             |           54,022 |  4.3% |   162,099 | 12.8% |   216,121 |   17.1% |
| macOS             |           77,149 |  6.1% |   529,347 | 41.8% |   606,496 |   47.9% |
| Windows           |          106,358 |  8.4% |   232,395 | 18.3% |   338,753 |   26.7% |
| Common            |          105,423 |  8.3% |           |       |   105,423 |    8.3% |
| Grand Total       |          342,952 | 27.1% |   923,841 | 72.9% | 1,266,793 | 100.00% |


*Class File Size (compiled), in bytes and percentage of total size*

If *jextract* could generate code for error state capturing, there would be even more generated and less manually written code.
