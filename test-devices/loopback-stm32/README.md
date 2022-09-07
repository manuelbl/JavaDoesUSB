# Loopback Test Device

For testing the *Java Does USB* library, a dedicated USB test device is needed. This is the code for an STM32F103C8 microcontroller. This microcontroller is found on many inexpensive development board, most notabily on the so called *Blue Pill*. Such boards are available for about 3 USD, including the required ST-Link programmer.


## Test features

### Endpoints

| Endpoint | Transfer Type | Direction | Packet Size | Function |
| - | - | - | - | - |
| 0x00 | Control | Bidirectional |  | See *Control requests* below |
| 0x01 | Bulk | Host to device | 64 bytes | Loopback: all data received on this endpoint are then transmitted on endpoint 0x82. |
| 0x82 | Bulk | Device to host | 64 bytes |  Loopback: Transmits the data received on endpoint 0x01. |
| 0x03 | Interrupt | Host to device | 16 bytes |  Echo: All packets received on this endpoint are transmitted twice on endpoint 0x83. |
| 0x83 | Interrupt | Device to host | 16 bytes |  Echo: Transmits all packets received on endpoint 0x03 twice. |

The bulk endpoints 0x01 and 0x82 use an internal buffer of about 1000 bytes. Data up to this amount can be sent and received sequentially. If more data is sent without receiving at the same time, flow control kicks in and endpoint 0x01 will stop receiving data until there is room in the buffer.


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
