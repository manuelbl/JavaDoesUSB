//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * SP_DEVICE_INTERFACE_DETAIL_DATA_W struct with a path length of 260 characters.
 */
public class SP_DEVICE_INTERFACE_DETAIL_DATA_W {

    // typedef struct _SP_DEVICE_INTERFACE_DETAIL_DATA_W {
    //    DWORD  cbSize;
    //    WCHAR  DevicePath[ANYSIZE_ARRAY];
    //} SP_DEVICE_INTERFACE_DETAIL_DATA_W, *PSP_DEVICE_INTERFACE_DETAIL_DATA_W;
    static final GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("cbSize"),
            MemoryLayout.sequenceLayout(260, JAVA_CHAR).withName("DevicePath")
    );

    public static GroupLayout $LAYOUT() {
        return $struct$LAYOUT;
    }

    static final VarHandle cbSize$VH = $struct$LAYOUT.varHandle(groupElement("cbSize"));

    public static int cbSize$get(MemorySegment seg) {
        return (int) cbSize$VH.get(seg);
    }

    public static void cbSize$set(MemorySegment seg, int x) {
        cbSize$VH.set(seg, x);
    }

    public static final long DevicePath$Offset = $struct$LAYOUT.byteOffset(groupElement("DevicePath"));
}
