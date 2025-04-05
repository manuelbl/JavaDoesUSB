//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.usbstandard;

import net.codecrete.usb.UsbException;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * USB string descriptor
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class StringDescriptor {

    private final MemorySegment descriptor;

    public StringDescriptor(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Indicates if this string descriptor is valid.
     * <p>
     * Invalid string descriptors might be missing the header,
     * have a descriptor type that is not a string descriptor,
     * indicate an incorrect length or have incomplete UTF-16 code units.
     * </p>
     * @return if this descriptor is valid
     */
    public boolean isValid() {
        return descriptor.byteSize() >= 2
                && descriptor.get(JAVA_BYTE, bDescriptorType$OFFSET) == 3
                && length() == descriptor.byteSize()
                && (descriptor.byteSize() & 1) == 0;
    }

    public int length() {
        return 0xff & descriptor.get(JAVA_BYTE, bLength$OFFSET);
    }

    /**
     * Returns the string of this string descriptor.
     * <p>
     * Invalid UTF-16 code units are replaced with the Unicode replacement character.
     * Trailing 0s (UTF-16 code unit with value 0) are truncated.
     * </p>
     * @throws UsbException if the string descriptor is invalid
     * @return the string value
     */
    public String string() {
        if (!isValid())
            throw new UsbException("String descriptor is invalid");
        var len = (int) (length() - 2L);
        var bytes = descriptor.asSlice(string$OFFSET, len).toArray(JAVA_BYTE);

        // truncate trailing 0s
        while (len > 0 && bytes[len - 2] == 0 && bytes[len - 1] == 0)
            len--;

        return new String(bytes, 0, len, StandardCharsets.UTF_16LE);
    }

    // struct USBStringDescriptor {
    //     uint8_t   bLength;
    //     uint8_t   bDescriptorType;
    //     uint16_t  string[1];
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_SHORT.withName("string")
    );

    private static final long bLength$OFFSET = 0;
    private static final long bDescriptorType$OFFSET = 1;
    private static final long string$OFFSET = 2;
}
