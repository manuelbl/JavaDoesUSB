// Generated by jextract

package net.codecrete.usb.linux.gen.usbdevice_fs;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
public class usbdevfs_urb {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_CHAR$LAYOUT.withName("type"),
        Constants$root.C_CHAR$LAYOUT.withName("endpoint"),
        MemoryLayout.paddingLayout(16),
        Constants$root.C_INT$LAYOUT.withName("status"),
        Constants$root.C_INT$LAYOUT.withName("flags"),
        MemoryLayout.paddingLayout(32),
        Constants$root.C_POINTER$LAYOUT.withName("buffer"),
        Constants$root.C_INT$LAYOUT.withName("buffer_length"),
        Constants$root.C_INT$LAYOUT.withName("actual_length"),
        Constants$root.C_INT$LAYOUT.withName("start_frame"),
        MemoryLayout.unionLayout(
            Constants$root.C_INT$LAYOUT.withName("number_of_packets"),
            Constants$root.C_INT$LAYOUT.withName("stream_id")
        ).withName("$anon$0"),
        Constants$root.C_INT$LAYOUT.withName("error_count"),
        Constants$root.C_INT$LAYOUT.withName("signr"),
        Constants$root.C_POINTER$LAYOUT.withName("usercontext"),
        MemoryLayout.sequenceLayout(0, MemoryLayout.structLayout(
            Constants$root.C_INT$LAYOUT.withName("length"),
            Constants$root.C_INT$LAYOUT.withName("actual_length"),
            Constants$root.C_INT$LAYOUT.withName("status")
        ).withName("usbdevfs_iso_packet_desc")).withName("iso_frame_desc")
    ).withName("usbdevfs_urb");
    public static MemoryLayout $LAYOUT() {
        return usbdevfs_urb.$struct$LAYOUT;
    }
    static final VarHandle type$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("type"));
    public static VarHandle type$VH() {
        return usbdevfs_urb.type$VH;
    }
    public static byte type$get(MemorySegment seg) {
        return (byte)usbdevfs_urb.type$VH.get(seg);
    }
    public static void type$set( MemorySegment seg, byte x) {
        usbdevfs_urb.type$VH.set(seg, x);
    }
    public static byte type$get(MemorySegment seg, long index) {
        return (byte)usbdevfs_urb.type$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void type$set(MemorySegment seg, long index, byte x) {
        usbdevfs_urb.type$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle endpoint$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("endpoint"));
    public static VarHandle endpoint$VH() {
        return usbdevfs_urb.endpoint$VH;
    }
    public static byte endpoint$get(MemorySegment seg) {
        return (byte)usbdevfs_urb.endpoint$VH.get(seg);
    }
    public static void endpoint$set( MemorySegment seg, byte x) {
        usbdevfs_urb.endpoint$VH.set(seg, x);
    }
    public static byte endpoint$get(MemorySegment seg, long index) {
        return (byte)usbdevfs_urb.endpoint$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void endpoint$set(MemorySegment seg, long index, byte x) {
        usbdevfs_urb.endpoint$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle status$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("status"));
    public static VarHandle status$VH() {
        return usbdevfs_urb.status$VH;
    }
    public static int status$get(MemorySegment seg) {
        return (int)usbdevfs_urb.status$VH.get(seg);
    }
    public static void status$set( MemorySegment seg, int x) {
        usbdevfs_urb.status$VH.set(seg, x);
    }
    public static int status$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.status$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void status$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.status$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle flags$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("flags"));
    public static VarHandle flags$VH() {
        return usbdevfs_urb.flags$VH;
    }
    public static int flags$get(MemorySegment seg) {
        return (int)usbdevfs_urb.flags$VH.get(seg);
    }
    public static void flags$set( MemorySegment seg, int x) {
        usbdevfs_urb.flags$VH.set(seg, x);
    }
    public static int flags$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.flags$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void flags$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.flags$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle buffer$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("buffer"));
    public static VarHandle buffer$VH() {
        return usbdevfs_urb.buffer$VH;
    }
    public static MemoryAddress buffer$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)usbdevfs_urb.buffer$VH.get(seg);
    }
    public static void buffer$set( MemorySegment seg, MemoryAddress x) {
        usbdevfs_urb.buffer$VH.set(seg, x);
    }
    public static MemoryAddress buffer$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)usbdevfs_urb.buffer$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void buffer$set(MemorySegment seg, long index, MemoryAddress x) {
        usbdevfs_urb.buffer$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle buffer_length$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("buffer_length"));
    public static VarHandle buffer_length$VH() {
        return usbdevfs_urb.buffer_length$VH;
    }
    public static int buffer_length$get(MemorySegment seg) {
        return (int)usbdevfs_urb.buffer_length$VH.get(seg);
    }
    public static void buffer_length$set( MemorySegment seg, int x) {
        usbdevfs_urb.buffer_length$VH.set(seg, x);
    }
    public static int buffer_length$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.buffer_length$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void buffer_length$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.buffer_length$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle actual_length$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("actual_length"));
    public static VarHandle actual_length$VH() {
        return usbdevfs_urb.actual_length$VH;
    }
    public static int actual_length$get(MemorySegment seg) {
        return (int)usbdevfs_urb.actual_length$VH.get(seg);
    }
    public static void actual_length$set( MemorySegment seg, int x) {
        usbdevfs_urb.actual_length$VH.set(seg, x);
    }
    public static int actual_length$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.actual_length$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void actual_length$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.actual_length$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle start_frame$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("start_frame"));
    public static VarHandle start_frame$VH() {
        return usbdevfs_urb.start_frame$VH;
    }
    public static int start_frame$get(MemorySegment seg) {
        return (int)usbdevfs_urb.start_frame$VH.get(seg);
    }
    public static void start_frame$set( MemorySegment seg, int x) {
        usbdevfs_urb.start_frame$VH.set(seg, x);
    }
    public static int start_frame$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.start_frame$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void start_frame$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.start_frame$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle number_of_packets$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("$anon$0"), MemoryLayout.PathElement.groupElement("number_of_packets"));
    public static VarHandle number_of_packets$VH() {
        return usbdevfs_urb.number_of_packets$VH;
    }
    public static int number_of_packets$get(MemorySegment seg) {
        return (int)usbdevfs_urb.number_of_packets$VH.get(seg);
    }
    public static void number_of_packets$set( MemorySegment seg, int x) {
        usbdevfs_urb.number_of_packets$VH.set(seg, x);
    }
    public static int number_of_packets$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.number_of_packets$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void number_of_packets$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.number_of_packets$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle stream_id$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("$anon$0"), MemoryLayout.PathElement.groupElement("stream_id"));
    public static VarHandle stream_id$VH() {
        return usbdevfs_urb.stream_id$VH;
    }
    public static int stream_id$get(MemorySegment seg) {
        return (int)usbdevfs_urb.stream_id$VH.get(seg);
    }
    public static void stream_id$set( MemorySegment seg, int x) {
        usbdevfs_urb.stream_id$VH.set(seg, x);
    }
    public static int stream_id$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.stream_id$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void stream_id$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.stream_id$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle error_count$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("error_count"));
    public static VarHandle error_count$VH() {
        return usbdevfs_urb.error_count$VH;
    }
    public static int error_count$get(MemorySegment seg) {
        return (int)usbdevfs_urb.error_count$VH.get(seg);
    }
    public static void error_count$set( MemorySegment seg, int x) {
        usbdevfs_urb.error_count$VH.set(seg, x);
    }
    public static int error_count$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.error_count$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void error_count$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.error_count$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle signr$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("signr"));
    public static VarHandle signr$VH() {
        return usbdevfs_urb.signr$VH;
    }
    public static int signr$get(MemorySegment seg) {
        return (int)usbdevfs_urb.signr$VH.get(seg);
    }
    public static void signr$set( MemorySegment seg, int x) {
        usbdevfs_urb.signr$VH.set(seg, x);
    }
    public static int signr$get(MemorySegment seg, long index) {
        return (int)usbdevfs_urb.signr$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void signr$set(MemorySegment seg, long index, int x) {
        usbdevfs_urb.signr$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle usercontext$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("usercontext"));
    public static VarHandle usercontext$VH() {
        return usbdevfs_urb.usercontext$VH;
    }
    public static MemoryAddress usercontext$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)usbdevfs_urb.usercontext$VH.get(seg);
    }
    public static void usercontext$set( MemorySegment seg, MemoryAddress x) {
        usbdevfs_urb.usercontext$VH.set(seg, x);
    }
    public static MemoryAddress usercontext$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)usbdevfs_urb.usercontext$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void usercontext$set(MemorySegment seg, long index, MemoryAddress x) {
        usbdevfs_urb.usercontext$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


