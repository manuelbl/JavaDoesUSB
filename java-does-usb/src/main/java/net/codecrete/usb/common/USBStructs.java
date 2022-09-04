package net.codecrete.usb.common;

import java.lang.foreign.GroupLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Memory layout of USB data structures.
 */
public class USBStructs {
    // typedef struct {
    //  uint8_t  RequestType;
    //  uint8_t  Request;
    //  uint8_t Value;
    //  uint16_t Index;
    //  uint16_t Length;
    //} __attribute__((packed));
    public static final GroupLayout SetupPacket$Struct = structLayout(JAVA_BYTE.withName("bmRequest"),
            JAVA_BYTE.withName("bRequest"), JAVA_SHORT.withName("wValue"), JAVA_SHORT.withName("wIndex"),
            JAVA_SHORT.withName("wLength"));

    public static final VarHandle SetupPacket_bmRequest = SetupPacket$Struct.varHandle(groupElement("bmRequest"));
    public static final VarHandle SetupPacket_bRequest = SetupPacket$Struct.varHandle(groupElement("bRequest"));
    public static final VarHandle SetupPacket_wValue = SetupPacket$Struct.varHandle(groupElement("wValue"));
    public static final VarHandle SetupPacket_wIndex = SetupPacket$Struct.varHandle(groupElement("wIndex"));
    public static final VarHandle SetupPacket_wLength = SetupPacket$Struct.varHandle(groupElement("wLength"));

    static {
        assert SetupPacket$Struct.byteSize() == 8;
    }
}
