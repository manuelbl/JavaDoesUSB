// Generated by jextract

package net.codecrete.usb.windows.gen.setupapi;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
public class _SP_DEVICE_INTERFACE_DETAIL_DATA_W {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_LONG$LAYOUT.withName("cbSize"),
        MemoryLayout.sequenceLayout(1, Constants$root.C_SHORT$LAYOUT).withName("DevicePath"),
        MemoryLayout.paddingLayout(16)
    ).withName("_SP_DEVICE_INTERFACE_DETAIL_DATA_W");
    public static MemoryLayout $LAYOUT() {
        return _SP_DEVICE_INTERFACE_DETAIL_DATA_W.$struct$LAYOUT;
    }
    static final VarHandle cbSize$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cbSize"));
    public static VarHandle cbSize$VH() {
        return _SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$VH;
    }
    public static int cbSize$get(MemorySegment seg) {
        return (int)_SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$VH.get(seg);
    }
    public static void cbSize$set( MemorySegment seg, int x) {
        _SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$VH.set(seg, x);
    }
    public static int cbSize$get(MemorySegment seg, long index) {
        return (int)_SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void cbSize$set(MemorySegment seg, long index, int x) {
        _SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment DevicePath$slice(MemorySegment seg) {
        return seg.asSlice(4, 2);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


