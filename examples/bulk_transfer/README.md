# Bulk Transfer

This sample shows how to find a device, open it and transfer data from and to bulk endpoints.

## Prerequisites

- Java 19
- Apache Maven
- 64-bit operating system (Windows, macOS, Linux)
- A USB device with bulk IN and OUT endpoints (e.g. the test device, see https://github.com/manuelbl/JavaDoesUSB/tree/main/test-devices/loopback-stm32)

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

### Create the *java-does-usb* library

Since the *java-does-usb* library is not yet available on Maven Central, it must be built locally:

```shell
$ cd JavaDoesUSB/java-does-usb
$ mvn install
```

The result will be put in your local Maven repository.

### Run the sample

```shell
$ cd JavaDoesUSB/examples/bulk_transfer
$ mvn compile exec:exec
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------< net.codecrete.usb.examples:bulk-transfer >--------------
[INFO] Building bulk-transfer 0.3-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:3.0.2:resources (default-resources) @ bulk-transfer ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/manuel/Documents/Lab/JavaDoesUSB/examples/bulk_transfer/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.0:compile (default-compile) @ bulk-transfer ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 1 source file to /Users/manuel/Documents/Lab/JavaDoesUSB/examples/bulk_transfer/target/classes
[INFO] 
[INFO] --- exec-maven-plugin:3.1.0:exec (default-cli) @ bulk-transfer ---
6 bytes sent.
6 bytes received.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.830 s
[INFO] Finished at: 2022-09-09T14:43:10+02:00
[INFO] ------------------------------------------------------------------------
```
