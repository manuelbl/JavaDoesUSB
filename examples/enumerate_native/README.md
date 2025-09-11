# Device Enumeration (Native)

This example project demonstrates how th build a native application that
enumerates connected devices using [GraalVM](https://www.graalvm.org/) and
this _Java Does USB_ library.

## Build and Run

### Prerequisites

- [GraalVM](https://www.graalvm.org/) 25 or higher
- [Maven](https://maven.apache.org/) 3.9 or higher
- Latest snapshot of _Java Does USB_


#### Java Does USB Snapshot

Check out the latest version of _Java Does USB_ and build it:

```shell
git clone https://github.com/manuelbl/JavaDoesUSB.git
cd JavaDoesUSB/java-does-usb
mvn clean install -DskipTests
```

The installation requires a valid GPG key pair. If you don't have one,
create it with and do not set a password:

```shell
gpg --gen-key
```

The full name and email address can be anyone's.


### Preparation

GraalVM needs help to learn about the Java FFM downcall and upcall descriptors,
and it needs some help to include all required methods. The relevant items
differ from operating system to operating system. When building the native image,
the operating system must be selected by changing the path in the file
`native-image.properties` in the directory
`src/main/resources/META-INF/native-image/net.codecrete.usb.examples/enumerate_native`.

```properties
Args = --enable-native-access=ALL-UNNAMED -H:ConfigurationFileDirectories=config/macos
```

Note the last word of the line. In this case, it is `macos`. Change this to
`linux` or `windows` if needed. (Windows is yet to come.)

In your own Maven project, you might also need to move the file or rather rename
directory. It must be named according to the pattern
`src/main/resources/META-INF/native-image/<groupId>/<artifactId>`.


### Building

```shell
mvn -Dnative package
```


### Running

```shell
./target/enumerate-native
```
