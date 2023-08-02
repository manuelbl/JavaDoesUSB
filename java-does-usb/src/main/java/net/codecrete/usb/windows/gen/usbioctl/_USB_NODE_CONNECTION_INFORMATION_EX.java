// Generated by jextract

package net.codecrete.usb.windows.gen.usbioctl;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.VarHandle;
/**
 * {@snippet :
 * struct _USB_NODE_CONNECTION_INFORMATION_EX {
 *     ULONG ConnectionIndex;
 *     USB_DEVICE_DESCRIPTOR DeviceDescriptor;
 *     UCHAR CurrentConfigurationValue;
 *     UCHAR Speed;
 *     BOOLEAN DeviceIsHub;
 *     USHORT DeviceAddress;
 *     ULONG NumberOfOpenPipes;
 *     USB_CONNECTION_STATUS ConnectionStatus;
 *     USB_PIPE_INFO PipeList[0];
 * };
 * }
 */
public class _USB_NODE_CONNECTION_INFORMATION_EX {

    public static MemoryLayout $LAYOUT() {
        return constants$1.const$2;
    }
    public static VarHandle ConnectionIndex$VH() {
        return constants$1.const$3;
    }
    /**
     * Getter for field:
     * {@snippet :
     * ULONG ConnectionIndex;
     * }
     */
    public static int ConnectionIndex$get(MemorySegment seg) {
        return (int)constants$1.const$3.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * ULONG ConnectionIndex;
     * }
     */
    public static void ConnectionIndex$set(MemorySegment seg, int x) {
        constants$1.const$3.set(seg, x);
    }
    public static int ConnectionIndex$get(MemorySegment seg, long index) {
        return (int)constants$1.const$3.get(seg.asSlice(index*sizeof()));
    }
    public static void ConnectionIndex$set(MemorySegment seg, long index, int x) {
        constants$1.const$3.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment DeviceDescriptor$slice(MemorySegment seg) {
        return seg.asSlice(4, 18);
    }
    public static VarHandle CurrentConfigurationValue$VH() {
        return constants$1.const$4;
    }
    /**
     * Getter for field:
     * {@snippet :
     * UCHAR CurrentConfigurationValue;
     * }
     */
    public static byte CurrentConfigurationValue$get(MemorySegment seg) {
        return (byte)constants$1.const$4.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * UCHAR CurrentConfigurationValue;
     * }
     */
    public static void CurrentConfigurationValue$set(MemorySegment seg, byte x) {
        constants$1.const$4.set(seg, x);
    }
    public static byte CurrentConfigurationValue$get(MemorySegment seg, long index) {
        return (byte)constants$1.const$4.get(seg.asSlice(index*sizeof()));
    }
    public static void CurrentConfigurationValue$set(MemorySegment seg, long index, byte x) {
        constants$1.const$4.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle Speed$VH() {
        return constants$1.const$5;
    }
    /**
     * Getter for field:
     * {@snippet :
     * UCHAR Speed;
     * }
     */
    public static byte Speed$get(MemorySegment seg) {
        return (byte)constants$1.const$5.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * UCHAR Speed;
     * }
     */
    public static void Speed$set(MemorySegment seg, byte x) {
        constants$1.const$5.set(seg, x);
    }
    public static byte Speed$get(MemorySegment seg, long index) {
        return (byte)constants$1.const$5.get(seg.asSlice(index*sizeof()));
    }
    public static void Speed$set(MemorySegment seg, long index, byte x) {
        constants$1.const$5.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle DeviceIsHub$VH() {
        return constants$2.const$0;
    }
    /**
     * Getter for field:
     * {@snippet :
     * BOOLEAN DeviceIsHub;
     * }
     */
    public static byte DeviceIsHub$get(MemorySegment seg) {
        return (byte)constants$2.const$0.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * BOOLEAN DeviceIsHub;
     * }
     */
    public static void DeviceIsHub$set(MemorySegment seg, byte x) {
        constants$2.const$0.set(seg, x);
    }
    public static byte DeviceIsHub$get(MemorySegment seg, long index) {
        return (byte)constants$2.const$0.get(seg.asSlice(index*sizeof()));
    }
    public static void DeviceIsHub$set(MemorySegment seg, long index, byte x) {
        constants$2.const$0.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle DeviceAddress$VH() {
        return constants$2.const$1;
    }
    /**
     * Getter for field:
     * {@snippet :
     * USHORT DeviceAddress;
     * }
     */
    public static short DeviceAddress$get(MemorySegment seg) {
        return (short)constants$2.const$1.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * USHORT DeviceAddress;
     * }
     */
    public static void DeviceAddress$set(MemorySegment seg, short x) {
        constants$2.const$1.set(seg, x);
    }
    public static short DeviceAddress$get(MemorySegment seg, long index) {
        return (short)constants$2.const$1.get(seg.asSlice(index*sizeof()));
    }
    public static void DeviceAddress$set(MemorySegment seg, long index, short x) {
        constants$2.const$1.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle NumberOfOpenPipes$VH() {
        return constants$2.const$2;
    }
    /**
     * Getter for field:
     * {@snippet :
     * ULONG NumberOfOpenPipes;
     * }
     */
    public static int NumberOfOpenPipes$get(MemorySegment seg) {
        return (int)constants$2.const$2.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * ULONG NumberOfOpenPipes;
     * }
     */
    public static void NumberOfOpenPipes$set(MemorySegment seg, int x) {
        constants$2.const$2.set(seg, x);
    }
    public static int NumberOfOpenPipes$get(MemorySegment seg, long index) {
        return (int)constants$2.const$2.get(seg.asSlice(index*sizeof()));
    }
    public static void NumberOfOpenPipes$set(MemorySegment seg, long index, int x) {
        constants$2.const$2.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle ConnectionStatus$VH() {
        return constants$2.const$3;
    }
    /**
     * Getter for field:
     * {@snippet :
     * USB_CONNECTION_STATUS ConnectionStatus;
     * }
     */
    public static int ConnectionStatus$get(MemorySegment seg) {
        return (int)constants$2.const$3.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * USB_CONNECTION_STATUS ConnectionStatus;
     * }
     */
    public static void ConnectionStatus$set(MemorySegment seg, int x) {
        constants$2.const$3.set(seg, x);
    }
    public static int ConnectionStatus$get(MemorySegment seg, long index) {
        return (int)constants$2.const$3.get(seg.asSlice(index*sizeof()));
    }
    public static void ConnectionStatus$set(MemorySegment seg, long index, int x) {
        constants$2.const$3.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, Arena scope) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, scope); }
}

