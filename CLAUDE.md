# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

*Java Does USB* is a pure-Java USB library for communicating with USB devices using custom/vendor-specific protocols (not standard device classes like mass storage or HID). It accesses the OS's native USB APIs through the **Foreign Function and Memory API** — no JNI, no native third-party libraries. Requires **JDK 25** (the `pom.xml` targets release 25; older JDKs need older library versions, see README).

The publishable library lives in `java-does-usb/`. The repo root also holds `examples/`, `test-devices/` (microcontroller firmware for the hardware test rig), `test-graalvm/` (GraalVM native-image compatibility check), and `reference/`.

## Commands

All library commands run from the `java-does-usb/` directory using the Maven wrapper (`./mvnw`).

```bash
cd java-does-usb

# Build & install to local Maven repo, skipping hardware tests
./mvnw clean install -DskipTests

# Run all tests (REQUIRES a physical test device connected — see below)
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=BulkTransferTest

# Run a single test method
./mvnw test -Dtest=BulkTransferTest#transferData

# Build javadoc
./mvnw javadoc:javadoc
```

Examples are independent projects, each with its own `mvnw`/`gradlew`. Build one with `cd examples/<name> && ./mvnw clean compile` (or `./gradlew build` for Kotlin examples).

### Native access flag

Any code calling into the library needs native access enabled. Maven surefire passes `--enable-native-access=ALL-UNNAMED` automatically. When running from an IDE or standalone, add the VM option:
`--enable-native-access=net.codecrete.usb` (or `ALL-UNNAMED` if modules are ignored).

## Testing requires hardware

The unit tests are integration tests against a real USB device — they will fail with "No test device connected" without one. The device is built from an inexpensive STM32 board flashed with firmware from `test-devices/`:

- **loopback-stm32** (VID `0xcafe`, PID `0xceaf`): supports all tests.
- **composite-stm32** (VID `0xcafe`, PID `0xcea0`): exercises composite-device handling; some tests are skipped.

`TestDeviceConfig.java` hard-codes the VID/PID and endpoint numbers for both variants; `TestDeviceBase` auto-detects which one is connected. On Linux, a udev rule is needed for device access (see README "Linux" section). On Windows the test device auto-installs the WinUSB driver via WCID descriptors.

The `continuous-integration.yaml` workflow only compiles the library and examples on all three OSes — it does **not** run the hardware tests.

## Architecture

### Cross-platform abstraction

The public API is in package `net.codecrete.usb` (`Usb`, `UsbDevice`, `UsbInterface`, `UsbEndpoint`, exceptions, enums). It is the only exported package (`module-info.java`).

`Usb.java` is the entry point. `Usb.instance()` lazily picks a platform implementation of `UsbDeviceRegistry` based on `os.name`/`os.arch` (Macos/Windows/Linux). Everything else flows through platform-specific subclasses of the abstractions in `net.codecrete.usb.common`:

- `UsbDeviceRegistry` (common) — singleton that runs a background daemon thread enumerating devices and emitting connect/disconnect events. Each OS subclass implements `monitorDevices()` and calls `setInitialDeviceList()` when the first enumeration completes.
- `UsbDeviceImpl`, `UsbInterfaceImpl`, `UsbEndpointImpl`, `UsbAlternateInterfaceImpl` (common) — shared state and logic; each OS provides a `*UsbDevice` subclass that implements the actual native transfers.
- `EndpointInputStream`/`EndpointOutputStream` + platform `*EndpointInputStream`/`*EndpointOutputStream` — the high-throughput streaming layer.
- `Transfer`/`TransferCompletion` + platform `*Transfer` and `*AsyncTask` — asynchronous transfer plumbing (I/O completion ports on Windows, epoll on Linux, run loop on macOS).
- `ConfigurationParser` (common) — parses raw USB configuration descriptors (portable, byte-level; not OS-specific).

When adding a feature, expect to touch the common abstraction plus **all three** platform implementations (`linux/`, `macos/`, `windows/`).

### Native bindings (`gen` packages)

Native API bindings live under `*/gen/` subpackages and are **generated code**, one package per shared library / macOS framework:

- **Linux & macOS**: generated with [jextract](https://jdk.java.net/jextract/) via scripts in `java-does-usb/jextract/{linux,macos}/`. The generated code **is committed** to the repo (must be regenerated per-OS; it is portable across x64/ARM64 of the same OS).
- **Windows**: generated at build time by the `windowsapi-maven-plugin` (Windows API Generator) — the function/struct/constant list is configured in `pom.xml`, and the output is **not committed**.

Some bindings are hand-written rather than generated, because jextract cannot capture thread-local error state (`errno` on Linux, `GetLastError()` on Windows) — those need an extra call-state parameter. See `java-does-usb/jextract/README.md` for the full generation process and jextract's quirks before regenerating.

Standard USB descriptor structs (device/config/interface/endpoint/string descriptors, setup packet) are modeled in `net.codecrete.usb.usbstandard` as `MemorySegment` views — these are portable and shared across platforms.
