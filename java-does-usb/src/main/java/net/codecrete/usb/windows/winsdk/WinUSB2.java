//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.usb.windows.winsdk;

import net.codecrete.usb.usbstandard.SetupPacket;
import net.codecrete.usb.windows.Win;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * Native function calls for WinUSB.
 * <p>
 * This code is manually created to include the additional parameters for capturing
 * {@code GetLastError()} until jextract catches up and can generate the corresponding code.
 * </p>
 */
public class WinUSB2 {
    static {
        System.loadLibrary("Winusb");
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();


    private static final FunctionDescriptor WinUsb_Initialize$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);

    private static final MethodHandle WinUsb_Initialize$MH =
            LINKER.downcallHandle(LOOKUP.find("WinUsb_Initialize").get(), WinUsb_Initialize$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_Initialize(MemorySegment DeviceHandle, MemorySegment InterfaceHandle,
                                        MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_Initialize$MH.invokeExact(lastErrorState, DeviceHandle, InterfaceHandle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_SetCurrentAlternateSetting$FUNC = FunctionDescriptor.of(JAVA_INT,
            ADDRESS, JAVA_BYTE);

    private static final MethodHandle WinUsb_SetCurrentAlternateSetting$MH = LINKER.downcallHandle(LOOKUP.find(
            "WinUsb_SetCurrentAlternateSetting").get(), WinUsb_SetCurrentAlternateSetting$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_SetCurrentAlternateSetting(MemorySegment InterfaceHandle, byte SettingNumber,
                                                        MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_SetCurrentAlternateSetting$MH.invokeExact(lastErrorState, InterfaceHandle,
                    SettingNumber);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_ControlTransfer$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS,
            SetupPacket.LAYOUT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    private static final MethodHandle WinUsb_ControlTransfer$MH = LINKER.downcallHandle(LOOKUP.find(
            "WinUsb_ControlTransfer").get(), WinUsb_ControlTransfer$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_ControlTransfer(MemorySegment InterfaceHandle, MemorySegment SetupPacket,
                                             MemorySegment Buffer, int BufferLength, MemorySegment LengthTransferred,
                                             MemorySegment Overlapped, MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_ControlTransfer$MH.invokeExact(lastErrorState, InterfaceHandle, SetupPacket, Buffer,
                    BufferLength, LengthTransferred, Overlapped);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_SetPipePolicy$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS,
            JAVA_BYTE, JAVA_INT, JAVA_INT, ADDRESS);

    private static final MethodHandle WinUsb_SetPipePolicy$MH = LINKER.downcallHandle(LOOKUP.find(
            "WinUsb_SetPipePolicy").get(), WinUsb_SetPipePolicy$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_SetPipePolicy(MemorySegment InterfaceHandle, byte PipeID, int PolicyType,
                                           int ValueLength, MemorySegment Value, MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_SetPipePolicy$MH.invokeExact(lastErrorState, InterfaceHandle, PipeID, PolicyType,
                    ValueLength, Value);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_WritePipe$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS,
            JAVA_BYTE, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    private static final MethodHandle WinUsb_WritePipe$MH =
            LINKER.downcallHandle(LOOKUP.find("WinUsb_WritePipe").get(), WinUsb_WritePipe$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_WritePipe(MemorySegment InterfaceHandle, byte PipeID, MemorySegment Buffer,
                                       int BufferLength, MemorySegment LengthTransferred, MemorySegment Overlapped,
                                       MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_WritePipe$MH.invokeExact(lastErrorState, InterfaceHandle, PipeID, Buffer,
                    BufferLength, LengthTransferred, Overlapped);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_ReadPipe$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE
            , ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    private static final MethodHandle WinUsb_ReadPipe$MH = LINKER.downcallHandle(LOOKUP.find("WinUsb_ReadPipe").get()
            , WinUsb_ReadPipe$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_ReadPipe(MemorySegment InterfaceHandle, byte PipeID, MemorySegment Buffer,
                                      int BufferLength, MemorySegment LengthTransferred, MemorySegment Overlapped,
                                      MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_ReadPipe$MH.invokeExact(lastErrorState, InterfaceHandle, PipeID, Buffer, BufferLength
                    , LengthTransferred, Overlapped);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_ResetPipe$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE);

    private static final MethodHandle WinUsb_ResetPipe$MH =
            LINKER.downcallHandle(LOOKUP.find("WinUsb_ResetPipe").get(), WinUsb_ResetPipe$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_ResetPipe(MemorySegment InterfaceHandle, byte PipeID, MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_ResetPipe$MH.invokeExact(lastErrorState, InterfaceHandle, PipeID);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final FunctionDescriptor WinUsb_AbortPipe$FUNC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE);

    private static final MethodHandle WinUsb_AbortPipe$MH =
            LINKER.downcallHandle(LOOKUP.find("WinUsb_AbortPipe").get(), WinUsb_ResetPipe$FUNC, Win.LAST_ERROR_STATE);

    public static int WinUsb_AbortPipe(MemorySegment InterfaceHandle, byte PipeID, MemorySegment lastErrorState) {
        try {
            return (int) WinUsb_AbortPipe$MH.invokeExact(lastErrorState, InterfaceHandle, PipeID);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
