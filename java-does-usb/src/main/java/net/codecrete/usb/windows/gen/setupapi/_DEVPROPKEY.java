// Generated by jextract

package net.codecrete.usb.windows.gen.setupapi;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
/**
 * {@snippet :
 * struct _DEVPROPKEY {
 *     DEVPROPGUID fmtid;
 *     DEVPROPID pid;
 * };
 * }
 */
public class _DEVPROPKEY {

    static final StructLayout $struct$LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(
            Constants$root.C_LONG$LAYOUT.withName("Data1"),
            Constants$root.C_SHORT$LAYOUT.withName("Data2"),
            Constants$root.C_SHORT$LAYOUT.withName("Data3"),
            MemoryLayout.sequenceLayout(8, Constants$root.C_CHAR$LAYOUT).withName("Data4")
        ).withName("fmtid"),
        Constants$root.C_LONG$LAYOUT.withName("pid")
    ).withName("_DEVPROPKEY");
    public static MemoryLayout $LAYOUT() {
        return _DEVPROPKEY.$struct$LAYOUT;
    }
    public static MemorySegment fmtid$slice(MemorySegment seg) {
        return seg.asSlice(0, 16);
    }
    static final VarHandle pid$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("pid"));
    public static VarHandle pid$VH() {
        return _DEVPROPKEY.pid$VH;
    }
    /**
     * Getter for field:
     * {@snippet :
     * DEVPROPID pid;
     * }
     */
    public static int pid$get(MemorySegment seg) {
        return (int)_DEVPROPKEY.pid$VH.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * DEVPROPID pid;
     * }
     */
    public static void pid$set(MemorySegment seg, int x) {
        _DEVPROPKEY.pid$VH.set(seg, x);
    }
    public static int pid$get(MemorySegment seg, long index) {
        return (int)_DEVPROPKEY.pid$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void pid$set(MemorySegment seg, long index, int x) {
        _DEVPROPKEY.pid$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, SegmentScope scope) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, scope); }
}

