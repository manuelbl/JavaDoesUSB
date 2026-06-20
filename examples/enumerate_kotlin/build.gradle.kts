plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "net.codecrete.usb.examples"
version = "1.2.1"

val javaDoesUsbVersion = "1.2.1"
val tinyLogVersion = "2.7.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.codecrete.usb:java-does-usb:$javaDoesUsbVersion")
    implementation("org.tinylog:tinylog-api:$tinyLogVersion")
    implementation("org.tinylog:tinylog-impl:$tinyLogVersion")
    implementation("org.tinylog:jsl-tinylog:$tinyLogVersion")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "net.codecrete.usb.examples.EnumerateKt"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
}