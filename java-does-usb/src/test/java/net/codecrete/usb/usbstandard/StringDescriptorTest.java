package net.codecrete.usb.usbstandard;

import net.codecrete.usb.UsbException;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringDescriptorTest {

    @Test
    void validStringDescriptor() {
        testDescriptor(
                new byte[]{0x0c, 0x03, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o', 0},
                stringDescriptor -> {
                    assertThat(stringDescriptor.isValid()).isTrue();
                    assertThat(stringDescriptor.string()).isEqualTo("Hello");
                }
        );
    }

    @Test
    void truncateTrailingZeros() {
        testDescriptor(
                new byte[]{0x0e, 0x03, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o', 0, 0, 0},
                stringDescriptor -> {
                    assertThat(stringDescriptor.isValid()).isTrue();
                    assertThat(stringDescriptor.string()).isEqualTo("Hello");
                }
        );
    }

    @Test
    void invalidDescriptorType() {
        testDescriptor(
                new byte[]{0x0c, 0x04, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o', 0},
                stringDescriptor -> assertThat(stringDescriptor.isValid()).isFalse()
        );
    }

    @Test
    void inconsistentLength() {
        testDescriptor(
                new byte[]{0x0c, 0x03, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o', 0, 0, 0},
                stringDescriptor -> assertThat(stringDescriptor.isValid()).isFalse()
        );
    }

    @Test
    void inconsistentLengthThrowsException() {
        testDescriptor(
                new byte[]{0x0c, 0x03, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o', 0, 0, 0},
                stringDescriptor -> {
                    assertThat(stringDescriptor.isValid()).isFalse();
                    assertThatThrownBy(stringDescriptor::string)
                            .isInstanceOf(UsbException.class)
                            .hasMessage("String descriptor is invalid");
                }
        );
    }

    @Test
    void oddLength() {
        testDescriptor(
                new byte[]{0x0d, 0x03, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o', 0, 0},
                stringDescriptor -> assertThat(stringDescriptor.isValid()).isFalse()
        );
    }

    @Test
    void unicodeSurrogates() {
        // In theory, USB is stuck with an old Unicode standard and the below is invalid.
        // In practice, it will work anyway.
        testDescriptor(
                new byte[]{0x06, 0x03, 0x3D, (byte) 0xD8, 0x1B, (byte) 0xDE},
                stringDescriptor -> {
                    assertThat(stringDescriptor.isValid()).isTrue();
                    assertThat(stringDescriptor.string()).isEqualTo("\uD83D\uDE1B");
                }
        );
    }

    @Test
    void invalidUnicodeCharactersAreReplaced() {
        testDescriptor(
                new byte[]{0x06, 0x03, 'H', 0, 0x1B, (byte) 0xDE},
                stringDescriptor -> {
                    assertThat(stringDescriptor.isValid()).isTrue();
                    assertThat(stringDescriptor.string()).isEqualTo("H�");
                }
        );
    }

    private void testDescriptor(byte[] descriptorBytes, Consumer<StringDescriptor> validator) {
        try (var arena = Arena.ofConfined()) {
            var memorySegment = arena.allocate(descriptorBytes.length);
            memorySegment.copyFrom(MemorySegment.ofArray(descriptorBytes));

            var stringDescriptor = new StringDescriptor(memorySegment);
            validator.accept(stringDescriptor);
        }
    }
}
