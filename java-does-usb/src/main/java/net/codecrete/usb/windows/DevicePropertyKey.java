//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import net.codecrete.usb.windows.gen.setupapi._DEVPROPKEY;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Device property keys (GUIDs)
 */
class DevicePropertyKey {

    private DevicePropertyKey() {
    }

    /**
     * DEVPKEY_Device_Address
     */
    static final MemorySegment Address = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 30);

    /**
     * DEVPKEY_Device_InstanceId
     */
    static final MemorySegment InstanceId = createDEVPROPKEY(0x78c34fc8, (short) 0x104a,
            (short) 0x4aca, (byte) 0x9e, (byte) 0xa4, (byte) 0x52, (byte) 0x4d, (byte) 0x52, (byte) 0x99, (byte) 0x6e
            , (byte) 0x57, 256);

    /**
     * DEVPKEY_Device_Parent
     */
    static final MemorySegment Parent = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa,
            (short) 0x4706, (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64, (byte) 0x80, (byte) 0x08, (byte) 0xa5
            , (byte) 0xa7, 8);

    /**
     * DEVPKEY_Device_Service
     */
    static final MemorySegment Service = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 6);

    /**
     * DEVPKEY_Device_Children
     */
    static final MemorySegment Children = createDEVPROPKEY(0x4340a6c5, (short) 0x93fa,
            (short) 0x4706, (byte) 0x97, (byte) 0x2c, (byte) 0x7b, (byte) 0x64, (byte) 0x80, (byte) 0x08, (byte) 0xa5
            , (byte) 0xa7, 9);

    /**
     * DEVPKEY_Device_HardwareIds
     */
    static final MemorySegment HardwareIds = createDEVPROPKEY(0xa45c254e, (short) 0xdf1c,
            (short) 0x4efd, (byte) 0x80, (byte) 0x20, (byte) 0x67, (byte) 0xd1, (byte) 0x46, (byte) 0xa8, (byte) 0x50
            , (byte) 0xe0, 3);


    @SuppressWarnings({"java:S107", "java:S117"})
    private static MemorySegment createDEVPROPKEY(int data1, short data2, short data3, byte data4_0, byte data4_1,
                                                  byte data4_2, byte data4_3, byte data4_4, byte data4_5,
                                                  byte data4_6, byte data4_7, int pid) {
        @SuppressWarnings("resource")
        var propKey = Arena.global().allocate(_DEVPROPKEY.layout());
        Win.setGUID(_DEVPROPKEY.fmtid(propKey), data1, data2, data3, data4_0, data4_1, data4_2, data4_3, data4_4
                , data4_5, data4_6, data4_7);
        _DEVPROPKEY.pid(propKey, pid);
        return propKey;
    }
}
