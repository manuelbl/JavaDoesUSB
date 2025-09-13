# Bulk Transfer

This sample shows how to find a device, open it and transfer data from and to bulk endpoints.

## Prerequisites

- Java 22
- Apache Maven
- 64-bit operating system (Windows, macOS, Linux)
- A USB device with bulk IN and OUT endpoints (e.g. the test device, see https://github.com/manuelbl/JavaDoesUSB/tree/main/test-devices/loopback-stm32)

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
$ cd JavaDoesUSB/examples/bulk_transfer
$ mvn compile exec:exec
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------< net.codecrete.usb.examples:bulk-transfer >--------------
[INFO] Building bulk-transfer 1.2.1
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:3.3.1:resources (default-resources) @ bulk-transfer ---
[INFO] skip non existing resourceDirectory /home/user/Documents/JavaDoesUSB/examples/bulk_transfer/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.12.1:compile (default-compile) @ bulk-transfer ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] 
[INFO] --- exec-maven-plugin:3.1.1:exec (default-cli) @ bulk-transfer ---
6 bytes sent.
6 bytes received.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.228 s
[INFO] Finished at: 2024-10-13T16:23:29+01:00
[INFO] ------------------------------------------------------------------------
```
