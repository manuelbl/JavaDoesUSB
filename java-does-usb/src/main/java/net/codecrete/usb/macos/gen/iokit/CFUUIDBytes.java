// Generated by jextract

package net.codecrete.usb.macos.gen.iokit;

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
 * struct {
 *     UInt8 byte0;
 *     UInt8 byte1;
 *     UInt8 byte2;
 *     UInt8 byte3;
 *     UInt8 byte4;
 *     UInt8 byte5;
 *     UInt8 byte6;
 *     UInt8 byte7;
 *     UInt8 byte8;
 *     UInt8 byte9;
 *     UInt8 byte10;
 *     UInt8 byte11;
 *     UInt8 byte12;
 *     UInt8 byte13;
 *     UInt8 byte14;
 *     UInt8 byte15;
 * }
 * }
 */
public class CFUUIDBytes {

    CFUUIDBytes() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        IOKit.C_CHAR.withName("byte0"),
        IOKit.C_CHAR.withName("byte1"),
        IOKit.C_CHAR.withName("byte2"),
        IOKit.C_CHAR.withName("byte3"),
        IOKit.C_CHAR.withName("byte4"),
        IOKit.C_CHAR.withName("byte5"),
        IOKit.C_CHAR.withName("byte6"),
        IOKit.C_CHAR.withName("byte7"),
        IOKit.C_CHAR.withName("byte8"),
        IOKit.C_CHAR.withName("byte9"),
        IOKit.C_CHAR.withName("byte10"),
        IOKit.C_CHAR.withName("byte11"),
        IOKit.C_CHAR.withName("byte12"),
        IOKit.C_CHAR.withName("byte13"),
        IOKit.C_CHAR.withName("byte14"),
        IOKit.C_CHAR.withName("byte15")
    ).withName("CFUUIDBytes");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfByte byte0$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte0"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte0
     * }
     */
    public static final OfByte byte0$layout() {
        return byte0$LAYOUT;
    }

    private static final long byte0$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte0
     * }
     */
    public static final long byte0$offset() {
        return byte0$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte0
     * }
     */
    public static byte byte0(MemorySegment struct) {
        return struct.get(byte0$LAYOUT, byte0$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte0
     * }
     */
    public static void byte0(MemorySegment struct, byte fieldValue) {
        struct.set(byte0$LAYOUT, byte0$OFFSET, fieldValue);
    }

    private static final OfByte byte1$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte1"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte1
     * }
     */
    public static final OfByte byte1$layout() {
        return byte1$LAYOUT;
    }

    private static final long byte1$OFFSET = 1;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte1
     * }
     */
    public static final long byte1$offset() {
        return byte1$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte1
     * }
     */
    public static byte byte1(MemorySegment struct) {
        return struct.get(byte1$LAYOUT, byte1$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte1
     * }
     */
    public static void byte1(MemorySegment struct, byte fieldValue) {
        struct.set(byte1$LAYOUT, byte1$OFFSET, fieldValue);
    }

    private static final OfByte byte2$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte2"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte2
     * }
     */
    public static final OfByte byte2$layout() {
        return byte2$LAYOUT;
    }

    private static final long byte2$OFFSET = 2;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte2
     * }
     */
    public static final long byte2$offset() {
        return byte2$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte2
     * }
     */
    public static byte byte2(MemorySegment struct) {
        return struct.get(byte2$LAYOUT, byte2$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte2
     * }
     */
    public static void byte2(MemorySegment struct, byte fieldValue) {
        struct.set(byte2$LAYOUT, byte2$OFFSET, fieldValue);
    }

    private static final OfByte byte3$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte3"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte3
     * }
     */
    public static final OfByte byte3$layout() {
        return byte3$LAYOUT;
    }

    private static final long byte3$OFFSET = 3;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte3
     * }
     */
    public static final long byte3$offset() {
        return byte3$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte3
     * }
     */
    public static byte byte3(MemorySegment struct) {
        return struct.get(byte3$LAYOUT, byte3$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte3
     * }
     */
    public static void byte3(MemorySegment struct, byte fieldValue) {
        struct.set(byte3$LAYOUT, byte3$OFFSET, fieldValue);
    }

    private static final OfByte byte4$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte4"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte4
     * }
     */
    public static final OfByte byte4$layout() {
        return byte4$LAYOUT;
    }

    private static final long byte4$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte4
     * }
     */
    public static final long byte4$offset() {
        return byte4$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte4
     * }
     */
    public static byte byte4(MemorySegment struct) {
        return struct.get(byte4$LAYOUT, byte4$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte4
     * }
     */
    public static void byte4(MemorySegment struct, byte fieldValue) {
        struct.set(byte4$LAYOUT, byte4$OFFSET, fieldValue);
    }

    private static final OfByte byte5$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte5"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte5
     * }
     */
    public static final OfByte byte5$layout() {
        return byte5$LAYOUT;
    }

    private static final long byte5$OFFSET = 5;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte5
     * }
     */
    public static final long byte5$offset() {
        return byte5$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte5
     * }
     */
    public static byte byte5(MemorySegment struct) {
        return struct.get(byte5$LAYOUT, byte5$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte5
     * }
     */
    public static void byte5(MemorySegment struct, byte fieldValue) {
        struct.set(byte5$LAYOUT, byte5$OFFSET, fieldValue);
    }

    private static final OfByte byte6$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte6"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte6
     * }
     */
    public static final OfByte byte6$layout() {
        return byte6$LAYOUT;
    }

    private static final long byte6$OFFSET = 6;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte6
     * }
     */
    public static final long byte6$offset() {
        return byte6$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte6
     * }
     */
    public static byte byte6(MemorySegment struct) {
        return struct.get(byte6$LAYOUT, byte6$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte6
     * }
     */
    public static void byte6(MemorySegment struct, byte fieldValue) {
        struct.set(byte6$LAYOUT, byte6$OFFSET, fieldValue);
    }

    private static final OfByte byte7$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte7"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte7
     * }
     */
    public static final OfByte byte7$layout() {
        return byte7$LAYOUT;
    }

    private static final long byte7$OFFSET = 7;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte7
     * }
     */
    public static final long byte7$offset() {
        return byte7$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte7
     * }
     */
    public static byte byte7(MemorySegment struct) {
        return struct.get(byte7$LAYOUT, byte7$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte7
     * }
     */
    public static void byte7(MemorySegment struct, byte fieldValue) {
        struct.set(byte7$LAYOUT, byte7$OFFSET, fieldValue);
    }

    private static final OfByte byte8$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte8"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte8
     * }
     */
    public static final OfByte byte8$layout() {
        return byte8$LAYOUT;
    }

    private static final long byte8$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte8
     * }
     */
    public static final long byte8$offset() {
        return byte8$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte8
     * }
     */
    public static byte byte8(MemorySegment struct) {
        return struct.get(byte8$LAYOUT, byte8$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte8
     * }
     */
    public static void byte8(MemorySegment struct, byte fieldValue) {
        struct.set(byte8$LAYOUT, byte8$OFFSET, fieldValue);
    }

    private static final OfByte byte9$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte9"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte9
     * }
     */
    public static final OfByte byte9$layout() {
        return byte9$LAYOUT;
    }

    private static final long byte9$OFFSET = 9;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte9
     * }
     */
    public static final long byte9$offset() {
        return byte9$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte9
     * }
     */
    public static byte byte9(MemorySegment struct) {
        return struct.get(byte9$LAYOUT, byte9$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte9
     * }
     */
    public static void byte9(MemorySegment struct, byte fieldValue) {
        struct.set(byte9$LAYOUT, byte9$OFFSET, fieldValue);
    }

    private static final OfByte byte10$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte10"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte10
     * }
     */
    public static final OfByte byte10$layout() {
        return byte10$LAYOUT;
    }

    private static final long byte10$OFFSET = 10;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte10
     * }
     */
    public static final long byte10$offset() {
        return byte10$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte10
     * }
     */
    public static byte byte10(MemorySegment struct) {
        return struct.get(byte10$LAYOUT, byte10$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte10
     * }
     */
    public static void byte10(MemorySegment struct, byte fieldValue) {
        struct.set(byte10$LAYOUT, byte10$OFFSET, fieldValue);
    }

    private static final OfByte byte11$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte11"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte11
     * }
     */
    public static final OfByte byte11$layout() {
        return byte11$LAYOUT;
    }

    private static final long byte11$OFFSET = 11;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte11
     * }
     */
    public static final long byte11$offset() {
        return byte11$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte11
     * }
     */
    public static byte byte11(MemorySegment struct) {
        return struct.get(byte11$LAYOUT, byte11$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte11
     * }
     */
    public static void byte11(MemorySegment struct, byte fieldValue) {
        struct.set(byte11$LAYOUT, byte11$OFFSET, fieldValue);
    }

    private static final OfByte byte12$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte12"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte12
     * }
     */
    public static final OfByte byte12$layout() {
        return byte12$LAYOUT;
    }

    private static final long byte12$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte12
     * }
     */
    public static final long byte12$offset() {
        return byte12$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte12
     * }
     */
    public static byte byte12(MemorySegment struct) {
        return struct.get(byte12$LAYOUT, byte12$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte12
     * }
     */
    public static void byte12(MemorySegment struct, byte fieldValue) {
        struct.set(byte12$LAYOUT, byte12$OFFSET, fieldValue);
    }

    private static final OfByte byte13$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte13"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte13
     * }
     */
    public static final OfByte byte13$layout() {
        return byte13$LAYOUT;
    }

    private static final long byte13$OFFSET = 13;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte13
     * }
     */
    public static final long byte13$offset() {
        return byte13$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte13
     * }
     */
    public static byte byte13(MemorySegment struct) {
        return struct.get(byte13$LAYOUT, byte13$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte13
     * }
     */
    public static void byte13(MemorySegment struct, byte fieldValue) {
        struct.set(byte13$LAYOUT, byte13$OFFSET, fieldValue);
    }

    private static final OfByte byte14$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte14"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte14
     * }
     */
    public static final OfByte byte14$layout() {
        return byte14$LAYOUT;
    }

    private static final long byte14$OFFSET = 14;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte14
     * }
     */
    public static final long byte14$offset() {
        return byte14$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte14
     * }
     */
    public static byte byte14(MemorySegment struct) {
        return struct.get(byte14$LAYOUT, byte14$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte14
     * }
     */
    public static void byte14(MemorySegment struct, byte fieldValue) {
        struct.set(byte14$LAYOUT, byte14$OFFSET, fieldValue);
    }

    private static final OfByte byte15$LAYOUT = (OfByte)$LAYOUT.select(groupElement("byte15"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * UInt8 byte15
     * }
     */
    public static final OfByte byte15$layout() {
        return byte15$LAYOUT;
    }

    private static final long byte15$OFFSET = 15;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * UInt8 byte15
     * }
     */
    public static final long byte15$offset() {
        return byte15$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * UInt8 byte15
     * }
     */
    public static byte byte15(MemorySegment struct) {
        return struct.get(byte15$LAYOUT, byte15$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * UInt8 byte15
     * }
     */
    public static void byte15(MemorySegment struct, byte fieldValue) {
        struct.set(byte15$LAYOUT, byte15$OFFSET, fieldValue);
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

