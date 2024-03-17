# Composite Test Device

Firmware for a composite device consisting of virtual serial port (aka CDC ACM) on interface 0 and 1 and a vendor-specific class on interface 2. This is the code for STM32 microcontrollers. It is found on many inexpensive development board, most notabily on the so called *Blue Pill* and *Black Pill* boards. They are available for about 3 USD.

## Supported boards

- BlackPill with STM32F401CC microcontroller
- BlackPill with STM32F411CE microcontroller
- BluePill with STM32F103C8 microcontroller

To upload the firmware, the STM32F4x microcontroller have a built-in USB bootloader. The STM32F1x microcontrollers need an ST-Link debug adapter (or a USB-to-serial converter).


## Test features

### Endpoints

| Endpoint | Transfer Type | Direction | Packet Size | Interface | Function |
| - | - | - | - | - | - |
| 0x00 | Control | Bidirectional |  | 2 | See *Control requests* below |
| 0x81 | Bulk | Device to host | 64 bytes | 1 | CDC: Serial data from device to host. |
| 0x02 | Bulk | Host to device | 64 bytes | 1 | CDC: Serial data from host to device. |
| 0x83 | Interrupt | Device to host | 64 bytes | 0 | CDC: Serial state events (not used). |
| 0x01 | Bulk | Host to device | 64 bytes | 3 | Loopback: all data received on this endpoint is transmitted on endpoint 0x82. |
| 0x82 | Bulk | Device to host | 64 bytes | 3 |  Loopback: Transmits the data received on endpoint 0x01. |

The virtual serial port on interfaces 0 and 1 implements the CDC ACM class. All operating systems will recognize it as serial port and will automatically make it available as such. No drivers need to be installed. The implementations connects the incoming and outgoing data in a loopback configuration. So all data sent from the host to the device is send back to the host. Control requests to configure baud rates, parity etc. are accepted but have no effect. And the implementation does not send any state events.

The bulk endpoints 0x01 and 0x82 use an internal buffer of about 500 bytes. Data up to this amount can be sent and received sequentially. If more data is sent without receiving at the same time, flow control kicks in and endpoint 0x01 will stop receiving data until there is room in the buffer.


### Control requests

Several vendor-specific control requests are supported for testing:

| `bmRequest` | `bRequest` | `wValue` | `wIndex` | `wLength` | Data | Action |
| - | - | - | - | - | - | - |
| 0x41 | 0x01 | *value* | 0 | 0 | none | Host to device: *value* is saved in device |
| 0x41 | 0x02 | 0 | 0 | 4 | *value* (32-bit LE) | Host to device: *value* is saved in device |
| 0xC1 | 0x03 | 0 | 0 | 4 | *value* (32-bit LE) | Device to host: saved *value* is transmitted |
| 0xC1 | 0x05 | 0 | 0 | 1 | *interface number* | Device to host: interface number is transmitted |



## Building the firmware

This project requires [PlatformIO](https://platformio.org/). The easiest way to get up and running is to use Visual Studio Code and then install the [PlatformIO IDE extension](https://marketplace.visualstudio.com/items?itemName=platformio.platformio-ide).

After the extension is installed, open this folder and select your board type by clicking on "Default (tinyusb-stm32)" in the status bar. Wait until the status bar no longer indicates activity. Then click the checkbox icon (*Build* action) in the status bar.

To load the firmware onto the board, either connect it via ST-Link programmer to the 4 pins on the short side of the board or use the built-in USB bootloader (BlackPill only).

For upload with a ST-Link programmer:

- Connect the ST-Link programmer to the development board
- Connect the ST-Link programmer to your computer
- Click the arrow icon (*Upload* action) in the status bar

For other means of upload, see *Binary releases* below.


## Binary releases

The directory `bin` contains a pre-built firmware:

- `blackpill-f401cc.bin`: Firmware for BlackPill with STM32F401CC microcontroller
- `blackpill-f411ce.bin`: Firmware for BlackPill with STM32F411CE microcontroller
- `bluepill-f103c8.bin`: Firmware for BluePill with STM32F103C8 microcontroller

### Upload using built-in bootloader

To upload using the BlackPill's built-in bootloader:

1. Install the *dfu-util* command-line utility (typically using a package manager like *HomeBrew* on macOS, *Chocolatey* on Windows, or *Apt* on Linux).
2. Press the *Boot* button while connecting the board via USB to your computer. By pressing the *Boot* button, the device enters bootloader mode.
3. Verify with `dfu-util --list` that the bootloader is available via USB. If not unplug the device and repeat step 2.
4. Run the below command from the project directory.
5. Unplug and reconnect the board from your computer. Both the power and user LED should be lit and the device should appear as a serial device (aka as COM port on Windows).

```
dfu-util --device 0483:df11 --alt 0 --dfuse-address 0x08000000 --reset --download bin/blackpill-fxxx.bin
```

Make sure you change the filename `blackpill-fxxx.bin` to the name matching your board.

If you built the firmware yourself, you will find the firmware file in `.pio/build/blackpill-f401cc/firmware.bin` (and similar for other boards).

### Upload using ST-Link programmer

In order to upload it using the ST-Link programmer:

1. Install the *stlink* command-line utility (typically using a package manager like *HomeBrew* on macOS, *Chocolatey* on Windows, or *Apt* on Linux).
2. Unplug the microcontroller board from your computer (in case it is connected).
3. Connect the ST-Link via jumper wires to your board (4 pins on the short side of the board).
4. Connect the ST-Link via USB cable to your computer.
5. Run the below command from the project directory.
6. Unplug the microcontroller board from the ST-Link and connect it to your computer (via USB).

```
st-flash write bin/bluepill-f103c8.bin 0x08000000
```

Make sure you change the filename `bluepill-f103c8.bin` to the name matching your board.

If you built the firmware yourself, you will find the firmware file in `.pio/build/bluepill-f103c8/firmware.bin` (and similar for other boards).

## Implementation

This code uses the CMSIS 5 library (mainly for startup code and register definitions) and TinyUSB for USB. For easier use with PlatformIO, a copy of TinyUSB is integrated into the project. The used TinyUSB code in `lib/tinyusb` is an unmodified subset of the library.

Since the official TinyUSB vendor class is rather limited, an alternative implementation is provided (see [vendor_custom.h](src/vendor_custom.h) and [vendor_custom.c](src/vendor_custom.c)).