# Java Does USB: USB library for Java

*Java Does USB* is a library for working with USB devices from Java. It allows to communicate with USB devices other than storage devices, keyboards etc., which implement standard USB classes the operating system takes care of. Typical cases are USB devices with a custom vendor-specific protocol.

The library uses the [Foreign Function & Memory API](https://github.com/openjdk/panama-foreign) to access native APIs of the underlying operating system. It only uses Java code and does not need JNI or any native third-party library.

The Foreign Function & Memory API (aka as project Panama) is in preview and will be introduced in a future Java version. Currently, it can be tested with Java 19 Early Access (with preview features enabled).


## Prerequisite

- Java 19 Early Access, preview features enabled (available at https://www.azul.com/downloads/?package=jdk)
- Windows (x86 64-bit), macOS (x86 64-bit, ARM 64-bit) or Linux 64 bit (x86 64-bit, ARM 64-bit)

It has been tested with Azul Zulu 19.0.75 EA 33.


## Testing

In order to run the unit tests, a special test device must be connected to the computer. See the [loopback-stm32](test-devices/loopback-stm32) directory.

Tests can be run from the command line:

```
mvn clean test
```

If they are run from an IDE (such as IntelliJ IDEA), you must likely configure VM options to enable preview features and allow native access:

```
--enable-preview --enable-native-access=net.codecrete.usb
```

Or (if modules are ignored):

```
--enable-preview --enable-native-access=ALL-UNNAMED
```

If you don't have the test device, you can get a glimpse at the library running the below command. It enumerates all connected USB devices.

```
MAVEN_OPTS="--enable-preview --enable-native-access=ALL-UNNAMED" mvn install exec:java -Dexec.classpathScope="test" -DskipTests -Dexec.mainClass="net.codecrete.usb.sample.EnumerateDevices"
```


## Features

### Implemented

- Single API for all operating systems (similar to WebUSB API)
- Enumeration of USB devices
- Control transfer
- Bulk transfer

### To do

- Interrupt transfer
- Isochronous transfer
- Notification about connected/disconnected devices
- Descriptive information about interfaces, settings and endpoints
- Alternate interface settings
- Composite devices (on Windows)
- Generate and publish JavaDoc documentation
- Make library available on Maven Central

### Not planned

- Changing configuration: The library selects the first configuration. Changing configurations is rarely used and not supported on Windows.
- USB 3.0 streams: Not supported on Windows.
- Providing information about USB buses, controllers and hubs


## Platform-specific considerations


### macOS

No special considerations apply. Using this library, a Java application can connect to any USB device and claim any interfaces that aren't claimed by an operating system driver or another application.


### Linux

*libsystemd* is used to discover USB devices. So it only runs on Linux distributions with systemd and the related library. The majority of Linux distributions suitable for desktop computing (as opposed to distributions optimized for containers) fulfills this.

Similar to macOS, a Java application can connect to any USB device and claim any interfaces that aren't claimed by an operating system driver or another application.

Most Linux distributions by default set up user accounts without the permission to access USB devices directly. The *udev* system daemon is responsible for assigning permissions to USB devices. It can be configured to assign specific permissions or ownership.

Create a file called `/etc/udev/rules.d/80-javadoesusb-udev.rules` with the below content:

```text
SUBSYSTEM=="usb", ATTRS{idVendor}=="cafe", MODE="0666"
```

This adds the rule to assign permission mode 0666 to all USB devices with vendor ID `0xCAFE`. This non-register vendor ID is used by test device.


### Windows

The Windows driver model is more rigid than the ones of macOS or Linux. It's not possible to open any USB device by default. Instead, only devices using the WinUSB driver can be opened. This even applies to devices with no installed driver.

USB devices can implement certain control requests to instruct Windows to automatically install the WinUSB driver (search for WCID or Microsoft OS Compatibility Descriptors). The driver can also be manually installed or replaced using a software called [Zadig](https://zadig.akeo.ie/).

The test device implements these control requests. So the driver is installed automatically.

This library does not yet run reliably on Windows as Java VM sometimes overwrites the last error code, which is not just needed in error cases. It works incorrectly when run in the debugger and sometimes even without the debugger. A future version of the Foreign Function & Memory API will hopefully provide a way to save the last error code. The developers are aware of the issue.

The library has not been tested on Windows for ARM64. It might or might not work.


### 32-bit versions

The Foreign Function & Memory API has not been implemented for 32-bit operating systems / JDKs. So it does not support them (and likely never will).
