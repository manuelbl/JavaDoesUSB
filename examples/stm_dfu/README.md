# Device Firmware Upload (DFU) for STM32

This sample programs implements firmware upload for STM32 microcontrollers with the built-in DFU mode.

Even though the DFU

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

### Build the application

Since the *java-does-usb* library is not yet available on Maven Central, it must be built locally:

```shell
$ mvn clean package
```

The result will be put in your local Maven repository.

### Run the application

- Connect an STM32 development board (like a BlackPill board) to your computer while pressing the *Boot* button.
- Ensure that it is DFU mode by checking macOS *System Information* or Windows *Device Manager*. The device should appear as "STM32 BOOTLOADER".
- Run the command below (adapting the file path depending on your specific board).

```shell
$ java --enable-preview --enable-native-access=ALL-UNNAMED -jar target/stm_dfu-0.3.1.jar ../../test-devices/loopback-stm32/bin/blackpill-f401cc.bin
DFU device found with serial 36C730037136.
Target memory segment: Internal Flash
Erasing page at 0x8000000 (size 0x4000)
Writing data at 0x8000000 (size 0x800)
Writing data at 0x8000800 (size 0x800)
Writing data at 0x8001000 (size 0x800)
Writing data at 0x8001800 (size 0x800)
Writing data at 0x8002000 (size 0x800)
Writing data at 0x8002800 (size 0x4cc)
Firmware successfully downloaded and verified
DFU mode exited and firmware started
```
