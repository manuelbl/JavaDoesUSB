# Loopback Test Device

For testing the *Java Does USB* library, a dedicated USB test device is needed. This is the code for an STM32F103C8 microcontroller. This microcontroller is found on many inexpensive development board, most notabily on the so called *Blue Pill*. Such boards are available for about 3 USD, including the required ST-Link programmer.


## Test features

### Loopback

All data sent to endpoint 0x01 (OUT) is sent back on endpoint 0x82 (IN). Both endpoints use bulk transfer with a maximum packet size of 64 bytes. The device uses an internal buffer of about 500 bytes. Up to this amount, the data can be first sent and then received.

### Control requests

Several vendor-specific control requests are supported for testing:

| `bmRequest` | `bRequest` | `wValue` | `wIndex` | `wLength` | Data | Action |
| - | - | - | - | - | - | - |
| 0x41 | 0x01 | *value* | 0 | 0 | none | Host to device: *value* is saved in device |
| 0x41 | 0x02 | 0 | 0 | 4 | *value* (32-bit LE) | Host to device: *value* is saved in device |
| 0xC1 | 0x03 | 0 | 0 | 4 | *value* (32-bit LE) | Device to host: saved *value* is transmitted |


## Building

This project requires [PlatformIO](https://platformio.org/). The easiest way to get up and running is to use Visual Studio Code and then install the [PlatformIO IDE extension](https://marketplace.visualstudio.com/items?itemName=platformio.platformio-ide).

After the extension is installed, open this folder and then click checkbox icon (*Build* action) in the status bar.

To upload the code to the microcontroller:

- Connect the ST-Link programmer to the development board
- Connect the ST-Link programmer to your computer
- Click the arrow icon (*Upload* action) in the status bar


## Binary release

The directory `bin` contains a pre-built firmware. In order to upload it, a utility is needed, either [STM32CubeProgrammer](https://www.st.com/en/development-tools/stm32cubeprog.html) (requires an STM account, does not properly work on macOS) or the [open-source ST-Link command line utility](https://github.com/stlink-org/stlink). See the respective web site for installation instructions.

If the commmand line utility is used, run these commands to upload it:

```
cd stm32-loopback/bin
st-flash write firmware.bin 0x08000000
```
