//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.common.USBStructs;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * WinUSB functions.
 */
public class WinUSB {

    private static final MethodHandle Initialize$Func;
    private static final MethodHandle Free$Func;
    private static final MethodHandle GetDescriptor$Func;
    private static final MethodHandle ControlTransfer$Func;
    private static final MethodHandle WritePipe$Func;
    private static final MethodHandle ReadPipe$Func;

    static {
        var session = MemorySession.openShared();
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.libraryLookup("Winusb", session);

        // BOOL WinUsb_Initialize(
        //  [in]  HANDLE                   DeviceHandle,
        //  [out] PWINUSB_INTERFACE_HANDLE InterfaceHandle
        //);
        Initialize$Func = linker.downcallHandle(
                lookup.lookup("WinUsb_Initialize").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
        );

        // BOOL WinUsb_Free(
        //  [in] WINUSB_INTERFACE_HANDLE InterfaceHandle
        //);
        Free$Func = linker.downcallHandle(
                lookup.lookup("WinUsb_Free").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
        );

        // BOOL WinUsb_GetDescriptor(
        //  [in]  WINUSB_INTERFACE_HANDLE InterfaceHandle,
        //  [in]  UCHAR                   DescriptorType,
        //  [in]  UCHAR                   Index,
        //  [in]  USHORT                  LanguageID,
        //  [out] PUCHAR                  Buffer,
        //  [in]  ULONG                   BufferLength,
        //  [out] PULONG                  LengthTransferred
        //);
        GetDescriptor$Func = linker.downcallHandle(
                lookup.lookup("WinUsb_GetDescriptor").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE, JAVA_BYTE, JAVA_SHORT, ADDRESS, JAVA_INT, ADDRESS)
        );

        // BOOL WinUsb_ControlTransfer(
        //  [in]            WINUSB_INTERFACE_HANDLE InterfaceHandle,
        //  [in]            WINUSB_SETUP_PACKET     SetupPacket,
        //  [out]           PUCHAR                  Buffer,
        //  [in]            ULONG                   BufferLength,
        //  [out, optional] PULONG                  LengthTransferred,
        //  [in, optional]  LPOVERLAPPED            Overlapped
        //);
        ControlTransfer$Func = linker.downcallHandle(
                lookup.lookup("WinUsb_ControlTransfer").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, USBStructs.SetupPacket$Struct, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
        );

        // BOOL WinUsb_WritePipe(
        //  [in]            WINUSB_INTERFACE_HANDLE InterfaceHandle,
        //  [in]            UCHAR                   PipeID,
        //  [in]            PUCHAR                  Buffer,
        //  [in]            ULONG                   BufferLength,
        //  [out, optional] PULONG                  LengthTransferred,
        //  [in, optional]  LPOVERLAPPED            Overlapped
        //);
        WritePipe$Func = linker.downcallHandle(
                lookup.lookup("WinUsb_WritePipe").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
        );

        // BOOL WinUsb_ReadPipe(
        //  [in]            WINUSB_INTERFACE_HANDLE InterfaceHandle,
        //  [in]            UCHAR                   PipeID,
        //  [out]           PUCHAR                  Buffer,
        //  [in]            ULONG                   BufferLength,
        //  [out, optional] PULONG                  LengthTransferred,
        //  [in, optional]  LPOVERLAPPED            Overlapped
        //);
        ReadPipe$Func = linker.downcallHandle(
                lookup.lookup("WinUsb_ReadPipe").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE, ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
        );
    }

    // BOOL WinUsb_Initialize(
    //  [in]  HANDLE                   DeviceHandle,
    //  [out] PWINUSB_INTERFACE_HANDLE InterfaceHandle
    //);
    public static boolean Initialize(Addressable deviceHandle, Addressable interfaceHandleHolder) {
        try {
            return (int) Initialize$Func.invokeExact(deviceHandle, interfaceHandleHolder) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // BOOL WinUsb_Free(
    //  [in] WINUSB_INTERFACE_HANDLE InterfaceHandle
    //);
    public static boolean Free(Addressable interfaceHandle) {
        try {
            return (int) Free$Func.invokeExact(interfaceHandle) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // BOOL WinUsb_GetDescriptor(
    //  [in]  WINUSB_INTERFACE_HANDLE InterfaceHandle,
    //  [in]  UCHAR                   DescriptorType,
    //  [in]  UCHAR                   Index,
    //  [in]  USHORT                  LanguageID,
    //  [out] PUCHAR                  Buffer,
    //  [in]  ULONG                   BufferLength,
    //  [out] PULONG                  LengthTransferred
    //);
    public static boolean GetDescriptor(Addressable interfaceHandle, byte descriptorType, byte index,
                                        short languageID, Addressable buffer, int bufferLength,
                                        Addressable lengthTransferredHolder) {
        try {
            return (int) GetDescriptor$Func.invokeExact(interfaceHandle, descriptorType, index,
                    languageID, buffer, bufferLength, lengthTransferredHolder) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // BOOL WinUsb_ControlTransfer(
    //  [in]            WINUSB_INTERFACE_HANDLE InterfaceHandle,
    //  [in]            WINUSB_SETUP_PACKET     SetupPacket,
    //  [out]           PUCHAR                  Buffer,
    //  [in]            ULONG                   BufferLength,
    //  [out, optional] PULONG                  LengthTransferred,
    //  [in, optional]  LPOVERLAPPED            Overlapped
    //);
    public static boolean ControlTransfer(Addressable interfaceHandle, MemorySegment setupPacket,
                                          Addressable buffer, int bufferLength,
                                          Addressable lengthTransferredHolder, Addressable overlapped) {
        try {
            return (int) ControlTransfer$Func.invokeExact(interfaceHandle, setupPacket,
                    buffer, bufferLength, lengthTransferredHolder, overlapped) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // BOOL WinUsb_WritePipe(
    //  [in]            WINUSB_INTERFACE_HANDLE InterfaceHandle,
    //  [in]            UCHAR                   PipeID,
    //  [in]            PUCHAR                  Buffer,
    //  [in]            ULONG                   BufferLength,
    //  [out, optional] PULONG                  LengthTransferred,
    //  [in, optional]  LPOVERLAPPED            Overlapped
    //);
    public static boolean WritePipe(Addressable interfaceHandle, byte pipeID,
                                    Addressable buffer, int bufferLength,
                                    Addressable lengthTransferredHolder, Addressable overlapped) {
        try {
            return (int) WritePipe$Func.invokeExact(interfaceHandle, pipeID,
                    buffer, bufferLength, lengthTransferredHolder, overlapped) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // BOOL WinUsb_ReadPipe(
    //  [in]            WINUSB_INTERFACE_HANDLE InterfaceHandle,
    //  [in]            UCHAR                   PipeID,
    //  [out]           PUCHAR                  Buffer,
    //  [in]            ULONG                   BufferLength,
    //  [out, optional] PULONG                  LengthTransferred,
    //  [in, optional]  LPOVERLAPPED            Overlapped
    //);
    public static boolean ReadPipe(Addressable interfaceHandle, byte pipeID,
                                   Addressable buffer, int bufferLength,
                                   Addressable lengthTransferredHolder, Addressable overlapped) {
        try {
            return (int) ReadPipe$Func.invokeExact(interfaceHandle, pipeID,
                    buffer, bufferLength, lengthTransferredHolder, overlapped) != 0;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
