# USB Device Monitoring

This sample program monitors USB devices as they are connected and disconnected.

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

### Run the sample

```shell
$ cd JavaDoesUSB/examples/monitor
$ mvn compile exec:exec

[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< net.codecrete.usb.examples:monitor >-----------------
[INFO] Building monitor 1.1.2
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ monitor ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- maven-compiler-plugin:3.12.1:compile (default-compile) @ monitor ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] 
[INFO] --- exec-maven-plugin:3.1.1:exec (default-cli) @ monitor ---
Present:      VID: 0x1d6b, PID: 0x0002, manufacturer: Linux 6.5.0-18-generic xhci-hcd, product: xHCI Host Controller, serial: 0000:00:14.0, ID: /dev/bus/usb/001/001
Present:      VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: /dev/bus/usb/001/005
Monitoring... Press ENTER to quit.
Disconnected: VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: /dev/bus/usb/001/005
Connected:    VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 35A737883336, ID: /dev/bus/usb/001/009

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  17.647 s
[INFO] Finished at: 2024-10-13T16:50:59+01:00
[INFO] -----------------------------------------------------------------------
```
