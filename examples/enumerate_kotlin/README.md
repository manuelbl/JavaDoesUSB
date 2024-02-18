# USB Device Enumeration (Kotlin)

This sample enumerates the connected USB devices and provides information about the interfaces and endpoints.

## Prerequisites

- Java 22
- Apache Maven
- 64-bit operating system (Windows, macOS, Linux)

## How to run

### Install Java 22

Check that *Java 22* is installed:

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
$ cd JavaDoesUSB/examples/enumerate_kotlin
$ mvn clean package
```

### Run the jar

```shell
$ java --enable-native-access=ALL-UNNAMED -jar target/enumerate-1.0.0-SNAPSHOT-jar-with-dependencies.jar
Device:
  VID: 0xcafe
  PID: 0xceaf
  Manufacturer:  JavaDoesUSB
  Product name:  Loopback
...
```
