# USB Device Monitoring

This sample program monitors USB devices as they are connected and disconnected.

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

### Run the sample

```shell
$ cd JavaDoesUSB/examples/monitor
$ mvn compile exec:exec
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< net.codecrete.usb.examples:monitor >-----------------
[INFO] Building monitor 0.7.1
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:3.0.2:resources (default-resources) @ monitor ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/me/Documents/JavaDoesUSB/examples/monitor/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.0:compile (default-compile) @ monitor ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 1 source file to /Users/me/Documents/JavaDoesUSB/examples/monitor/target/classes
[INFO] 
[INFO] --- exec-maven-plugin:3.1.0:exec (default-cli) @ monitor ---
Present:      VID: 0xcafe, PID: 0xceaf, manufacturer: JavaDoesUSB, product: Loopback, serial: 8D8F515C5456, ID: 4295291950
Present:      VID: 0x1a40, PID: 0x0801, manufacturer: null, product: USB 2.0 Hub, serial: null, ID: 4295291734
Monitoring... Press ENTER to quit.
```
