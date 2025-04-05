package net.codecrete.usb.windows;

import net.codecrete.usb.usbstandard.SetupPacket;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

@SuppressWarnings({"java:S100", "java:S101", "java:S112", "java:S117"})
public class CustomApis {
    private CustomApis() {
    }

    static {
        System.loadLibrary("KERNEL32");
        System.loadLibrary("WINUSB");
    }

    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();
    private static final Linker LINKER = Linker.nativeLinker();
    private static final Linker.Option LAST_ERROR_STATE = Linker.Option.captureCallState("GetLastError");

    // Custom implementation of WinUsb_ControlTransfer as FFM cannot deal with the
    // WINUSB_SETUP_PACKET being passed by value as it uses unaligned fields.
    // SetupPacket does not use unaligned fields.
    private static class WinUsb_ControlTransfer$IMPL {
        private static final FunctionDescriptor DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS, SetupPacket.LAYOUT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
        private static final MethodHandle HANDLE = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("WinUsb_ControlTransfer"), DESC, LAST_ERROR_STATE);
    }

    public static int WinUsb_ControlTransfer(MemorySegment lastErrorState, MemorySegment InterfaceHandle, MemorySegment SetupPacket, MemorySegment Buffer, int BufferLength, MemorySegment LengthTransferred, MemorySegment Overlapped) {
        try {
            return (int) WinUsb_ControlTransfer$IMPL.HANDLE.invokeExact(lastErrorState, InterfaceHandle, SetupPacket, Buffer, BufferLength, LengthTransferred, Overlapped);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    // CloseHandle implementation without error state
    private static class CloseHandle$IMPL {
        private static final FunctionDescriptor DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        private static final MethodHandle HANDLE = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CloseHandle"), DESC);
    }

    public static int CloseHandle(MemorySegment hObject) {
        try {
            return (int) CloseHandle$IMPL.HANDLE.invokeExact(hObject);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

}
