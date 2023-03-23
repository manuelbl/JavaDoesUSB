//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.ForeignMemory;
import net.codecrete.usb.macos.gen.iokit.IOUSBDeviceInterface100;
import net.codecrete.usb.macos.gen.iokit.IOUSBInterfaceInterface190;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;

import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * Helper functions to call the virtual methods of IOKit USB interfaces.
 */
public class IoKitUSB {

    private static MemorySegment getVtable(MemorySegment self) {
        var object = MemorySegment.ofAddress(self.address(), ADDRESS.byteSize(), SegmentScope.global());
        // 800: size of biggest vtable and then some
        return ForeignMemory.deref(object, 800);
    }

    // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv);
    public static int QueryInterface(MemorySegment self, MemorySegment iid, MemorySegment ppv) {
        return IOUSBDeviceInterface100.QueryInterface(getVtable(self), SegmentScope.global()).apply(self, iid, ppv);
    }

    // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer);
    public static int AddRef(MemorySegment self) {
        return IOUSBDeviceInterface100.AddRef(getVtable(self), SegmentScope.global()).apply(self);
    }

    // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer)
    public static int Release(MemorySegment self) {
        return IOUSBDeviceInterface100.Release(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (* CreateDeviceAsyncEventSource)(void* self, CFRunLoopSourceRef* source);
    public static int CreateDeviceAsyncEventSource(MemorySegment self, MemorySegment source) {
        return IOUSBDeviceInterface100.CreateDeviceAsyncEventSource(getVtable(self), SegmentScope.global()).apply(self, source);
    }

    // CFRunLoopSourceRef (* GetDeviceAsyncEventSource)(void* self);
    public static MemorySegment GetDeviceAsyncEventSource(MemorySegment self) {
        return IOUSBDeviceInterface100.GetDeviceAsyncEventSource(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (*USBDeviceOpen)(void *self);
    public static int USBDeviceOpen(MemorySegment self) {
        return IOUSBDeviceInterface100.USBDeviceOpen(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (*USBDeviceClose)(void *self);
    public static int USBDeviceClose(MemorySegment self) {
        return IOUSBDeviceInterface100.USBDeviceClose(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (*GetConfigurationDescriptorPtr)(void *self, UInt8 configIndex, IOUSBConfigurationDescriptorPtr *desc);
    public static int GetConfigurationDescriptorPtr(MemorySegment self, byte configIndex, MemorySegment descHolder) {
        return IOUSBDeviceInterface100.GetConfigurationDescriptorPtr(getVtable(self), SegmentScope.global()).apply(self, configIndex, descHolder);
    }

    // IOReturn (*SetConfiguration)(void *self, UInt8 configNum);
    public static int SetConfiguration(MemorySegment self, byte configValue) {
        return IOUSBDeviceInterface100.SetConfiguration(getVtable(self), SegmentScope.global()).apply(self,
                configValue);
    }

    // IOReturn (*CreateInterfaceIterator)(void *self, IOUSBFindInterfaceRequest *req, io_iterator_t *iter);
    public static int CreateInterfaceIterator(MemorySegment self, MemorySegment req, MemorySegment iter) {
        return IOUSBDeviceInterface100.CreateInterfaceIterator(getVtable(self), SegmentScope.global()).apply(self,
                req, iter);
    }

    // IOReturn (* DeviceRequestAsync)(void* self, IOUSBDevRequest* req, IOAsyncCallback1 callback, void* refCon);
    public static int DeviceRequestAsync(MemorySegment self, MemorySegment deviceRequest, MemorySegment callback,
                                         MemorySegment refCon) {
        return IOUSBDeviceInterface100.DeviceRequestAsync(getVtable(self), SegmentScope.global()).apply(self,
                deviceRequest, callback, refCon);
    }

    // IOReturn (*USBInterfaceOpen)(void *self);;
    public static int USBInterfaceOpen(MemorySegment self) {
        return IOUSBInterfaceInterface190.USBInterfaceOpen(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (*USBInterfaceClose)(void *self);;
    public static int USBInterfaceClose(MemorySegment self) {
        return IOUSBInterfaceInterface190.USBInterfaceClose(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (*GetInterfaceNumber)(void *self, UInt8 *intfNumber);
    public static int GetInterfaceNumber(MemorySegment self, MemorySegment intfNumberHolder) {
        return IOUSBInterfaceInterface190.GetInterfaceNumber(getVtable(self), SegmentScope.global()).apply(self,
                intfNumberHolder);
    }

    // IOReturn (*GetNumEndpoints)(void *self, UInt8 *intfNumEndpoints);
    public static int GetNumEndpoints(MemorySegment self, MemorySegment intfNumEndpointsHolder) {
        return IOUSBInterfaceInterface190.GetNumEndpoints(getVtable(self), SegmentScope.global()).apply(self,
                intfNumEndpointsHolder);
    }

    // IOReturn (*GetPipeProperties)(void *self, UInt8 pipeRef, UInt8 *direction, UInt8 *number, UInt8 *transferType,
    // UInt16 *maxPacketSize, UInt8 *interval);
    public static int GetPipeProperties(MemorySegment self, byte pipeRef, MemorySegment directionHolder,
                                        MemorySegment numberHolder, MemorySegment transferTypeHolder,
                                        MemorySegment maxPacketSizeHolder, MemorySegment intervalHolder) {
        return IOUSBInterfaceInterface190.GetPipeProperties(getVtable(self), SegmentScope.global()).apply(self,
                pipeRef, directionHolder, numberHolder, transferTypeHolder, maxPacketSizeHolder, intervalHolder);
    }

    // IOReturn (*ReadPipeAsync)(void *self, UInt8 pipeRef, void *buf, UInt32 size, IOAsyncCallback1 callback, void
    // *refcon);
    public static int ReadPipeAsync(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                                    MemorySegment callback, MemorySegment refcon) {
        return IOUSBInterfaceInterface190.ReadPipeAsync(getVtable(self), SegmentScope.global()).apply(self, pipeRef,
                buf, size, callback, refcon);
    }

    // IOReturn (*ReadPipeAsyncTO)(void *self, UInt8 pipeRef, void *buf, UInt32 size, UInt32 noDataTimeout, UInt32
    // completionTimeout, IOAsyncCallback1 callback, void *refcon);
    public static int ReadPipeAsyncTO(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                                      int noDataTimeout, int completionTimeout, MemorySegment callback,
                                      MemorySegment refcon) {
        return IOUSBInterfaceInterface190.ReadPipeAsyncTO(getVtable(self), SegmentScope.global()).apply(self, pipeRef
                , buf, size, noDataTimeout, completionTimeout, callback, refcon);
    }

    // IOReturn (*WritePipeAsync)(vovoid *self, UInt8 pipeRef, void *buf, UInt32 size, IOAsyncCallback1 callback,
    // void *refcon);
    public static int WritePipeAsync(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                                     MemorySegment callback, MemorySegment refcon) {
        return IOUSBInterfaceInterface190.WritePipeAsync(getVtable(self), SegmentScope.global()).apply(self, pipeRef,
                buf, size, callback, refcon);
    }

    // IOReturn (*WritePipeAsyncTO)(void *self, UInt8 pipeRef, void *buf, UInt32 size, UInt32 noDataTimeout, UInt32
    // completionTimeout, IOAsyncCallback1 callback, void *refcon);
    public static int WritePipeAsyncTO(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                                       int noDataTimeout, int completionTimeout, MemorySegment callback,
                                       MemorySegment refcon) {
        return IOUSBInterfaceInterface190.WritePipeAsyncTO(getVtable(self), SegmentScope.global()).apply(self,
                pipeRef, buf, size, noDataTimeout, completionTimeout, callback, refcon);
    }

    // IOReturn (* AbortPipe)(void* self, UInt8 pipeRef);
    public static int AbortPipe(MemorySegment self, byte pipeRef) {
        return IOUSBInterfaceInterface190.AbortPipe(getVtable(self), SegmentScope.global()).apply(self, pipeRef);
    }

    // IOReturn (*SetAlternateInterface)(void *self, UInt8 alternateSetting);
    public static int SetAlternateInterface(MemorySegment self, byte alternateSetting) {
        return IOUSBInterfaceInterface190.SetAlternateInterface(getVtable(self), SegmentScope.global()).apply(self,
                alternateSetting);
    }

    // IOReturn (* ClearPipeStallBothEnds)(void* self, UInt8 pipeRef);
    public static int ClearPipeStallBothEnds(MemorySegment self, byte pipeRef) {
        return IOUSBInterfaceInterface190.ClearPipeStallBothEnds(getVtable(self), SegmentScope.global()).apply(self,
                pipeRef);
    }

    // CFRunLoopSourceRef (*GetInterfaceAsyncEventSource)(void* self);
    public static MemorySegment GetInterfaceAsyncEventSource(MemorySegment self) {
        return IOUSBInterfaceInterface190.GetInterfaceAsyncEventSource(getVtable(self), SegmentScope.global()).apply(self);
    }

    // IOReturn (*CreateInterfaceAsyncEventSource)(void *self, CFRunLoopSourceRef *source);
    public static int CreateInterfaceAsyncEventSource(MemorySegment self, MemorySegment source) {
        return IOUSBInterfaceInterface190.CreateInterfaceAsyncEventSource(getVtable(self), SegmentScope.global()).apply(self, source);
    }
}
