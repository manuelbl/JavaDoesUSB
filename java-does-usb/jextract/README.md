# Code Generation with *jextract*

Some of the binding code for accessing native functions and data structures is generated with [jextract](https://jdk.java.net/jextract/). The tool is still under construction and has its limitations. 

In order to generate the code, the scripts in this directory have to be run (`gen_linux.sh`, `gen_macos.sh` and `gen_win_xxx.cmd`). Each script has to be run on that particular operating system.

The code is generated in directories below `gen`, i.e. `main/java/net/codecrete/usb/linux/gen` and similar for the other operating systems. For each library (`xxx.so` or `xxx.dll`) and each macOS framework, a separate package is created.

The scripts explicitly specify the functions, structs etc. to include as generating code for entire operating system header files can result in an excessive amount of Java source files and classes.

The resulting code is then committed to the source code repository. Before the commit, imports are cleaned up to get rid of superfluous imports. Most IDEs provide a convenient command to execute this on entire directories.


## General limitations

- Binaries of *jextract* can be downloaded from https://jdk.java.net/jextract/. Only x64 binaries are available. It is unclear if the x64 jextract run on macOS with Apple Silicon (ARM64) using the Rosetta emulator produces valid bindings. So far, not problems have been detected.


## Linux

To run the script, most likely the header files for *libudev* must be installed (the library itself is most likely already installed):

```
sudo apt-get install libudev-dev
```

On Linux, *jextract* is very successful. The only limitations are:

- `usbdevice_fs.h`: The macro `USBDEVFS_CONTROL` (and all similar ones) are not generated. They are probably considered a function-like macro. *jextract* does not generate code for function-like macro. But `USBDEVFS_CONTROL` is actually a constant.

- `sd-device.h` (header file for *libsystemd*): *jextract* fails with *"Error: /usr/include/inttypes.h:290:8: error: unknown type name 'intmax_t'"*. The reason is yet unknown. This code is currently not needed as it uses *libudev* instead of *libsystemd*. They are related, *libsystemd* is the future solution, but it is missing support for monitoring devices.

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

On Windows, several scripts must be run as *jextract* is a batch file that never returns. It's not possible to call it multiple times from a single script.

Most Windows SDK header files are not independent. They require that `Windows.h` is included first. So instead of specifying the target header files directly, a helper header file (`windows_headers.h` in this directory) is specified.

The known limitations are:

- `typedef` to `struct`: If only the *typedef* is specified (`--include-typedef`), an empty Java class is generated. If both *typedef* and the *struct* it refers to are specified, the *typedef* class is still empty but inherits from the *struct* class, which contains all the *struct* members.

- Variable size *struct*: Several Windows struct are of variable size. The last member is an array. The *struct* definition specifies array length 1. But you are expected to allocate more space depending on the actual array size you need. *jextract* generates code for array length 1 and when access array members, the length is checked. So the generated code isn't really usable.

- `USB_NODE_CONNECTION_INFORMATION_EX`: This struct uses a packed layout without considering alignment. The last four members are on an offset even though they are multiple bytes long. *jextract* creates the correct offsets but defines strict alignment constraints for the members. So the memory layout cannot be instantiated as it throws an exception.

- GUID constants like `GUID_DEVINTERFACE_USB_DEVICE` do not work. While code is generated, the code fails at run-time as it is unable to locate the symbol. This is due to the fact that `GUID_DEVINTERFACE_USB_DEVICE` actually resolve to a variable defintion and not to a variable declaration. So there is no such symbol in any library. Such constants should be skipped by *jextract*.
