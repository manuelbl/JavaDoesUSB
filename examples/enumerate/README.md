# USB Device Enumeration

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

### Run the sample

```shell
$ cd JavaDoesUSB/examples/enumerate
$ mvn compile exec:exec
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------< net.codecrete.usb.examples:enumerate >----------------
[INFO] Building enumerate 1.2.1
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ enumerate ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- maven-compiler-plugin:3.12.1:compile (default-compile) @ enumerate ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] 
[INFO] --- exec-maven-plugin:3.1.1:exec (default-cli) @ enumerate ---
Device:
  VID: 0xcafe
  PID: 0xceaf
  Manufacturer:  JavaDoesUSB
  Product name:  Loopback
  Serial number: 35A737883336
...
```
