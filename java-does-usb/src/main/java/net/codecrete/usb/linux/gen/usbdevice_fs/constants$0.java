// Generated by jextract

package net.codecrete.usb.linux.gen.usbdevice_fs;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
class constants$0 {

    static final FunctionDescriptor __kernel_sighandler_t$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle __kernel_sighandler_t$MH = RuntimeHelper.downcallHandle(
        constants$0.__kernel_sighandler_t$FUNC
    );
    static final MemorySegment REISERFS_SUPER_MAGIC_STRING$SEGMENT = RuntimeHelper.CONSTANT_ALLOCATOR.allocateUtf8String("ReIsErFs");
    static final MemorySegment REISER2FS_SUPER_MAGIC_STRING$SEGMENT = RuntimeHelper.CONSTANT_ALLOCATOR.allocateUtf8String("ReIsEr2Fs");
    static final MemorySegment REISER2FS_JR_SUPER_MAGIC_STRING$SEGMENT = RuntimeHelper.CONSTANT_ALLOCATOR.allocateUtf8String("ReIsEr3Fs");
}


