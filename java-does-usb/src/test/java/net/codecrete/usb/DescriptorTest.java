package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptorTest extends TestDeviceBase {

    @Test
    void deviceDescriptor_isAvailable() {
        var desc = testDevice.deviceDescriptor();
        assertEquals(18, desc.length);
        assertEquals(0x01, desc[1]);
    }

    @Test
    void configurationDescriptor_isAvailable() {
        var desc = testDevice.configurationDescriptor();
        assertTrue(desc.length > 60);
        assertEquals(0x02, desc[1]);
    }
}
