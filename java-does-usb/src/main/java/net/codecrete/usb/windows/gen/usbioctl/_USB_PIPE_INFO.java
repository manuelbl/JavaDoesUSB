// Generated by jextract

package net.codecrete.usb.windows.gen.usbioctl;

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.OfInt;

/**
 * {@snippet lang=c :
 * struct _USB_PIPE_INFO {
 *     USB_ENDPOINT_DESCRIPTOR EndpointDescriptor;
 *     ULONG ScheduleOffset;
 * }
 * }
 */
public class _USB_PIPE_INFO {

    _USB_PIPE_INFO() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        _USB_ENDPOINT_DESCRIPTOR.layout().withName("EndpointDescriptor"),
        USBIoctl.align(USBIoctl.C_LONG, 1).withName("ScheduleOffset")
    ).withName("_USB_PIPE_INFO");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final GroupLayout EndpointDescriptor$LAYOUT = (GroupLayout)$LAYOUT.select(groupElement("EndpointDescriptor"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * USB_ENDPOINT_DESCRIPTOR EndpointDescriptor
     * }
     */
    public static final GroupLayout EndpointDescriptor$layout() {
        return EndpointDescriptor$LAYOUT;
    }

    private static final long EndpointDescriptor$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * USB_ENDPOINT_DESCRIPTOR EndpointDescriptor
     * }
     */
    public static final long EndpointDescriptor$offset() {
        return EndpointDescriptor$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * USB_ENDPOINT_DESCRIPTOR EndpointDescriptor
     * }
     */
    public static MemorySegment EndpointDescriptor(MemorySegment struct) {
        return struct.asSlice(EndpointDescriptor$OFFSET, EndpointDescriptor$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * USB_ENDPOINT_DESCRIPTOR EndpointDescriptor
     * }
     */
    public static void EndpointDescriptor(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, EndpointDescriptor$OFFSET, EndpointDescriptor$LAYOUT.byteSize());
    }

    private static final OfInt ScheduleOffset$LAYOUT = (OfInt)$LAYOUT.select(groupElement("ScheduleOffset"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * ULONG ScheduleOffset
     * }
     */
    public static final OfInt ScheduleOffset$layout() {
        return ScheduleOffset$LAYOUT;
    }

    private static final long ScheduleOffset$OFFSET = 7;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * ULONG ScheduleOffset
     * }
     */
    public static final long ScheduleOffset$offset() {
        return ScheduleOffset$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * ULONG ScheduleOffset
     * }
     */
    public static int ScheduleOffset(MemorySegment struct) {
        return struct.get(ScheduleOffset$LAYOUT, ScheduleOffset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * ULONG ScheduleOffset
     * }
     */
    public static void ScheduleOffset(MemorySegment struct, int fieldValue) {
        struct.set(ScheduleOffset$LAYOUT, ScheduleOffset$OFFSET, fieldValue);
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

