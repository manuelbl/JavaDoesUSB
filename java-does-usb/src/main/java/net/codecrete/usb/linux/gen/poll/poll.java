// Generated by jextract

package net.codecrete.usb.linux.gen.poll;

import java.lang.foreign.Addressable;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;
public class poll  {

    /* package-private */ poll() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int POLLIN() {
        return (int)1L;
    }
    public static MethodHandle poll$MH() {
        return RuntimeHelper.requireNonNull(constants$0.poll$MH,"poll");
    }
    public static int poll ( Addressable __fds,  long __nfds,  int __timeout) {
        var mh$ = poll$MH();
        try {
            return (int)mh$.invokeExact(__fds, __nfds, __timeout);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}

