// Generated by jextract

package net.codecrete.usb.windows.gen.user32;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct _DEV_BROADCAST_HDR {
 *     DWORD dbch_size;
 *     DWORD dbch_devicetype;
 *     DWORD dbch_reserved;
 * }
 * }
 */
public class _DEV_BROADCAST_HDR {

    _DEV_BROADCAST_HDR() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        User32.C_LONG.withName("dbch_size"),
        User32.C_LONG.withName("dbch_devicetype"),
        User32.C_LONG.withName("dbch_reserved")
    ).withName("_DEV_BROADCAST_HDR");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfInt dbch_size$LAYOUT = (OfInt)$LAYOUT.select(groupElement("dbch_size"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * DWORD dbch_size
     * }
     */
    public static final OfInt dbch_size$layout() {
        return dbch_size$LAYOUT;
    }

    private static final long dbch_size$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * DWORD dbch_size
     * }
     */
    public static final long dbch_size$offset() {
        return dbch_size$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * DWORD dbch_size
     * }
     */
    public static int dbch_size(MemorySegment struct) {
        return struct.get(dbch_size$LAYOUT, dbch_size$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * DWORD dbch_size
     * }
     */
    public static void dbch_size(MemorySegment struct, int fieldValue) {
        struct.set(dbch_size$LAYOUT, dbch_size$OFFSET, fieldValue);
    }

    private static final OfInt dbch_devicetype$LAYOUT = (OfInt)$LAYOUT.select(groupElement("dbch_devicetype"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * DWORD dbch_devicetype
     * }
     */
    public static final OfInt dbch_devicetype$layout() {
        return dbch_devicetype$LAYOUT;
    }

    private static final long dbch_devicetype$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * DWORD dbch_devicetype
     * }
     */
    public static final long dbch_devicetype$offset() {
        return dbch_devicetype$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * DWORD dbch_devicetype
     * }
     */
    public static int dbch_devicetype(MemorySegment struct) {
        return struct.get(dbch_devicetype$LAYOUT, dbch_devicetype$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * DWORD dbch_devicetype
     * }
     */
    public static void dbch_devicetype(MemorySegment struct, int fieldValue) {
        struct.set(dbch_devicetype$LAYOUT, dbch_devicetype$OFFSET, fieldValue);
    }

    private static final OfInt dbch_reserved$LAYOUT = (OfInt)$LAYOUT.select(groupElement("dbch_reserved"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * DWORD dbch_reserved
     * }
     */
    public static final OfInt dbch_reserved$layout() {
        return dbch_reserved$LAYOUT;
    }

    private static final long dbch_reserved$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * DWORD dbch_reserved
     * }
     */
    public static final long dbch_reserved$offset() {
        return dbch_reserved$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * DWORD dbch_reserved
     * }
     */
    public static int dbch_reserved(MemorySegment struct) {
        return struct.get(dbch_reserved$LAYOUT, dbch_reserved$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * DWORD dbch_reserved
     * }
     */
    public static void dbch_reserved(MemorySegment struct, int fieldValue) {
        struct.set(dbch_reserved$LAYOUT, dbch_reserved$OFFSET, fieldValue);
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction) (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction) (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}

