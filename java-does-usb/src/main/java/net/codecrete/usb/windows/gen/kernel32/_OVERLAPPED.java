// Generated by jextract

package net.codecrete.usb.windows.gen.kernel32;

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
 * struct _OVERLAPPED {
 *     ULONG_PTR Internal;
 *     ULONG_PTR InternalHigh;
 *     union {
 *         struct {
 *             DWORD Offset;
 *             DWORD OffsetHigh;
 *         };
 *         PVOID Pointer;
 *     };
 *     HANDLE hEvent;
 * }
 * }
 */
public class _OVERLAPPED {

    _OVERLAPPED() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        Kernel32.C_LONG_LONG.withName("Internal"),
        Kernel32.C_LONG_LONG.withName("InternalHigh"),
        MemoryLayout.unionLayout(
            MemoryLayout.structLayout(
                Kernel32.C_LONG.withName("Offset"),
                Kernel32.C_LONG.withName("OffsetHigh")
            ).withName("$anon$56:9"),
            Kernel32.C_POINTER.withName("Pointer")
        ).withName("$anon$55:5"),
        Kernel32.C_POINTER.withName("hEvent")
    ).withName("_OVERLAPPED");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfLong Internal$LAYOUT = (OfLong)$LAYOUT.select(groupElement("Internal"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * ULONG_PTR Internal
     * }
     */
    public static final OfLong Internal$layout() {
        return Internal$LAYOUT;
    }

    private static final long Internal$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * ULONG_PTR Internal
     * }
     */
    public static final long Internal$offset() {
        return Internal$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * ULONG_PTR Internal
     * }
     */
    public static long Internal(MemorySegment struct) {
        return struct.get(Internal$LAYOUT, Internal$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * ULONG_PTR Internal
     * }
     */
    public static void Internal(MemorySegment struct, long fieldValue) {
        struct.set(Internal$LAYOUT, Internal$OFFSET, fieldValue);
    }

    private static final OfLong InternalHigh$LAYOUT = (OfLong)$LAYOUT.select(groupElement("InternalHigh"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * ULONG_PTR InternalHigh
     * }
     */
    public static final OfLong InternalHigh$layout() {
        return InternalHigh$LAYOUT;
    }

    private static final long InternalHigh$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * ULONG_PTR InternalHigh
     * }
     */
    public static final long InternalHigh$offset() {
        return InternalHigh$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * ULONG_PTR InternalHigh
     * }
     */
    public static long InternalHigh(MemorySegment struct) {
        return struct.get(InternalHigh$LAYOUT, InternalHigh$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * ULONG_PTR InternalHigh
     * }
     */
    public static void InternalHigh(MemorySegment struct, long fieldValue) {
        struct.set(InternalHigh$LAYOUT, InternalHigh$OFFSET, fieldValue);
    }

    private static final OfInt Offset$LAYOUT = (OfInt)$LAYOUT.select(groupElement("$anon$55:5"), groupElement("$anon$56:9"), groupElement("Offset"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * DWORD Offset
     * }
     */
    public static final OfInt Offset$layout() {
        return Offset$LAYOUT;
    }

    private static final long Offset$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * DWORD Offset
     * }
     */
    public static final long Offset$offset() {
        return Offset$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * DWORD Offset
     * }
     */
    public static int Offset(MemorySegment struct) {
        return struct.get(Offset$LAYOUT, Offset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * DWORD Offset
     * }
     */
    public static void Offset(MemorySegment struct, int fieldValue) {
        struct.set(Offset$LAYOUT, Offset$OFFSET, fieldValue);
    }

    private static final OfInt OffsetHigh$LAYOUT = (OfInt)$LAYOUT.select(groupElement("$anon$55:5"), groupElement("$anon$56:9"), groupElement("OffsetHigh"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * DWORD OffsetHigh
     * }
     */
    public static final OfInt OffsetHigh$layout() {
        return OffsetHigh$LAYOUT;
    }

    private static final long OffsetHigh$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * DWORD OffsetHigh
     * }
     */
    public static final long OffsetHigh$offset() {
        return OffsetHigh$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * DWORD OffsetHigh
     * }
     */
    public static int OffsetHigh(MemorySegment struct) {
        return struct.get(OffsetHigh$LAYOUT, OffsetHigh$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * DWORD OffsetHigh
     * }
     */
    public static void OffsetHigh(MemorySegment struct, int fieldValue) {
        struct.set(OffsetHigh$LAYOUT, OffsetHigh$OFFSET, fieldValue);
    }

    private static final AddressLayout Pointer$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("$anon$55:5"), groupElement("Pointer"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * PVOID Pointer
     * }
     */
    public static final AddressLayout Pointer$layout() {
        return Pointer$LAYOUT;
    }

    private static final long Pointer$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * PVOID Pointer
     * }
     */
    public static final long Pointer$offset() {
        return Pointer$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * PVOID Pointer
     * }
     */
    public static MemorySegment Pointer(MemorySegment struct) {
        return struct.get(Pointer$LAYOUT, Pointer$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * PVOID Pointer
     * }
     */
    public static void Pointer(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(Pointer$LAYOUT, Pointer$OFFSET, fieldValue);
    }

    private static final AddressLayout hEvent$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("hEvent"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * HANDLE hEvent
     * }
     */
    public static final AddressLayout hEvent$layout() {
        return hEvent$LAYOUT;
    }

    private static final long hEvent$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * HANDLE hEvent
     * }
     */
    public static final long hEvent$offset() {
        return hEvent$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * HANDLE hEvent
     * }
     */
    public static MemorySegment hEvent(MemorySegment struct) {
        return struct.get(hEvent$LAYOUT, hEvent$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * HANDLE hEvent
     * }
     */
    public static void hEvent(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(hEvent$LAYOUT, hEvent$OFFSET, fieldValue);
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

