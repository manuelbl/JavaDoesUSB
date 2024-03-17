# Device Firmware Upload (DFU) for STM32

This sample programs implements firmware upload for STM32 microcontrollers with the built-in DFU mode.

Even though the DFU

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

### Build the application

```shell
$ mvn clean package
```

### Put the STM32 development board in DFU mode

- Connect an STM32 development board (like a BlackPill board) to your computer while pressing the *Boot* button.
- Ensure that it is DFU mode by checking macOS *System Information* or Windows *Device Manager*. The device should appear as "STM32 BOOTLOADER".

On Windows, the *WinUSB* driver must be installed. See https://github.com/manuelbl/JavaDoesUSB/wiki/DFU-on-Windows for additional information.

On many Linux distributions, the default permissions of USB devices do not allow access. To change it, create a file `/etc/udev/rules.d/50-stm-dfu.rules` with the below content:

```text
SUBSYSTEM=="usb", ATTRS{idVendor}=="0483", ATTRS{idProduct}=="df11", MODE="0666"
```


### Run the application

Run the command below (adapting the file path depending on your specific board):

```shell
$ mvn package
$ java --enable-native-access=ALL-UNNAMED -jar target/stm_dfu-1.0.0-SNAPSHOT.jar ../../test-devices/loopback-stm32/bin/blackpill-f401cc.bin
DFU device found with serial 35A737883336.
Target memory segment: Internal Flash
Erasing page at 0x8000000 (size 0x4000)
Writing data at 0x8000000 (size 0x800)
Writing data at 0x8000800 (size 0x800)
Writing data at 0x8001000 (size 0x800)
Writing data at 0x8001800 (size 0x800)
Writing data at 0x8002000 (size 0x800)
Writing data at 0x8002800 (size 0x1f4)
Firmware successfully downloaded and verified
DFU mode exited and firmware started
```
