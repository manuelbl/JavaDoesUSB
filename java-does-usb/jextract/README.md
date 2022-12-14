# Code Generation with *jextract*

Some of the binding code for accessing native functions and data structures is generated with [jextract](https://jdk.java.net/jextract/). The tool is still under construction and has its limitations. 

In order to generate the code, the scripts in this directory have to be run (`gen_linux.sh`, `gen_macos.sh` and `gen_win_xxx.cmd`). Each script has to be run on that particular operating system.

The code is generated in directories below `gen`, i.e. `main/java/net/codecrete/usb/linux/gen` and similar for the other operating systems. For each library (`xxx.so` or `xxx.dll`) and each macOS framework, a separate package is created.

The scripts explicitly specify the functions, structs etc. to include as generating code for entire operating system header files can result in an excessive amount of Java source files and classes.

The resulting code is then committed to the source code repository. Before the commit, imports are cleaned up to get rid of superfluous imports. Most IDEs provide a convenient command to execute this on entire directories.


## General limitations

- Binaries of *jextract* can be downloaded from https://jdk.java.net/jextract/. x64 binaries are available but no ARM64 binaries. According to the mailing list, cross-compiling is not possible, i.e. ARM64 binaries are needed on macOS with Apple Silicon. But so far, the x64 binaries (using the Rosetta2 emulation) have worked without problems.

- `typedef` and `struct`:

  1. If only the `typedef` is included (`--include-typedef`), an empty Java class is generated.
  2. If both *typedef* and the `struct` it refers to are included, the `typedef` class inherits from the `struct` class, which contains all the `struct` members.
  3. If the `typedef` refers to an unnamed `struct`, the generated class contains all the `struct` members.
  
  Case 1 looks like a bug.

- *jextract* is not really transparent about what it does. It often skips elements without providing any information. In particular, it will silently skip a requested element in these cases:

  - `--include-var myvar` if `myvar` is declared as `static`.
  - `--include-var myvar` if `myvar` is an `enum` constant. `enum` constants must be requested with `--include-macro`.
  - `--include-macro MYMACRO` if `MYMACRO` is function-like, even if it evaluates to a constant.
  - `--include-struct mystruct` if `mystruct` is actually a `typedef` to a `struct`.
  - `--include-typedef mystruct` if `mystruct` is actually a `struct`.
  - `--include-typedef mytypedef` if `mytypedef` is a `typedef` for a primitive type.



## Linux

To run the script, most likely the header files for *libudev* must be installed (the library itself is most likely already installed):

```
sudo apt-get install libudev-dev
```

On Linux, the limitations are:

- `usbdevice_fs.h`: The macro `USBDEVFS_CONTROL` and all similar ones are not generated. They are probably considered function-like macros. *jextract* does not generate code for function-like macros. But `USBDEVFS_CONTROL` evaluates to a constant.

- `sd-device.h` (header file for *libsystemd*): *jextract* fails with *"Error: /usr/include/inttypes.h:290:8: error: unknown type name 'intmax_t'"*. The reason is yet unknown. This code is currently not needed as *libudev* is used instead of *libsystemd*. They are related, *libsystemd* is the future solution, but it is missing support for monitoring devices.

- `libudev.h`: After code generation, the class `RuntimeHelper.java` in `.../linux/gen/udev` must be manually modified as the code to access the library does not work for the directory the library is located in. So replace:

```
System.loadLibrary("udev");
SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
```

with:

```
SymbolLookup loaderLookup = SymbolLookup.libraryLookup("libudev.so", MemorySession.openImplicit());
```


## MacOS

Most of the required native functions on macOS are part of a framework. Framework internally have a more complex file organization of header and binary files than appears on the outside. Thus they require a special logic to locate framework header files. *clang* supports it with the `-F`. *jextract* allows to specify the options via `compiler_flags.txt` file. Since the file must be in the local directory and since it does not apply to Linux and Windows, separate directories must be used for the operating systems.

The generated code has the same problem as the Linux code for *udev*. It must be manually changed to use `SymbolLookup.libraryLookup()` for the libraries `CoreFoundation.framework/CoreFoundation` and `IOKit.framework/IOKit` respectively.


## Windows

Most Windows SDK header files are not independent. They require that `Windows.h` is included first. So instead of specifying the target header files directly, a helper header file (`windows_headers.h` in this directory) is specified.

The known limitations are:

- Variable size `struct`: Several Windows struct are of variable size. The last member is an array. The `struct` definition specifies array length 1. But you are expected to allocate more space depending on the actual array size you need. *jextract* generates code for array length 1 and when access array members, the length is checked. So the generated code is difficult to use. Variable size `struct`s are a pain - in any language.

- `USB_NODE_CONNECTION_INFORMATION_EX`: This struct uses a packed layout without considering alignment. The last four members are on an odd offset even though they are multiple bytes long. *jextract* creates the correct offsets but defines strict alignment constraints for the members. So the memory layout cannot be instantiated as it throws an exception.

- GUID constants like `GUID_DEVINTERFACE_USB_DEVICE` do not work. While code is generated, the code fails at run-time as it is unable to locate the symbol. This is due to the fact that `GUID_DEVINTERFACE_USB_DEVICE` actually resolve to a variable definition and not to a variable declaration. It is not part of any library. Such constants should be skipped by *jextract*.

- *jextract* is a batch script and turns off *echo mode*. If a single batch scripts has multiple calls of *jextract*, two things need to be considered:

    - If the regular command interpreter `cmd.exe` is used, *jextract* must be called using `call`, i.e. `call jextract header.h`.
    - If *PowerShell* is used instead, `call` is not needed but *PowerShell* must be configured to allow the execution of scripts.
    - *jextract* turns off *echo mode*. So the first call will behave differently than the following calls.


## Code Size

*jextract* generates a comprehensive set of methods for each function, struct, struct member etc. Most of it will not be used as a typical application just uses a subset of struct members, might only read or write them etc. So a considerable amount of code is generated. For some types, it's a bit excessive.

The worst example is [`IOUSBInterfaceStruct942`](https://github.com/manuelbl/JavaDoesUSB/blob/main/java-does-usb/src/main/java/net/codecrete/usb/macos/gen/iokit/IOUSBDeviceStruct942.java) (macOS). This is a `struct` consisting of about 75 member functions. It's bascially a vtable of a C++ class. *jextract* generates the same number of classes plus a huge class for the struct itself. The total code size (compiled) for this single `struct` is over 300 kByte.

The table below shows statictics for version 0.2.0 of the library:

| Operating Systems | Manually Created | % | Generated | % | Total | % |
| - | -:| -:| -:| -:| -:| -:|
| Linux       |  24,140 |  1.96% |   182,774 | 14.84% |   206,914 |  16.80% |
| macOS       |  57,788 |  4.69% |   666,037 | 54.08% |   723,825 |  58.77% |
| Windows     |  46,085 |  3.74% |   201,000 | 16.32% |   247,085 |  20.06% |
| Common      |  53,774 |  4.37% |    53,774 |  0.00% |           |   4.37% |
| Grand Total | 181,787 | 14.76% | 1,049,811 | 85.24% | 1,231,598 | 100.00% |

*Code Size (compiled), in bytes and percentage of total size*
