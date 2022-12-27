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
        return IOUSBDeviceInterface100.SetConfiguration(getVtable(self), SegmentScope.global()).apply(self, configValue);
    }

    // IOReturn (*CreateInterfaceIterator)(void *self, IOUSBFindInterfaceRequest *req, io_iterator_t *iter);
    public static int CreateInterfaceIterator(MemorySegment self, MemorySegment req, MemorySegment iter) {
        return IOUSBDeviceInterface100.CreateInterfaceIterator(getVtable(self), SegmentScope.global()).apply(self, req, iter);
    }

    // IOReturn (*DeviceRequest)(void *self, IOUSBDevRequest *req);
    public static int DeviceRequest(MemorySegment self, MemorySegment deviceRequest) {
        return IOUSBDeviceInterface100.DeviceRequest(getVtable(self), SegmentScope.global()).apply(self, deviceRequest);
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
        return IOUSBInterfaceInterface190.GetInterfaceNumber(getVtable(self), SegmentScope.global()).apply(self, intfNumberHolder);
    }

    // IOReturn (*GetNumEndpoints)(void *self, UInt8 *intfNumEndpoints);
    public static int GetNumEndpoints(MemorySegment self, MemorySegment intfNumEndpointsHolder) {
        return IOUSBInterfaceInterface190.GetNumEndpoints(getVtable(self), SegmentScope.global()).apply(self, intfNumEndpointsHolder);
    }

    // IOReturn (*GetPipeProperties)(void *self, UInt8 pipeRef, UInt8 *direction, UInt8 *number, UInt8 *transferType,
    // UInt16 *maxPacketSize, UInt8 *interval);
    public static int GetPipeProperties(MemorySegment self, byte pipeRef, MemorySegment directionHolder,
                                        MemorySegment numberHolder, MemorySegment transferTypeHolder,
                                        MemorySegment maxPacketSizeHolder, MemorySegment intervalHolder) {
        return IOUSBInterfaceInterface190.GetPipeProperties(getVtable(self), SegmentScope.global()).apply(self, pipeRef,
                directionHolder, numberHolder, transferTypeHolder, maxPacketSizeHolder, intervalHolder);
    }

    // IOReturn (*ReadPipe)(void *self, UInt8 pipeRef, void *buf, UInt32 *size);
    public static int ReadPipe(MemorySegment self, byte pipeRef, MemorySegment buf, MemorySegment sizeHolder) {
        return IOUSBInterfaceInterface190.ReadPipe(getVtable(self), SegmentScope.global())
                .apply(self, pipeRef, buf, sizeHolder);
    }

    // IOReturn (* ReadPipeTO)(void* self, UInt8 pipeRef, void* buf, UInt32* size, UInt32 noDataTimeout, UInt32 completionTimeout);
    public static int ReadPipeTO(MemorySegment self, byte pipeRef, MemorySegment buf, MemorySegment sizeHolder, int noDataTimeout, int completionTimeout) {
        return IOUSBInterfaceInterface190.ReadPipeTO(getVtable(self), SegmentScope.global())
                .apply(self, pipeRef, buf, sizeHolder, noDataTimeout, completionTimeout);
    }

    // IOReturn (*WritePipe)(void *self, UInt8 pipeRef, void *buf, UInt32 size);
    public static int WritePipe(MemorySegment self, byte pipeRef, MemorySegment buf, int size) {
        return IOUSBInterfaceInterface190.WritePipe(getVtable(self), SegmentScope.global()).apply(self, pipeRef, buf, size);
    }

    // IOReturn (* WritePipeTO)(void* self, UInt8 pipeRef, void* buf, UInt32 size, UInt32 noDataTimeout, UInt32 completionTimeout);
    public static int WritePipeTO(MemorySegment self, byte pipeRef, MemorySegment buf, int size, int noDataTimeout, int completionTimeout) {
        return IOUSBInterfaceInterface190.WritePipeTO(getVtable(self), SegmentScope.global()).apply(self, pipeRef, buf, size, noDataTimeout, completionTimeout);
    }

    // IOReturn (* AbortPipe)(void* self, UInt8 pipeRef);
    public static int AbortPipe(MemorySegment self, byte pipeRef) {
        return IOUSBInterfaceInterface190.AbortPipe(getVtable(self), SegmentScope.global()).apply(self, pipeRef);
    }

    // IOReturn (*SetAlternateInterface)(void *self, UInt8 alternateSetting);
    public static int SetAlternateInterface(MemorySegment self, byte alternateSetting) {
        return IOUSBInterfaceInterface190.SetAlternateInterface(getVtable(self), SegmentScope.global()).apply(self, alternateSetting);
    }

    // IOReturn (* ClearPipeStallBothEnds)(void* self, UInt8 pipeRef);
    public static int ClearPipeStallBothEnds(MemorySegment self, byte pipeRef) {
        return IOUSBInterfaceInterface190.ClearPipeStallBothEnds(getVtable(self), SegmentScope.global()).apply(self, pipeRef);
    }
}
