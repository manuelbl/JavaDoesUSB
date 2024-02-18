// Generated by jextract

package net.codecrete.usb.linux.gen.string;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class string {

    string() {
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

    private static class strerror {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(
            string.C_POINTER,
            string.C_INT
        );

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(
                    string.findOrThrow("strerror"),
                    DESC);
    }

    /**
     * Function descriptor for:
     * {@snippet lang=c :
     * extern char *strerror(int __errnum)
     * }
     */
    public static FunctionDescriptor strerror$descriptor() {
        return strerror.DESC;
    }

    /**
     * Downcall method handle for:
     * {@snippet lang=c :
     * extern char *strerror(int __errnum)
     * }
     */
    public static MethodHandle strerror$handle() {
        return strerror.HANDLE;
    }
    /**
     * {@snippet lang=c :
     * extern char *strerror(int __errnum)
     * }
     */
    public static MemorySegment strerror(int __errnum) {
        var mh$ = strerror.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("strerror", __errnum);
            }
            return (MemorySegment)mh$.invokeExact(__errnum);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }
}

