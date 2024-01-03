# USB Device Monitoring (Kotlin)

This sample enumerates the connected USB devices and provides information about the interfaces and endpoints.

## Prerequisites

- Java 21
- Apache Maven
- 64-bit operating system (Windows, macOS, Linux)

## How to run

### Install Java 21

Check that *Java 21* is installed:

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
$ java --enable-preview --enable-native-access=ALL-UNNAMED -jar target/monitor-0.7.0-jar-with-dependencies.jar
WARNING: "public static final void net.codecrete.usb.examples.MonitorKt.main()" chosen over "public static void net.codecrete.usb.examples.MonitorKt.main(java.lang.String[])"
Present:      VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: 4295265643
Present:      VID: 0x1a40, PID: 0x0801, manufacturer: null, product: USB 2.0 Hub, serial: null, ID: 4295259660
Monitoring... Press ENTER to quit.
...
```
