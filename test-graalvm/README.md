# Application for Testing the GraalVM Configuration

## Collect Reachability Data

Reachability data can be collected by running the unit test
of the _java-does-usb_ project:

```shell
cd java-does-usb
export JAVA_TOOL_OPTIONS="-agentlib:native-image-agent=config-output-dir=metadata-{pid}-{datetime}/"
mvn test
```


## Building

```shell
mvn -Pnative package
```


## Running

```shell
./target/test_graalvm
```
