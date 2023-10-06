package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DescriptorTest extends TestDeviceBase {

    @Test
    void deviceDescriptor_isAvailable() {
        var desc = testDevice.deviceDescriptor();
        assertThat(desc).hasSize(18);
        assertThat(desc[1]).isEqualTo((byte) 0x01);
    }

    @Test
    void configurationDescriptor_isAvailable() {
        var desc = testDevice.configurationDescriptor();
        assertThat(desc).hasSizeGreaterThan(60);
        assertThat(desc[1]).isEqualTo((byte) 0x02);
    }
}
