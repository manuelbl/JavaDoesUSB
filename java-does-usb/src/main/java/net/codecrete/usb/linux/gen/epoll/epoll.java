// Generated by jextract

package net.codecrete.usb.linux.gen.epoll;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

public class epoll {

    epoll() {
        // Should not be called directly
    }

    static final Arena LIBRARY_ARENA = Arena.ofAuto();
    static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("jextract.trace.downcalls");

    static void traceDowncall(String name, Object... args) {
         String traceArgs = Arrays.stream(args)
                       .map(Object::toString)
                       .collect(Collectors.joining(", "));
         System.out.printf("%s(%s)\n", name, traceArgs);
    }

    static MemorySegment findOrThrow(String symbol) {
        return SYMBOL_LOOKUP.find(symbol)
            .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
    }

    static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
        try {
            return MethodHandles.lookup().findVirtual(fi, name, fdesc.toMethodType());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    static MemoryLayout align(MemoryLayout layout, long align) {
        return switch (layout) {
            case PaddingLayout p -> p;
            case ValueLayout v -> v.withByteAlignment(align);
            case GroupLayout g -> {
                MemoryLayout[] alignedMembers = g.memberLayouts().stream()
                        .map(m -> align(m, align)).toArray(MemoryLayout[]::new);
                yield g instanceof StructLayout ?
                        MemoryLayout.structLayout(alignedMembers) : MemoryLayout.unionLayout(alignedMembers);
            }
            case SequenceLayout s -> MemoryLayout.sequenceLayout(s.elementCount(), align(s.elementLayout(), align));
        };
    }

    static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup()
            .or(Linker.nativeLinker().defaultLookup());

    public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
    public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
    public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
    public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
    public static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;
    private static final int EPOLL_CTL_ADD = (int)1L;
    /**
     * {@snippet lang=c :
     * #define EPOLL_CTL_ADD 1
     * }
     */
    public static int EPOLL_CTL_ADD() {
        return EPOLL_CTL_ADD;
    }
    private static final int EPOLL_CTL_DEL = (int)2L;
    /**
     * {@snippet lang=c :
     * #define EPOLL_CTL_DEL 2
     * }
     */
    public static int EPOLL_CTL_DEL() {
        return EPOLL_CTL_DEL;
    }
    private static final int EPOLLIN = (int)1L;
    /**
     * {@snippet lang=c :
     * enum EPOLL_EVENTS.EPOLLIN = 1
     * }
     */
    public static int EPOLLIN() {
        return EPOLLIN;
    }
    private static final int EPOLLOUT = (int)4L;
    /**
     * {@snippet lang=c :
     * enum EPOLL_EVENTS.EPOLLOUT = 4
     * }
     */
    public static int EPOLLOUT() {
        return EPOLLOUT;
    }
    private static final int EPOLLWAKEUP = (int)536870912L;
    /**
     * {@snippet lang=c :
     * enum EPOLL_EVENTS.EPOLLWAKEUP = 536870912
     * }
     */
    public static int EPOLLWAKEUP() {
        return EPOLLWAKEUP;
    }
}

