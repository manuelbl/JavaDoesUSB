# USB Device Enumeration

This sample enumerates the connected USB devices and provides information about the interfaces and endpoints.

## Prerequisites

- Java 19
- Apache Maven
- 64-bit operating system (Windows, macOS, Linux)

## How to run

### Install Java 19

Check that *Java 19* is installed:

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
[INFO] Building enumerate 0.4.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:3.0.2:resources (default-resources) @ enumerate ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/me/Documents/JavaDoesUSB/examples/enumerate/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.0:compile (default-compile) @ enumerate ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- exec-maven-plugin:3.1.0:exec (default-cli) @ enumerate ---
Device:
  VID: 0xcafe
  PID: 0xceaf
  Manufacturer:  JavaDoesUSB
  Product name:  Loopback
...
```
