//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.iokit.IOUSBDeviceStruct187;
import net.codecrete.usb.macos.gen.iokit.IOUSBInterfaceStruct190;

import java.lang.foreign.MemorySegment;

import static net.codecrete.usb.macos.IoKitHelper.getVtable;

/**
 * Helper functions to call the virtual methods of IOKit USB interfaces.
 */
@SuppressWarnings({"java:S100", "java:S107", "UnusedReturnValue", "SameParameterValue"})
class IoKitUsb {

    private IoKitUsb() {
    }

    // HRESULT (STDMETHODCALLTYPE *QueryInterface)(void *thisPointer, REFIID iid, LPVOID *ppv)
    static int QueryInterface(MemorySegment self, MemorySegment iid, MemorySegment ppv) {
        return IOUSBDeviceStruct187.QueryInterface.invoke(IOUSBDeviceStruct187.QueryInterface(getVtable(self)), self, iid, ppv);
    }

    // ULONG (STDMETHODCALLTYPE *AddRef)(void *thisPointer)
    static int AddRef(MemorySegment self) {
        return IOUSBDeviceStruct187.AddRef.invoke(IOUSBDeviceStruct187.AddRef(getVtable(self)), self);
    }

    // ULONG (STDMETHODCALLTYPE *Release)(void *thisPointer)
    static int Release(MemorySegment self) {
        return IOUSBDeviceStruct187.Release.invoke(IOUSBDeviceStruct187.Release(getVtable(self)), self);
    }

    // IOReturn (* CreateDeviceAsyncEventSource)(void* self, CFRunLoopSourceRef* source)
    static int CreateDeviceAsyncEventSource(MemorySegment self, MemorySegment source) {
        return IOUSBDeviceStruct187.CreateDeviceAsyncEventSource.invoke(IOUSBDeviceStruct187.CreateDeviceAsyncEventSource(getVtable(self)), self,
                source);
    }

    // CFRunLoopSourceRef (* GetDeviceAsyncEventSource)(void* self)
    static MemorySegment GetDeviceAsyncEventSource(MemorySegment self) {
        return IOUSBDeviceStruct187.GetDeviceAsyncEventSource.invoke(IOUSBDeviceStruct187.GetDeviceAsyncEventSource(getVtable(self)), self);
    }

    // IOReturn (*USBDeviceOpenSeize)(void *self)
    static int USBDeviceOpenSeize(MemorySegment self) {
        return IOUSBDeviceStruct187.USBDeviceOpenSeize.invoke(IOUSBDeviceStruct187.USBDeviceOpenSeize(getVtable(self)), self);
    }

    // IOReturn (*USBDeviceClose)(void *self)
    static int USBDeviceClose(MemorySegment self) {
        return IOUSBDeviceStruct187.USBDeviceClose.invoke(IOUSBDeviceStruct187.USBDeviceClose(getVtable(self)), self);
    }

    // IOReturn (* USBDeviceReEnumerate)(void* self, UInt32 options)
    static int USBDeviceReEnumerate(MemorySegment self, int options) {
        return IOUSBDeviceStruct187.USBDeviceReEnumerate.invoke(IOUSBDeviceStruct187.USBDeviceReEnumerate(getVtable(self)), self, options);
    }

    // IOReturn (*GetConfigurationDescriptorPtr)(void *self, UInt8 configIndex, IOUSBConfigurationDescriptorPtr *desc)
    static int GetConfigurationDescriptorPtr(MemorySegment self, byte configIndex, MemorySegment descHolder) {
        return IOUSBDeviceStruct187.GetConfigurationDescriptorPtr.invoke(IOUSBDeviceStruct187.GetConfigurationDescriptorPtr(getVtable(self)), self,
                configIndex, descHolder);
    }

    // IOReturn (*SetConfiguration)(void *self, UInt8 configNum)
    static int SetConfiguration(MemorySegment self, byte configValue) {
        return IOUSBDeviceStruct187.SetConfiguration.invoke(IOUSBDeviceStruct187.SetConfiguration(getVtable(self)), self, configValue);
    }

