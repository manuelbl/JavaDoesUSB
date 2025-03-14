# Java Does USB: USB Library for Java

[![javadoc](https://javadoc.io/badge2/net.codecrete.usb/java-does-usb/javadoc.svg)](https://javadoc.io/doc/net.codecrete.usb/java-does-usb)

*Java Does USB* is a Java library for working with USB devices. It allows to query the conntected USB devices and to communicate with them using custom / vendor specific protocols. It is not intended for communication with standard types of USB devices such as mass storage devices, keyboards etc.

The library uses the [Foreign Function and Memory API](https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html) to access native APIs of the underlying operating system. It is written entirely in Java and does not use JNI or any native third-party library. The *Foreign Function and Memory API* has been introduced with Java 22.



## Features

- Single API for all operating systems (similar to WebUSB API)
- Enumeration of USB devices
- Control, bulk and interrupt transfers (optionally with timeout)
- Notifications about connected/disconnected devices
- Descriptive information about interfaces, settings and endpoints
- High-throughput input/output streams
- Support for alternate interface settings, composite devices and interface association
- Published on Maven Central and licensed under the permissive MIT license



## Getting Started

The library is available at Maven Central. To use it, just add it to your Maven or Gradle project.

If you are using Maven, add the below dependency to your pom.xml:

```xml
<dependency>
      <groupId>net.codecrete.usb</groupId>
      <artifactId>java-does-usb</artifactId>
      <version>1.2.0</version>
</dependency>
```

If you are using Gradle, add the below dependency to your build.gradle file:

```groovy
compile group: 'net.codecrete.usb', name: 'java-does-usb', version: '1.2.0'
```

```java
package net.codecrete.usb.sample;

import net.codecrete.usb.Usb;

public class EnumerateDevices {

    public static void main(String[] args) {
        for (var device : Usb.getDevices()) {
            System.out.println(device);
        }
    }
}
```



## Documentation

- [Code Examples](https://github.com/manuelbl/JavaDoesUSB/wiki/Java-Does-USB-By-Examples)
- [Javadoc](https://javadoc.io/doc/net.codecrete.usb/java-does-usb)



## Examples

- [Bulk Transfer](examples/bulk_transfer/) demonstrates how to find a USB device, open it and communicate using bulk transfer.
- Enumeration ([Java](examples/enumerate/) / [Kotlin](examples/enumerate_kotlin/)) lists all connected USB devices and displays information about interfaces and endpoints.
- Monitor ([Java](examples/monitor/) / [Kotlin](examples/monitor_kotlin/)) lists the connected USB devices and then monitors for devices being connected and disconnected.
- [Device Firmware Upload (DFU) for STM32](examples/stm_dfu) uploads firmware to STM32 microcontrollers supporting the built-in DFU mode.
- [ePaper Display](examples/epaper_display) communicates with an IT8951 controller for e-Paper displays and shows an image on the display.



## Prerequisite

- Java 22 or higher, available at [jdk.java.net](https://jdk.java.net/), [Azul](https://www.azul.com/downloads/?package=jdk), [Adoptium](https://adoptium.net/temurin/releases/) or with your favorite package manager.
- Windows (x86 64-bit, ARM 64-bit), macOS (x86 64-bit, ARM 64-bit) or Linux 64 bit (x86 64-bit, ARM 64-bit).



## Platform-specific Considerations


### macOS

No special considerations apply. Using this library, a Java application can connect to any USB device and claim any interface that isn't claimed by an operating system driver or another application. Standard operation system drivers can be unloaded if the application is run with *root* privileges. It runs both on Macs with Apple Silicon and Intel processors.


### Linux

*libudev* is used to discover and monitor USB devices. It is closely tied to *systemd*. So the library runs on Linux distributions with *systemd* and the related libraries. The majority of Linux distributions suitable for desktop computing (as opposed to distributions optimized for containers) fulfill this requirement. It runs on both Intel/AMD and ARM processors.

Similar to macOS, a Java application can connect to any USB device and claim any interface that isn't claimed by an operating system driver or another application. Standard operation system drivers can be unloaded (without the need for root privileges).

Most Linux distributions set up user accounts without permissions to access USB devices. The *udev* system daemon is responsible for assigning permissions to USB devices. It can be configured to assign specific permissions or ownership:

Create a file called `/etc/udev/rules.d/80-javadoesusb-udev.rules` with the below content:

```text
SUBSYSTEM=="usb", ATTRS{idVendor}=="cafe", MODE="0666"
```

This adds the rule to assign permission mode 0666 to all USB devices with vendor ID `0xCAFE`. This unregistered vendor ID is used by the test devices.

Without the *udev* rule, it is still possible to enumerate and query all USB devices.


### Windows

The Windows driver model is rather rigid. It's not possible to open a USB device unless it uses the *WinUSB* driver. This even applies to devices with no installed driver. Enumerating and querying USB devices is possible independent of the driver.

USB devices can implement special control requests to instruct Windows to automatically install the WinUSB driver (search for *WCID* or *Microsoft OS Compatibility Descriptors*). The WinUSB driver can also be manually installed or replaced using a software called [Zadig](https://zadig.akeo.ie/).

The test devices implement the required control requests. So the driver is installed automatically.

The implementation runs on both Windows for Intel/AMD and ARM processors. Few OpenJDK distributions support Windows on ARM, and they do not support all recent versions of Java. [Eclipse Temurin](https://adoptium.net/temurin/releases/?os=windows&arch=aarch64&version=23) has released a version for Java 23.



## Troubleshooting

### 32-bit versions

The *Foreign Function And Memory API* has not been implemented for 32-bit operating systems / JDKs (and likely never will be).



## Running on older JDK versions

The *Foreign Function And Memory API* has been available as a preview feature in JDKs before 22. However, incompatible changes were made from preview to preview to release. Earlier versions can be used with specific versions of this library:

| Version | Main New Features | Compatibility |
| - | - | - |
| 1.x.x | Release for final Java API | JDK 22 and higher |
| 0.7.x | New setter/getter names for improved Kotlin support; Kotlin examples | JDK 21 |
| 0.6.x | Support for JDK 21; better handling of composite devices on Windows | JDK 21 |
| 0.5.x | Support for JDK 20; high-throuput I/O streams | JDK 20 |
| 0.4.x | Early release | JDK 19 |

When using an older JDK, preview features must be enabled using the `--enable-preview` VM option.



## Building from source

To build from source, run the following command:

```
cd java-does-usb
mvn clean install -DskipTests
```

The tests are skipped as they require that a special test device is connected to the computer. See the next section for more information.



## Testing

In order to run the unit tests, a special test device must be connected to the computer, which can be easily created from very inexpensive microcontroller boards. Two variants exist:

- [loopback-stm32](test-devices/loopback-stm32)
- [composite-stm32](test-devices/composite-stm32)

The test device with the *loopback-stm32* code supports all tests. If the test device with the *composite-stm32* code is connected, some tests are skipped. However, if it is used, the correct handling of composite devices is verified.

Tests can be run from the command line:

```
cd java-does-usb
mvn clean test
```

If they are run from an IDE (such as IntelliJ IDEA), you must likely configure VM options to allow native access:

```
--enable-native-access=net.codecrete.usb
```

Or (if modules are ignored):

```
--enable-native-access=ALL-UNNAMED
```



## Code generation

Many bindings for the native APIs have been generated with *jextract*. See the [jextract](java-does-usb/jextract) subdirectory for more information. For functions that need to retain the error state (`errno` on Linux, `GetLastError()` on Windows), the bindings have been manually written as *jextract* does not support it.

Since the code can only be generated for the current operating system, it must be generated on separate computers for Linux, Windows and macOS. Thus, the generated code is included in the repository. The generated code is compilable on all operating systems.
