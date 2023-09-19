# E-Paper Display

This sample shows how to communicate with an IT8951 controller for e-paper displays, e.g. [Waveshare E-Ink display HAT for Raspberry Pi](https://www.waveshare.com/9.7inch-e-paper-hat.htm)

## Prerequisites

- Java 21
- Apache Maven
- 64-bit operating system (macOS, Linux, Windows)
- IT8951 controller

On Windows, the display controller's driver must be replaced with the *WinUSB* driver
using [Zadig](https://zadig.akeo.ie/).

On macOS, root privileges are required to successfully run the sample. (The standard driver will
be temporarily detached.)

## How to run

### Install Java 21

Check that *Java 21* is installed:

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
$ cd JavaDoesUSB/examples/epaper_display
$ mvn compile exec:exec
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------< net.codecrete.usb.examples:epaper-display >--------------
[INFO] Building epaper-display 0.5.1
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ epaper-display ---
[INFO] skip non existing resourceDirectory /Users/me/Documents/JavaDoesUSB/examples/epaper_display/src/main/resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ epaper-display ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 2 source files with javac [debug release 20] to target/classes
[INFO] 
[INFO] --- exec:3.1.0:exec (default-cli) @ epaper-display ---
Display size: 1200 x 825
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.502 s
[INFO] Finished at: 2023-07-02T14:08:50+02:00
[INFO] ------------------------------------------------------------------------
```

### Run on macOS

In order to run the sample with root privileges, the best approach is to build it first without
root privileges and then run it as root without Maven:

```shell
$ cd JavaDoesUSB/examples/epaper_display
$ mvn compile
[INFO] Scanning for projects...
...
[INFO] ------------------------------------------------------------------------
$ sudo -i
Password:
$ cd /Users/me/Documents/JavaDoesUSB/examples/epaper_display
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-20.jdk/Contents/Home
$ $JAVA_HOME/bin/java --enable-preview --enable-native-access=ALL-UNNAMED -cp target/classes:/Users/me/.m2/repository/net/codecrete/usb/java-does-usb/0.5.1/java-does-usb-0.5.1.jar net.codecrete.usb.examples.EPaperDisplay
Display size: 1200 x 825
```