    // IOReturn (*CreateInterfaceIterator)(void *self, IOUSBFindInterfaceRequest *req, io_iterator_t *iter)
    static int CreateInterfaceIterator(MemorySegment self, MemorySegment req, MemorySegment iter) {
        return IOUSBDeviceStruct187.CreateInterfaceIterator.invoke(IOUSBDeviceStruct187.CreateInterfaceIterator(getVtable(self)), self, req, iter);
    }

    // IOReturn (* DeviceRequest)(void* self, IOUSBDevRequest* req)
    static int DeviceRequest(MemorySegment self, MemorySegment deviceRequest) {
        return IOUSBDeviceStruct187.DeviceRequest.invoke(IOUSBDeviceStruct187.DeviceRequest(getVtable(self)), self, deviceRequest);
    }

    // IOReturn (* DeviceRequestAsync)(void* self, IOUSBDevRequest* req, IOAsyncCallback1 callback, void* refCon)
    static int DeviceRequestAsync(MemorySegment self, MemorySegment deviceRequest, MemorySegment callback,
                                  MemorySegment refCon) {
        return IOUSBDeviceStruct187.DeviceRequestAsync.invoke(IOUSBDeviceStruct187.DeviceRequestAsync(getVtable(self)), self, deviceRequest,
                callback, refCon);
    }

    // IOReturn (*USBInterfaceOpen)(void *self)
    static int USBInterfaceOpen(MemorySegment self) {
        return IOUSBInterfaceStruct190.USBInterfaceOpen.invoke(IOUSBInterfaceStruct190.USBInterfaceOpen(getVtable(self)), self);
    }

    // IOReturn (*USBInterfaceClose)(void *self)
    static int USBInterfaceClose(MemorySegment self) {
        return IOUSBInterfaceStruct190.USBInterfaceClose.invoke(IOUSBInterfaceStruct190.USBInterfaceClose(getVtable(self)), self);
    }

    // IOReturn (*GetInterfaceNumber)(void *self, UInt8 *intfNumber)
    static int GetInterfaceNumber(MemorySegment self, MemorySegment intfNumberHolder) {
        return IOUSBInterfaceStruct190.GetInterfaceNumber.invoke(IOUSBInterfaceStruct190.GetInterfaceNumber(getVtable(self)), self,
                intfNumberHolder);
    }

    // IOReturn (*GetNumEndpoints)(void *self, UInt8 *intfNumEndpoints)
    static int GetNumEndpoints(MemorySegment self, MemorySegment intfNumEndpointsHolder) {
        return IOUSBInterfaceStruct190.GetNumEndpoints.invoke(IOUSBInterfaceStruct190.GetNumEndpoints(getVtable(self)), self,
                intfNumEndpointsHolder);
    }

    // IOReturn (*GetPipeProperties)(void *self, UInt8 pipeRef, UInt8 *direction, UInt8 *number, UInt8 *transferType,
    // UInt16 *maxPacketSize, UInt8 *interval)
    static int GetPipeProperties(MemorySegment self, byte pipeRef, MemorySegment directionHolder,
                                 MemorySegment numberHolder, MemorySegment transferTypeHolder,
                                 MemorySegment maxPacketSizeHolder, MemorySegment intervalHolder) {
        return IOUSBInterfaceStruct190.GetPipeProperties.invoke(IOUSBInterfaceStruct190.GetPipeProperties(getVtable(self)), self, pipeRef,
                directionHolder, numberHolder, transferTypeHolder, maxPacketSizeHolder, intervalHolder);
    }

    // IOReturn (*ReadPipeAsync)(void *self, UInt8 pipeRef, void *buf, UInt32 size, IOAsyncCallback1 callback, void
    // *refcon)
    static int ReadPipeAsync(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                             MemorySegment callback, MemorySegment refcon) {
        return IOUSBInterfaceStruct190.ReadPipeAsync.invoke(IOUSBInterfaceStruct190.ReadPipeAsync(getVtable(self)), self, pipeRef, buf,
                size, callback, refcon);
    }

