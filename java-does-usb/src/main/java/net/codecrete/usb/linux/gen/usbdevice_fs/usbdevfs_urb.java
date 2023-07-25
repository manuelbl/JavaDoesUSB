// Generated by jextract

package net.codecrete.usb.linux.gen.usbdevice_fs;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.VarHandle;
/**
 * {@snippet :
 * struct usbdevfs_urb {
 *     unsigned char type;
 *     unsigned char endpoint;
 *     int status;
 *     unsigned int flags;
 *     void* buffer;
 *     int buffer_length;
 *     int actual_length;
 *     int start_frame;
 *     union {
 *         int number_of_packets;
 *         unsigned int stream_id;
 *     };
 *     int error_count;
 *     unsigned int signr;
 *     void* usercontext;
 *     struct usbdevfs_iso_packet_desc iso_frame_desc[0];
 * };
 * }
 */
public class usbdevfs_urb {

    public static MemoryLayout $LAYOUT() {
        return constants$2.const$4;
    }
    public static VarHandle type$VH() {
        return constants$2.const$5;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char type;
     * }
     */
    public static byte type$get(MemorySegment seg) {
        return (byte)constants$2.const$5.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char type;
     * }
     */
    public static void type$set(MemorySegment seg, byte x) {
        constants$2.const$5.set(seg, x);
    }
    public static byte type$get(MemorySegment seg, long index) {
        return (byte)constants$2.const$5.get(seg.asSlice(index*sizeof()));
    }
    public static void type$set(MemorySegment seg, long index, byte x) {
        constants$2.const$5.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle endpoint$VH() {
        return constants$3.const$0;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned char endpoint;
     * }
     */
    public static byte endpoint$get(MemorySegment seg) {
        return (byte)constants$3.const$0.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned char endpoint;
     * }
     */
    public static void endpoint$set(MemorySegment seg, byte x) {
        constants$3.const$0.set(seg, x);
    }
    public static byte endpoint$get(MemorySegment seg, long index) {
        return (byte)constants$3.const$0.get(seg.asSlice(index*sizeof()));
    }
    public static void endpoint$set(MemorySegment seg, long index, byte x) {
        constants$3.const$0.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle status$VH() {
        return constants$3.const$1;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int status;
     * }
     */
    public static int status$get(MemorySegment seg) {
        return (int)constants$3.const$1.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int status;
     * }
     */
    public static void status$set(MemorySegment seg, int x) {
        constants$3.const$1.set(seg, x);
    }
    public static int status$get(MemorySegment seg, long index) {
        return (int)constants$3.const$1.get(seg.asSlice(index*sizeof()));
    }
    public static void status$set(MemorySegment seg, long index, int x) {
        constants$3.const$1.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle flags$VH() {
        return constants$3.const$2;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned int flags;
     * }
     */
    public static int flags$get(MemorySegment seg) {
        return (int)constants$3.const$2.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned int flags;
     * }
     */
    public static void flags$set(MemorySegment seg, int x) {
        constants$3.const$2.set(seg, x);
    }
    public static int flags$get(MemorySegment seg, long index) {
        return (int)constants$3.const$2.get(seg.asSlice(index*sizeof()));
    }
    public static void flags$set(MemorySegment seg, long index, int x) {
        constants$3.const$2.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle buffer$VH() {
        return constants$3.const$3;
    }
    /**
     * Getter for field:
     * {@snippet :
     * void* buffer;
     * }
     */
    public static MemorySegment buffer$get(MemorySegment seg) {
        return (java.lang.foreign.MemorySegment)constants$3.const$3.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * void* buffer;
     * }
     */
    public static void buffer$set(MemorySegment seg, MemorySegment x) {
        constants$3.const$3.set(seg, x);
    }
    public static MemorySegment buffer$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemorySegment)constants$3.const$3.get(seg.asSlice(index*sizeof()));
    }
    public static void buffer$set(MemorySegment seg, long index, MemorySegment x) {
        constants$3.const$3.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle buffer_length$VH() {
        return constants$3.const$4;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int buffer_length;
     * }
     */
    public static int buffer_length$get(MemorySegment seg) {
        return (int)constants$3.const$4.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int buffer_length;
     * }
     */
    public static void buffer_length$set(MemorySegment seg, int x) {
        constants$3.const$4.set(seg, x);
    }
    public static int buffer_length$get(MemorySegment seg, long index) {
        return (int)constants$3.const$4.get(seg.asSlice(index*sizeof()));
    }
    public static void buffer_length$set(MemorySegment seg, long index, int x) {
        constants$3.const$4.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle actual_length$VH() {
        return constants$3.const$5;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int actual_length;
     * }
     */
    public static int actual_length$get(MemorySegment seg) {
        return (int)constants$3.const$5.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int actual_length;
     * }
     */
    public static void actual_length$set(MemorySegment seg, int x) {
        constants$3.const$5.set(seg, x);
    }
    public static int actual_length$get(MemorySegment seg, long index) {
        return (int)constants$3.const$5.get(seg.asSlice(index*sizeof()));
    }
    public static void actual_length$set(MemorySegment seg, long index, int x) {
        constants$3.const$5.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle start_frame$VH() {
        return constants$4.const$0;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int start_frame;
     * }
     */
    public static int start_frame$get(MemorySegment seg) {
        return (int)constants$4.const$0.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int start_frame;
     * }
     */
    public static void start_frame$set(MemorySegment seg, int x) {
        constants$4.const$0.set(seg, x);
    }
    public static int start_frame$get(MemorySegment seg, long index) {
        return (int)constants$4.const$0.get(seg.asSlice(index*sizeof()));
    }
    public static void start_frame$set(MemorySegment seg, long index, int x) {
        constants$4.const$0.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle number_of_packets$VH() {
        return constants$4.const$1;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int number_of_packets;
     * }
     */
    public static int number_of_packets$get(MemorySegment seg) {
        return (int)constants$4.const$1.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int number_of_packets;
     * }
     */
    public static void number_of_packets$set(MemorySegment seg, int x) {
        constants$4.const$1.set(seg, x);
    }
    public static int number_of_packets$get(MemorySegment seg, long index) {
        return (int)constants$4.const$1.get(seg.asSlice(index*sizeof()));
    }
    public static void number_of_packets$set(MemorySegment seg, long index, int x) {
        constants$4.const$1.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle stream_id$VH() {
        return constants$4.const$2;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned int stream_id;
     * }
     */
    public static int stream_id$get(MemorySegment seg) {
        return (int)constants$4.const$2.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned int stream_id;
     * }
     */
    public static void stream_id$set(MemorySegment seg, int x) {
        constants$4.const$2.set(seg, x);
    }
    public static int stream_id$get(MemorySegment seg, long index) {
        return (int)constants$4.const$2.get(seg.asSlice(index*sizeof()));
    }
    public static void stream_id$set(MemorySegment seg, long index, int x) {
        constants$4.const$2.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle error_count$VH() {
        return constants$4.const$3;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int error_count;
     * }
     */
    public static int error_count$get(MemorySegment seg) {
        return (int)constants$4.const$3.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int error_count;
     * }
     */
    public static void error_count$set(MemorySegment seg, int x) {
        constants$4.const$3.set(seg, x);
    }
    public static int error_count$get(MemorySegment seg, long index) {
        return (int)constants$4.const$3.get(seg.asSlice(index*sizeof()));
    }
    public static void error_count$set(MemorySegment seg, long index, int x) {
        constants$4.const$3.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle signr$VH() {
        return constants$4.const$4;
    }
    /**
     * Getter for field:
     * {@snippet :
     * unsigned int signr;
     * }
     */
    public static int signr$get(MemorySegment seg) {
        return (int)constants$4.const$4.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * unsigned int signr;
     * }
     */
    public static void signr$set(MemorySegment seg, int x) {
        constants$4.const$4.set(seg, x);
    }
    public static int signr$get(MemorySegment seg, long index) {
        return (int)constants$4.const$4.get(seg.asSlice(index*sizeof()));
    }
    public static void signr$set(MemorySegment seg, long index, int x) {
        constants$4.const$4.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle usercontext$VH() {
        return constants$4.const$5;
    }
    /**
     * Getter for field:
     * {@snippet :
     * void* usercontext;
     * }
     */
    public static MemorySegment usercontext$get(MemorySegment seg) {
        return (java.lang.foreign.MemorySegment)constants$4.const$5.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * void* usercontext;
     * }
     */
    public static void usercontext$set(MemorySegment seg, MemorySegment x) {
        constants$4.const$5.set(seg, x);
    }
    public static MemorySegment usercontext$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemorySegment)constants$4.const$5.get(seg.asSlice(index*sizeof()));
    }
    public static void usercontext$set(MemorySegment seg, long index, MemorySegment x) {
        constants$4.const$5.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, Arena scope) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, scope); }
}


