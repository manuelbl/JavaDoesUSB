// Generated by jextract

package net.codecrete.usb.linux.gen.errno;

import java.lang.foreign.MemoryAddress;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;
public class errno  {

    /* package-private */ errno() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int EPIPE() {
        return (int)32L;
    }
    public static int ETIMEDOUT() {
        return (int)110L;
    }
    public static MethodHandle __errno_location$MH() {
        return RuntimeHelper.requireNonNull(constants$0.__errno_location$MH,"__errno_location");
    }
    public static MemoryAddress __errno_location () {
        var mh$ = __errno_location$MH();
        try {
            return (java.lang.foreign.MemoryAddress)mh$.invokeExact();
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}


