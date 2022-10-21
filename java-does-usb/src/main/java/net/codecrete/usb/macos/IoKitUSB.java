//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.iokit.IOUSBDeviceInterface;
import net.codecrete.usb.macos.gen.iokit.IOUSBInterfaceInterface;
import net.codecrete.usb.macos.gen.iokit.IOUSBInterfaceStruct942;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * Helper functions to call the virtual methods of IOKit USB interfaces.
 */
public class IoKitUSB {

    private static MemorySegment getVtable(MemoryAddress self, MemorySession session) {
        var object = MemorySegment.ofAddress(self, ADDRESS.byteSize(), session);
        var vtableAddr = object.get(ADDRESS, 0);
        // 800: size of biggest vtable and then some
        return MemorySegment.ofAddress(vtableAddr, 800, session);
    }

    // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv);
    public static int QueryInterface(MemoryAddress self, MemorySegment iid, MemoryAddress ppv) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.QueryInterface(getVtable(self, session), session).apply(self, iid, ppv);
        }
    }

    // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer);
    public static int AddRef(MemoryAddress self) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.AddRef(getVtable(self, session), session).apply(self);
        }
    }

    // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer)
    public static int Release(MemoryAddress self) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.Release(getVtable(self, session), session).apply(self);
        }
    }

    // IOReturn (*USBDeviceOpen)(void *self);
    public static int USBDeviceOpen(MemoryAddress self) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.USBDeviceOpen(getVtable(self, session), session).apply(self);
        }
    }

    // IOReturn (*USBDeviceClose)(void *self);
    public static int USBDeviceClose(MemoryAddress self) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.USBDeviceClose(getVtable(self, session), session).apply(self);
        }
    }

    // IOReturn (*GetConfigurationDescriptorPtr)(void *self, UInt8 configIndex, IOUSBConfigurationDescriptorPtr *desc);
    public static int GetConfigurationDescriptorPtr(MemoryAddress self, byte configIndex,
                                                    MemoryAddress descHolder) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.GetConfigurationDescriptorPtr(getVtable(self, session), session).apply(self, configIndex, descHolder);
        }
    }

    // IOReturn (*SetConfiguration)(void *self, UInt8 configNum);
    public static int SetConfiguration(MemoryAddress self, byte configValue) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.SetConfiguration(getVtable(self, session), session).apply(self, configValue);
        }
    }

    // IOReturn (*CreateInterfaceIterator)(void *self, IOUSBFindInterfaceRequest *req, io_iterator_t *iter);
    public static int CreateInterfaceIterator(MemoryAddress self, MemoryAddress req, MemoryAddress iter) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.CreateInterfaceIterator(getVtable(self, session), session).apply(self, req, iter);
        }
    }

    // IOReturn (*DeviceRequest)(void *self, IOUSBDevRequest *req);
    public static int DeviceRequest(MemoryAddress self, MemoryAddress deviceRequest) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBDeviceInterface.DeviceRequest(getVtable(self, session), session).apply(self, deviceRequest);
        }
    }

    // IOReturn (*USBInterfaceOpen)(void *self);;
    public static int USBInterfaceOpen(MemoryAddress self) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.USBInterfaceOpen(getVtable(self, session), session).apply(self);
        }
    }

    // IOReturn (*USBInterfaceClose)(void *self);;
    public static int USBInterfaceClose(MemoryAddress self) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.USBInterfaceClose(getVtable(self, session), session).apply(self);
        }
    }

    // IOReturn (*GetInterfaceNumber)(void *self, UInt8 *intfNumber);
    public static int GetInterfaceNumber(MemoryAddress self, MemoryAddress intfNumberHolder) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.GetInterfaceNumber(getVtable(self, session), session).apply(self, intfNumberHolder);
        }
    }

    // IOReturn (*GetNumEndpoints)(void *self, UInt8 *intfNumEndpoints);
    public static int GetNumEndpoints(MemoryAddress self, MemoryAddress intfNumEndpointsHolder) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.GetNumEndpoints(getVtable(self, session), session).apply(self, intfNumEndpointsHolder);
        }
    }

    // IOReturn (*GetPipeProperties)(void *self, UInt8 pipeRef, UInt8 *direction, UInt8 *number, UInt8 *transferType,
    // UInt16 *maxPacketSize, UInt8 *interval);
    public static int GetPipeProperties(MemoryAddress self, byte pipeRef, MemoryAddress directionHolder,
                                        MemoryAddress numberHolder, MemoryAddress transferTypeHolder,
                                        MemoryAddress maxPacketSizeHolder, MemoryAddress intervalHolder) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.GetPipeProperties(getVtable(self, session), session).apply(self, pipeRef,
                    directionHolder, numberHolder, transferTypeHolder, maxPacketSizeHolder, intervalHolder);
        }
    }

    // IOReturn (*ReadPipe)(void *self, UInt8 pipeRef, void *buf, UInt32 *size);
    public static int ReadPipe(MemoryAddress self, byte pipeRef, MemoryAddress buf, MemoryAddress sizeHolder) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.ReadPipe(getVtable(self, session), session)
                    .apply(self, pipeRef, buf, sizeHolder);
        }
    }

    // IOReturn (* ReadPipeTO)(void* self, UInt8 pipeRef, void* buf, UInt32* size, UInt32 noDataTimeout, UInt32 completionTimeout);
    public static int ReadPipeTO(MemoryAddress self, byte pipeRef, MemoryAddress buf, MemoryAddress sizeHolder, int noDataTimeout, int completionTimeout) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.ReadPipeTO(getVtable(self, session), session)
                    .apply(self, pipeRef, buf, sizeHolder, noDataTimeout, completionTimeout);
        }
    }

    // IOReturn (*WritePipe)(void *self, UInt8 pipeRef, void *buf, UInt32 size);
    public static int WritePipe(MemoryAddress self, byte pipeRef, MemoryAddress buf, int size) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.WritePipe(getVtable(self, session), session).apply(self, pipeRef, buf, size);
        }
    }

    // IOReturn (* WritePipeTO)(void* self, UInt8 pipeRef, void* buf, UInt32 size, UInt32 noDataTimeout, UInt32 completionTimeout);
    public static int WritePipeTO(MemoryAddress self, byte pipeRef, MemoryAddress buf, int size, int noDataTimeout, int completionTimeout) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.WritePipeTO(getVtable(self, session), session).apply(self, pipeRef, buf, size, noDataTimeout, completionTimeout);
        }
    }

    // IOReturn (* AbortPipe)(void* self, UInt8 pipeRef);
    public static int AbortPipe(MemoryAddress self, byte pipeRef) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.AbortPipe(getVtable(self, session), session).apply(self, pipeRef);
        }
    }

    // IOReturn (*SetAlternateInterface)(void *self, UInt8 alternateSetting);
    public static int SetAlternateInterface(MemoryAddress self, byte alternateSetting) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceInterface.SetAlternateInterface(getVtable(self, session), session).apply(self, alternateSetting);
        }
    }

    // IOReturn (* ClearPipeStallBothEnds)(void* self, UInt8 pipeRef);
    public static int ClearPipeStallBothEnds(MemoryAddress self, byte pipeRef) {
        try (var session = MemorySession.openConfined()) {
            return IOUSBInterfaceStruct942.ClearPipeStallBothEnds(getVtable(self, session), session).apply(self, pipeRef);
        }
    }
}
