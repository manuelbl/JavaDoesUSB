package net.codecrete.usb.usbstandard;

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * USB setup packet.
 */
@SuppressWarnings({"java:S115", "java:S125"})
public class SetupPacket {

    private final MemorySegment descriptor;

    /**
     * Creates a setup packet accessing the specified memory segment.
     *
     * @param descriptor memory segment
     */
    public SetupPacket(MemorySegment descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Creates a setup packet by allocating a native memory segment.
     *
     * @param arena arena
     */
    public SetupPacket(Arena arena) {
        this.descriptor = arena.allocate(LAYOUT);
    }

    /**
     * Gets the memory segment of this setup packet.
     *
     * @return memory segment
     */
    public MemorySegment segment() {
        return descriptor;
    }

    public int requestType() {
        return 0xff & descriptor.get(JAVA_BYTE, bmRequestType$OFFSET);
    }

    public void setRequestType(int requestType) {
        descriptor.set(JAVA_BYTE, bmRequestType$OFFSET, (byte) requestType);
    }

    public int request() {
        return 0xff & descriptor.get(JAVA_BYTE, bRequest$OFFSET);
    }

    public void setRequest(int request) {
        descriptor.set(JAVA_BYTE, bRequest$OFFSET, (byte) request);
    }

    public int value() {
        return 0xffff & descriptor.get(JAVA_SHORT, wValue$OFFSET);
    }

    public void setValue(int value) {
        descriptor.set(JAVA_SHORT, wValue$OFFSET, (short) value);
    }

    public int index() {
        return 0xffff & descriptor.get(JAVA_SHORT, wIndex$OFFSET);
    }

    public void setIndex(int index) {
        descriptor.set(JAVA_SHORT, wIndex$OFFSET, (short) index);
    }

    public int length() {
        return 0xffff & descriptor.get(JAVA_SHORT, wLength$OFFSET);
    }

    public void setLength(int length) {
        descriptor.set(JAVA_SHORT, wLength$OFFSET, (short) length);
    }

    // struct USBSetupPacket {
    //     uint8_t  bmRequestType;
    //     uint8_t  bRequest;
    //     uint8_t  wValue;
    //     uint16_t wIndex;
    //     uint16_t wLength;
    // } __attribute__((packed));
    public static final GroupLayout LAYOUT = structLayout(
            JAVA_BYTE.withName("bmRequestType"),
            JAVA_BYTE.withName("bRequest"),
            JAVA_SHORT.withName("wValue"),
            JAVA_SHORT.withName("wIndex"),
            JAVA_SHORT.withName("wLength")
    );

    private static final long bmRequestType$OFFSET = 0;
    private static final long bRequest$OFFSET = 1;
    private static final long wValue$OFFSET = 2;
    private static final long wIndex$OFFSET = 4;
    private static final long wLength$OFFSET = 6;

    static {
        assert LAYOUT.byteSize() == 8;
    }
}
