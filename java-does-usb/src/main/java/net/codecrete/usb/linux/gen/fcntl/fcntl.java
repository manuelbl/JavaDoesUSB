// Generated by jextract

package net.codecrete.usb.linux.gen.fcntl;

import java.lang.foreign.Addressable;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;
public class fcntl  {

    /* package-private */ fcntl() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int O_RDWR() {
        return (int)2L;
    }
    public static MethodHandle open$MH() {
        return RuntimeHelper.requireNonNull(constants$0.open$MH,"open");
    }
    public static int open ( Addressable __file,  int __oflag, Object... x2) {
        var mh$ = open$MH();
        try {
            return (int)mh$.invokeExact(__file, __oflag, x2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static int O_CLOEXEC() {
        return (int)524288L;
    }
}


