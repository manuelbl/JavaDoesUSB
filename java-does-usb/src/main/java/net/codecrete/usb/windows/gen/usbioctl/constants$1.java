// Generated by jextract

package net.codecrete.usb.windows.gen.usbioctl;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
final class constants$1 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$1() {}
    static final VarHandle const$0 = constants$0.const$2.varHandle(MemoryLayout.PathElement.groupElement("wIndex"));
    static final VarHandle const$1 = constants$0.const$2.varHandle(MemoryLayout.PathElement.groupElement("wLength"));
    static final StructLayout const$2 = MemoryLayout.structLayout(
        JAVA_INT.withByteAlignment(1).withName("ConnectionIndex"),
        MemoryLayout.structLayout(
            JAVA_BYTE.withName("bLength"),
            JAVA_BYTE.withName("bDescriptorType"),
            JAVA_SHORT.withByteAlignment(1).withName("bcdUSB"),
            JAVA_BYTE.withName("bDeviceClass"),
            JAVA_BYTE.withName("bDeviceSubClass"),
            JAVA_BYTE.withName("bDeviceProtocol"),
            JAVA_BYTE.withName("bMaxPacketSize0"),
            JAVA_SHORT.withByteAlignment(1).withName("idVendor"),
            JAVA_SHORT.withByteAlignment(1).withName("idProduct"),
            JAVA_SHORT.withByteAlignment(1).withName("bcdDevice"),
            JAVA_BYTE.withName("iManufacturer"),
            JAVA_BYTE.withName("iProduct"),
            JAVA_BYTE.withName("iSerialNumber"),
            JAVA_BYTE.withName("bNumConfigurations")
        ).withName("DeviceDescriptor"),
        JAVA_BYTE.withName("CurrentConfigurationValue"),
        JAVA_BYTE.withName("Speed"),
        JAVA_BYTE.withName("DeviceIsHub"),
        JAVA_SHORT.withByteAlignment(1).withName("DeviceAddress"),
        JAVA_INT.withByteAlignment(1).withName("NumberOfOpenPipes"),
        JAVA_INT.withByteAlignment(1).withName("ConnectionStatus"),
        MemoryLayout.sequenceLayout(0, MemoryLayout.structLayout(
            MemoryLayout.structLayout(
                JAVA_BYTE.withName("bLength"),
                JAVA_BYTE.withName("bDescriptorType"),
                JAVA_BYTE.withName("bEndpointAddress"),
                JAVA_BYTE.withName("bmAttributes"),
                JAVA_SHORT.withByteAlignment(1).withName("wMaxPacketSize"),
                JAVA_BYTE.withName("bInterval")
            ).withName("EndpointDescriptor"),
            JAVA_INT.withByteAlignment(1).withName("ScheduleOffset")
        ).withName("_USB_PIPE_INFO")).withName("PipeList")
    ).withName("_USB_NODE_CONNECTION_INFORMATION_EX");
    static final VarHandle const$3 = constants$1.const$2.varHandle(MemoryLayout.PathElement.groupElement("ConnectionIndex"));
    static final VarHandle const$4 = constants$1.const$2.varHandle(MemoryLayout.PathElement.groupElement("CurrentConfigurationValue"));
    static final VarHandle const$5 = constants$1.const$2.varHandle(MemoryLayout.PathElement.groupElement("Speed"));
}


