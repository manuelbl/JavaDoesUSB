# Java Does USB: USB Library for Java

[![javadoc](https://javadoc.io/badge2/net.codecrete.usb/java-does-usb/javadoc.svg)](https://javadoc.io/doc/net.codecrete.usb/java-does-usb)

*Java Does USB* is a Java library for working with USB devices. It allows to query information about all conntected USB devices and to communicate with USB devices using custom / vendor specific protocols. (It is not intended for communication with standard types of USB devices such as mass storage devices, keyboards etc.)

The library uses the [Foreign Function & Memory API](https://github.com/openjdk/panama-foreign) to access native APIs of the underlying operating system. It is written entirely in Java and does not need JNI or any native third-party library. The *Foreign Function & Memory API* (aka as project Panama) has been introduced with Java 22. Older versions of this library can be used with preview versions of the API that were available in Java 19, Java 20 or Java 21 (with preview features enabled).

| Version | Main New Features | Compatibility |
| - | - | - |
| 1.0.x | Release for final Java API | JDK 22 |
| 0.7.x | New setter/getter names for improved Kotlin support; Kotlin examples | JDK 21 |
| 0.6.x | Support for JDK 21; better handling of composite devices on Windows | JDK 21 |
| 0.5.x | Support for JDK 20; high-throuput I/O streams | JDK 20 |
| 0.4.x | Early release | JDK 19 |

*Note: The main branch and published versions ≥ 1.0 work with JDK 22 and later only. For JDK 21, user version 0.7.*. For JDK 20, use version 0.5.*. For JDK 19, use version 0.4.x.


## Features

- Single API for all operating systems (similar to WebUSB API)
- Enumeration of USB devices
- Control, bulk and interrupt transfers (optionally with timeout)
- Notifications about connected/disconnected devices
- Descriptive information about interfaces, settings and endpoints
- High-throughput input/output streams
- Support for alternate interface settings, composite devices and interface association
- Published on Maven Central

### Planned

- Isochronous transfer

### Not planned

- Changing configuration: The library selects the first configuration. Changing configurations is rarely used and not supported on Windows (limitation of WinUSB).
- USB 3.0 streams: Not supported on Windows (limitation of WinUSB).
- Providing information about USB buses, controllers and hubs


## Getting Started

The library is available at Maven Central. To use it, just add it to your Maven or Gradle project.

If you are using Maven, add the below dependency to your pom.xml:

```xml
<dependency>
      <groupId>net.codecrete.usb</groupId>
      <artifactId>java-does-usb</artifactId>
      <version>1.0.0-SNAPSHOT</version>
</dependency>
```

If you are using Gradle, add the below dependency to your build.gradle file:

```groovy
compile group: 'net.codecrete.usb', name: 'java-does-usb', version: '1.0.0-SNAPSHOT'
```

```java
package net.codecrete.usb.sample;

import net.codecrete.usb.USB;

public class EnumerateDevices {

    public static void main(String[] args) {
        for (var device : USB.getAllDevices()) {
            System.out.println(device);
        }
    }
}
```


## Documentation

- [Javadoc](https://javadoc.io/doc/net.codecrete.usb/java-does-usb) 


## Examples

- [Bulk Transfer](examples/bulk_transfer/) demonstrates how to find a USB device, open it and communicate using bulk transfer.
- Enumeration ([Java](examples/enumerate/) / [Kotlin](examples/enumerate_kotlin/)) lists all connected USB devices and displays information about interfaces and endpoints.
- Monitor ([Java](examples/monitor/) / [Kotlin](examples/monitor_kotlin/)) lists the connected USB devices and then monitors for devices being connected and disconnnected.
- [Device Firmware Upload (DFU) for STM32](examples/stm_dfu) uploads firmware to STM32 microcontrollers supporting the built-in DFU mode.
- [ePaper Display](examples/epaper_display) communicates with an IT8951 controller for e-Paper displays and shows an image on the display.


## Prerequisite

- Java 22 (available at https://www.azul.com/downloads/?package=jdk)
- Windows (x86 64-bit), macOS (x86 64-bit, ARM 64-bit) or Linux 64 bit (x86 64-bit, ARM 64-bit)

For JDK 21, use the latest published version 0.7.x. For JDK 20, use the latest published version 0.5.x. For JDK 19, use the latest published version 0.4.x.


## Platform-specific Considerations


### macOS

No special considerations apply. Using this library, a Java application can connect to any USB device and claim any interfaces that isn't claimed by an operating system driver or another application. Standard operation-system drivers can be unloaded if the application is run with root privileges. It runs both on Macs with Apple Silicion and Intel processors.


### Linux

*libudev* is used to discover and monitor USB devices. It is closely tied to *systemd*. So the library only runs on Linux distributions with *systemd* and the related libraries. The majority of Linux distributions suitable for desktop computing (as opposed to distributions optimized for containers) fulfill this requirement. It runs on both Intel and ARM64 processors.

Similar to macOS, a Java application can connect to any USB device and claim any interfaces that isn't claimed by an operating system driver or another application. Standard operation system drivers can be unloaded (without the need for root privileges).

Most Linux distributions by default set up user accounts without permissions to access USB devices directly. The *udev* system daemon is responsible for assigning permissions to USB devices. It can be configured to assign specific permissions or ownership:

Create a file called `/etc/udev/rules.d/80-javadoesusb-udev.rules` with the below content:

```text
SUBSYSTEM=="usb", ATTRS{idVendor}=="cafe", MODE="0666"
```

This adds the rule to assign permission mode 0666 to all USB devices with vendor ID `0xCAFE`. This unregistered vendor ID is used by the test devices.


### Windows

The Windows driver model is more rigid than the ones of macOS or Linux. It's not possible to open any USB device that is not claimed. Instead, only devices using the *WinUSB* driver can be opened. This even applies to devices with no installed driver.

USB devices can implement certain control requests to instruct Windows to automatically install the WinUSB driver (search for *WCID* or *Microsoft OS Compatibility Descriptors*). The WinUSB driver can also be manually installed or replaced using a software called [Zadig](https://zadig.akeo.ie/).

The test devices implement the required control requests. So the driver is installed automatically.

Windows for ARM64 is not yet supported. A port is probably easy, provided you have hardware to test it.


### Troubleshooting

### 32-bit versions

The Foreign Function & Memory API has not been implemented for 32-bit operating systems / JDKs  (and likely never will be).



## Code generation

Many bindings for the native APIs have been generated with *jextract*. See the [jextract](java-does-usb/jextract) subdirectory for more information. For functions that need to retain the error state (`errno` on Linux, `GetLastError()` on Windows), the bindings have been manually written as *jextract* does not support it.



## Testing

In order to run the unit tests, a special test device must be connected to the computer, which can be easily created from very inexpensive microcontroller boards. Two variants exist:

- [loopback-stm32](test-devices/loopback-stm32)
- [composite-stm32](test-devices/composite-stm32)

The test device with the *loopback-stm32* code supports all tests. If the test device with the *composite-stm32* code is connected, some tests are skipped. However, if it is used, the correct handling of composite devices is verified.

Tests can be run from the command line:

```
mvn clean test
```

If they are run from an IDE (such as IntelliJ IDEA), you must likely configure VM options to enable preview features and allow native access:

```
--enable-native-access=net.codecrete.usb
```

Or (if modules are ignored):

```
--enable-native-access=ALL-UNNAMED
```