    // IOReturn (*ReadPipeAsyncTO)(void *self, UInt8 pipeRef, void *buf, UInt32 size, UInt32 noDataTimeout, UInt32
    // completionTimeout, IOAsyncCallback1 callback, void *refcon)
    static int ReadPipeAsyncTO(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                               int noDataTimeout, int completionTimeout, MemorySegment callback,
                               MemorySegment refcon) {
        return IOUSBInterfaceStruct190.ReadPipeAsyncTO.invoke(IOUSBInterfaceStruct190.ReadPipeAsyncTO(getVtable(self)), self, pipeRef, buf,
                size, noDataTimeout, completionTimeout, callback, refcon);
    }

    // IOReturn (*WritePipeAsync)(vovoid *self, UInt8 pipeRef, void *buf, UInt32 size, IOAsyncCallback1 callback,
    // void *refcon)
    static int WritePipeAsync(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                              MemorySegment callback, MemorySegment refcon) {
        return IOUSBInterfaceStruct190.WritePipeAsync.invoke(IOUSBInterfaceStruct190.WritePipeAsync(getVtable(self)), self, pipeRef, buf,
                size, callback, refcon);
    }

    // IOReturn (*WritePipeAsyncTO)(void *self, UInt8 pipeRef, void *buf, UInt32 size, UInt32 noDataTimeout, UInt32
    // completionTimeout, IOAsyncCallback1 callback, void *refcon)
    static int WritePipeAsyncTO(MemorySegment self, byte pipeRef, MemorySegment buf, int size,
                                int noDataTimeout, int completionTimeout, MemorySegment callback,
                                MemorySegment refcon) {
        return IOUSBInterfaceStruct190.WritePipeAsyncTO.invoke(IOUSBInterfaceStruct190.WritePipeAsyncTO(getVtable(self)), self, pipeRef, buf,
                size, noDataTimeout, completionTimeout, callback, refcon);
    }

    // IOReturn (* AbortPipe)(void* self, UInt8 pipeRef)
    static int AbortPipe(MemorySegment self, byte pipeRef) {
        return IOUSBInterfaceStruct190.AbortPipe.invoke(IOUSBInterfaceStruct190.AbortPipe(getVtable(self)), self, pipeRef);
    }

    // IOReturn (*SetAlternateInterface)(void *self, UInt8 alternateSetting)
    static int SetAlternateInterface(MemorySegment self, byte alternateSetting) {
        return IOUSBInterfaceStruct190.SetAlternateInterface.invoke(IOUSBInterfaceStruct190.SetAlternateInterface(getVtable(self)), self,
                alternateSetting);
    }

    // IOReturn (* ClearPipeStallBothEnds)(void* self, UInt8 pipeRef)
    static int ClearPipeStallBothEnds(MemorySegment self, byte pipeRef) {
        return IOUSBInterfaceStruct190.ClearPipeStallBothEnds.invoke(IOUSBInterfaceStruct190.ClearPipeStallBothEnds(getVtable(self)), self, pipeRef);
    }

    // CFRunLoopSourceRef (*GetInterfaceAsyncEventSource)(void* self)
    static MemorySegment GetInterfaceAsyncEventSource(MemorySegment self) {
        return IOUSBInterfaceStruct190.GetInterfaceAsyncEventSource.invoke(IOUSBInterfaceStruct190.GetInterfaceAsyncEventSource(getVtable(self)), self);
    }

    // IOReturn (*CreateInterfaceAsyncEventSource)(void *self, CFRunLoopSourceRef *source)
    static int CreateInterfaceAsyncEventSource(MemorySegment self, MemorySegment source) {
        return IOUSBInterfaceStruct190.CreateInterfaceAsyncEventSource.invoke(IOUSBInterfaceStruct190.CreateInterfaceAsyncEventSource(getVtable(self)), self
                , source);
    }
}
