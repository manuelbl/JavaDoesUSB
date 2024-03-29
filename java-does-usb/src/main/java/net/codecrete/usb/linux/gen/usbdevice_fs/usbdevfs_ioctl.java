// Generated by jextract

package net.codecrete.usb.linux.gen.usbdevice_fs;

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
 * struct usbdevfs_ioctl {
 *     int ifno;
 *     int ioctl_code;
 *     void *data;
 * }
 * }
 */
public class usbdevfs_ioctl {

    usbdevfs_ioctl() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        usbdevice_fs.C_INT.withName("ifno"),
        usbdevice_fs.C_INT.withName("ioctl_code"),
        usbdevice_fs.C_POINTER.withName("data")
    ).withName("usbdevfs_ioctl");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfInt ifno$LAYOUT = (OfInt)$LAYOUT.select(groupElement("ifno"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ifno
     * }
     */
    public static final OfInt ifno$layout() {
        return ifno$LAYOUT;
    }

    private static final long ifno$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ifno
     * }
     */
    public static final long ifno$offset() {
        return ifno$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ifno
     * }
     */
    public static int ifno(MemorySegment struct) {
        return struct.get(ifno$LAYOUT, ifno$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ifno
     * }
     */
    public static void ifno(MemorySegment struct, int fieldValue) {
        struct.set(ifno$LAYOUT, ifno$OFFSET, fieldValue);
    }

    private static final OfInt ioctl_code$LAYOUT = (OfInt)$LAYOUT.select(groupElement("ioctl_code"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ioctl_code
     * }
     */
    public static final OfInt ioctl_code$layout() {
        return ioctl_code$LAYOUT;
    }

    private static final long ioctl_code$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ioctl_code
     * }
     */
    public static final long ioctl_code$offset() {
        return ioctl_code$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ioctl_code
     * }
     */
    public static int ioctl_code(MemorySegment struct) {
        return struct.get(ioctl_code$LAYOUT, ioctl_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ioctl_code
     * }
     */
    public static void ioctl_code(MemorySegment struct, int fieldValue) {
        struct.set(ioctl_code$LAYOUT, ioctl_code$OFFSET, fieldValue);
    }

    private static final AddressLayout data$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("data"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * void *data
     * }
     */
    public static final AddressLayout data$layout() {
        return data$LAYOUT;
    }

    private static final long data$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * void *data
     * }
     */
    public static final long data$offset() {
        return data$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * void *data
     * }
     */
    public static MemorySegment data(MemorySegment struct) {
        return struct.get(data$LAYOUT, data$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * void *data
     * }
     */
    public static void data(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(data$LAYOUT, data$OFFSET, fieldValue);
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

