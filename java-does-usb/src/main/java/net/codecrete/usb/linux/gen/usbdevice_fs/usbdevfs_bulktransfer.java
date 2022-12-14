// Generated by jextract

package net.codecrete.usb.linux.gen.usbdevice_fs;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
public class usbdevfs_bulktransfer {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("ep"),
        Constants$root.C_INT$LAYOUT.withName("len"),
        Constants$root.C_INT$LAYOUT.withName("timeout"),
        MemoryLayout.paddingLayout(32),
        Constants$root.C_POINTER$LAYOUT.withName("data")
    ).withName("usbdevfs_bulktransfer");
    public static MemoryLayout $LAYOUT() {
        return usbdevfs_bulktransfer.$struct$LAYOUT;
    }
    static final VarHandle ep$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ep"));
    public static VarHandle ep$VH() {
        return usbdevfs_bulktransfer.ep$VH;
    }
    public static int ep$get(MemorySegment seg) {
        return (int)usbdevfs_bulktransfer.ep$VH.get(seg);
    }
    public static void ep$set( MemorySegment seg, int x) {
        usbdevfs_bulktransfer.ep$VH.set(seg, x);
    }
    public static int ep$get(MemorySegment seg, long index) {
        return (int)usbdevfs_bulktransfer.ep$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void ep$set(MemorySegment seg, long index, int x) {
        usbdevfs_bulktransfer.ep$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle len$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("len"));
    public static VarHandle len$VH() {
        return usbdevfs_bulktransfer.len$VH;
    }
    public static int len$get(MemorySegment seg) {
        return (int)usbdevfs_bulktransfer.len$VH.get(seg);
    }
    public static void len$set( MemorySegment seg, int x) {
        usbdevfs_bulktransfer.len$VH.set(seg, x);
    }
    public static int len$get(MemorySegment seg, long index) {
        return (int)usbdevfs_bulktransfer.len$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void len$set(MemorySegment seg, long index, int x) {
        usbdevfs_bulktransfer.len$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle timeout$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("timeout"));
    public static VarHandle timeout$VH() {
        return usbdevfs_bulktransfer.timeout$VH;
    }
    public static int timeout$get(MemorySegment seg) {
        return (int)usbdevfs_bulktransfer.timeout$VH.get(seg);
    }
    public static void timeout$set( MemorySegment seg, int x) {
        usbdevfs_bulktransfer.timeout$VH.set(seg, x);
    }
    public static int timeout$get(MemorySegment seg, long index) {
        return (int)usbdevfs_bulktransfer.timeout$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void timeout$set(MemorySegment seg, long index, int x) {
        usbdevfs_bulktransfer.timeout$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle data$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));
    public static VarHandle data$VH() {
        return usbdevfs_bulktransfer.data$VH;
    }
    public static MemoryAddress data$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)usbdevfs_bulktransfer.data$VH.get(seg);
    }
    public static void data$set( MemorySegment seg, MemoryAddress x) {
        usbdevfs_bulktransfer.data$VH.set(seg, x);
    }
    public static MemoryAddress data$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)usbdevfs_bulktransfer.data$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void data$set(MemorySegment seg, long index, MemoryAddress x) {
        usbdevfs_bulktransfer.data$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


