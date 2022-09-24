package net.codecrete.usb.usbstandard;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * USB setup packet.
 */
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
     * @param session memory session
     */
    public SetupPacket(MemorySession session) {
        this.descriptor = session.allocate(LAYOUT);
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
        return 0xff & (byte) bmRequestType$VH.get(descriptor);
    }

    public void setRequestType(int requestType) {
        bmRequestType$VH.set(descriptor, (byte) requestType);
    }

    public int request() {
        return 0xff & (byte) bRequest$VH.get(descriptor);
    }

    public void setRequest(int request) {
        bRequest$VH.set(descriptor, (byte) request);
    }

    public int value() {
        return 0xffff & (short) wValue$VH.get(descriptor);
    }

    public void setValue(int value) {
        wValue$VH.set(descriptor, (short) value);
    }

    public int index() {
        return 0xffff & (short) wIndex$VH.get(descriptor);
    }

    public void setIndex(int index) {
        wIndex$VH.set(descriptor, (short) index);
    }

    public int length() {
        return 0xffff & (short) wLength$VH.get(descriptor);
    }

    public void setLength(int length) {
        wLength$VH.set(descriptor, (short) length);
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

    private static final VarHandle bmRequestType$VH = LAYOUT.varHandle(groupElement("bmRequestType"));
    private static final VarHandle bRequest$VH = LAYOUT.varHandle(groupElement("bRequest"));
    private static final VarHandle wValue$VH = LAYOUT.varHandle(groupElement("wValue"));
    private static final VarHandle wIndex$VH = LAYOUT.varHandle(groupElement("wIndex"));
    private static final VarHandle wLength$VH = LAYOUT.varHandle(groupElement("wLength"));

    static {
        assert LAYOUT.byteSize() == 8;
    }
}
