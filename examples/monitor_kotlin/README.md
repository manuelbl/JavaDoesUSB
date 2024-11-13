# USB Device Monitoring (Kotlin)

This sample enumerates the connected USB devices and provides information about the interfaces and endpoints.

## Prerequisites

- Java 22
- Apache Maven
- 64-bit operating system (Windows, macOS, Linux)

## How to run

### Install Java 22 or higher

Check that Java 22 or higher is installed:

```shell
$ java -version
```

If not, download and install it, e.g. from [Azul](https://www.azul.com/downloads/?package=jdk).

### Install Maven

Check that *Maven* is installed:

```shell
$ mvn -version
```

If it is not present, install it, typically using package manager like *Homebrew* on macOS, *Chocolately* on Windows and *apt* on Linux.

### Build a self-contained jar file

```shell
$ cd JavaDoesUSB/examples/monitor_kotlin
$ mvn clean package
```

### Run the jar

```shell
$ java --enable-native-access=ALL-UNNAMED -jar target/monitor-1.1.1-jar-with-dependencies.jar
Present:      VID: 0x1d6b, PID: 0x0002, manufacturer: Linux 6.5.0-18-generic xhci-hcd, product: xHCI Host Controller, serial: 0000:00:14.0, ID: /dev/bus/usb/001/001
Present:      VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: /dev/bus/usb/001/009
Monitoring... Press ENTER to quit.
Disconnected: VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: /dev/bus/usb/001/009
Connected:    VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: /dev/bus/usb/001/010
```
