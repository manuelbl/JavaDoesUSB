// Generated by jextract

package net.codecrete.usb.windows.gen.usbioctl;

import java.lang.foreign.AddressLayout;

import static java.lang.foreign.ValueLayout.*;
public class USBIoctl  {

    public static final OfByte C_CHAR = JAVA_BYTE;
    public static final OfShort C_SHORT = JAVA_SHORT;
    public static final OfInt C_INT = JAVA_INT;
    public static final OfInt C_LONG = JAVA_INT;
    public static final OfLong C_LONG_LONG = JAVA_LONG;
    public static final OfFloat C_FLOAT = JAVA_FLOAT;
    public static final OfDouble C_DOUBLE = JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = RuntimeHelper.POINTER;
    /**
     * {@snippet :
     * #define IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION 2229264
     * }
     */
    public static int IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION() {
        return (int)2229264L;
    }
    /**
     * {@snippet :
     * #define IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX 2229320
     * }
     */
    public static int IOCTL_USB_GET_NODE_CONNECTION_INFORMATION_EX() {
        return (int)2229320L;
    }
}


