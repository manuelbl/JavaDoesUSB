//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.USBException;
import net.codecrete.usb.windows.gen.kernel32.GUID;
import net.codecrete.usb.windows.gen.kernel32.Kernel32;
import net.codecrete.usb.windows.gen.setupapi.SP_DEVICE_INTERFACE_DATA;
import net.codecrete.usb.windows.gen.setupapi.SetupAPI;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.*;

/**
 * Device property GUIDs and functions
 */
public class DeviceProperty {

    public static final MemorySegment DEVPKEY_Device_Address = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c, (short) 0x4efd,
            (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1,
            (byte) 0x46, (byte) 0xa8, (byte) 0x50, (byte) 0xe0, 30);

    public static final MemorySegment DEVPKEY_Device_InstanceId = createDEVPROPKEY(0x78c34fc8, (short) 0x104a, (short) 0x4aca,
            (byte) 0x9e, (byte) 0xa4, (byte) 0x52, (byte) 0x4d,
            (byte) 0x52, (byte) 0x99, (byte) 0x6e, (byte) 0x57, 256);

    public static final MemorySegment DEVPKEY_Device_Parent = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa, (short) 0x4706,
            (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64,
            (byte) 0x80, (byte) 0x08, (byte) 0xa5, (byte) 0xa7, 8);

    public static int getDeviceIntProperty(Addressable devInfo, Addressable devInfoData, Addressable propertyKey) {
        try (var session = MemorySession.openConfined()) {
            var propertyTypeHolder = session.allocate(JAVA_INT);
            var propertyValueHolder = session.allocate(JAVA_INT);
            if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                    propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
                throw new USBException("Internal error (SetupDiGetDevicePropertyW)", Kernel32.GetLastError());

            if (propertyTypeHolder.get(JAVA_INT, 0) != SetupAPI.DEVPROP_TYPE_UINT32())
                throw new USBException("Internal error (expected property typ UINT32)");

            return propertyValueHolder.get(JAVA_INT, 0);
        }
    }

    public static String getDeviceStringProperty(Addressable devInfo, Addressable devInfoData, Addressable propertyKey) {
        try (var session = MemorySession.openConfined()) {
            var propertyTypeHolder = session.allocate(JAVA_INT);
            var requiredSizeHolder = session.allocate(JAVA_INT);
            if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                    NULL, 0, requiredSizeHolder, 0) == 0) {
                // TODO: Reactivate when proper GetLastError() handling is available
//                int err = Kernel32.GetLastError();
//                if (err != Kernel32.ERROR_INSUFFICIENT_BUFFER())
//                    throw new USBException("Internal error (SetupDiGetDevicePropertyW)", Kernel32.GetLastError());
            }

            if (propertyTypeHolder.get(JAVA_INT, 0) != SetupAPI.DEVPROP_TYPE_STRING())
                throw new USBException("Internal error (expected property typ UINT32)");

            int stringLen = requiredSizeHolder.get(JAVA_INT, 0) / 2 - 1;

            var propertyValueHolder = session.allocateArray(JAVA_CHAR, stringLen + 1);
            if (SetupAPI.SetupDiGetDevicePropertyW(devInfo, devInfoData, propertyKey, propertyTypeHolder,
                    propertyValueHolder, (int) propertyValueHolder.byteSize(), NULL, 0) == 0)
                throw new USBException("Internal error (SetupDiGetDevicePropertyW)", Kernel32.GetLastError());

            return Win.createStringFromSegment(propertyValueHolder);
        }
    }

    public static String getDevicePath(String instanceID, Addressable interfaceGuid) {
        try (var session = MemorySession.openConfined()) {
            // get device info set for instance
            var instanceIDSegment = Win.createSegmentFromString(instanceID, session);
            final var devInfoSetHandle = SetupAPI.SetupDiGetClassDevsW(interfaceGuid, instanceIDSegment, NULL, SetupAPI.DIGCF_PRESENT() | SetupAPI.DIGCF_DEVICEINTERFACE());
            if (Win.IsInvalidHandle(devInfoSetHandle))
                throw new USBException("internal error (SetupDiGetClassDevsW)");

            // ensure the result is destroyed when the scope is left
            session.addCloseAction(() -> SetupAPI.SetupDiDestroyDeviceInfoList(devInfoSetHandle));

            // retrieve first element of enumeration
            var devIntfData = session.allocate(SP_DEVICE_INTERFACE_DATA.$LAYOUT());
            SP_DEVICE_INTERFACE_DATA.cbSize$set(devIntfData, (int) devIntfData.byteSize());
            if (SetupAPI.SetupDiEnumDeviceInterfaces(devInfoSetHandle, NULL, interfaceGuid, 0, devIntfData) == 0)
                throw new USBException("internal error (SetupDiEnumDeviceInterfaces)");

            // get path
            var intfDetailData = session.allocate(SP_DEVICE_INTERFACE_DETAIL_DATA_W.$LAYOUT());
            SP_DEVICE_INTERFACE_DETAIL_DATA_W.cbSize$set(intfDetailData, 8);
            int intfDetailDataSize = (int) SP_DEVICE_INTERFACE_DETAIL_DATA_W.$LAYOUT().byteSize();
            if (SetupAPI.SetupDiGetDeviceInterfaceDetailW(devInfoSetHandle, devIntfData,
                    intfDetailData, intfDetailDataSize, NULL, NULL) == 0)
                throw new USBException("Internal error (SetupDiGetDeviceInterfaceDetailW)", Kernel32.GetLastError());

            return Win.createStringFromSegment(intfDetailData.asSlice(SP_DEVICE_INTERFACE_DETAIL_DATA_W.DevicePath$Offset));
        }
    }

    public static MemorySegment createDEVPROPKEY(int data1, short data2, short data3,
                                           byte data4_0, byte data4_1, byte data4_2, byte data4_3,
                                           byte data4_4, byte data4_5, byte data4_6, byte data4_7,
                                         int pid) {
        var propKey = MemorySession.global().allocate(GUID.sizeof() + JAVA_INT.byteSize());
        Win.setGUID(propKey, data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4, data4_5, data4_6, data4_7);
        propKey.set(JAVA_INT, 16, pid);
        return propKey;
    }

}
