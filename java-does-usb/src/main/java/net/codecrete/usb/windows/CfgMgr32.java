//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.windows;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Windows CfgMgr32 functions and structures
 */
public class CfgMgr32 {

    private static final MethodHandle CM_Get_Parent$Func;
    private static final MethodHandle CM_Get_Device_IDW$Func;

    static {
        var session = MemorySession.openShared();
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.libraryLookup("CfgMgr32", session);

        // CMAPI CONFIGRET CM_Get_Parent(
        //  [out] PDEVINST pdnDevInst,
        //  [in]  DEVINST  dnDevInst,
        //  [in]  ULONG    ulFlags
        //);
        CM_Get_Parent$Func = linker.downcallHandle(
                lookup.lookup("CM_Get_Parent").get(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT)
        );

        // CMAPI CONFIGRET CM_Get_Device_IDW(
        //  [in]  DEVINST dnDevInst,
        //  [out] PWSTR   Buffer,
        //  [in]  ULONG   BufferLen,
        //  [in]  ULONG   ulFlags
        //);
        CM_Get_Device_IDW$Func = linker.downcallHandle(
                lookup.lookup("CM_Get_Device_IDW").get(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT)
        );
    }

    // CMAPI CONFIGRET CM_Get_Parent(
    //  [out] PDEVINST pdnDevInst,
    //  [in]  DEVINST  dnDevInst,
    //  [in]  ULONG    ulFlags
    //);
    public static int CM_Get_Parent(Addressable pdnDevInst, int dnDevInst, int ulFlags) {
        try {
            return (int) CM_Get_Parent$Func.invokeExact(pdnDevInst, dnDevInst, ulFlags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // CMAPI CONFIGRET CM_Get_Device_IDW(
    //  [in]  DEVINST dnDevInst,
    //  [out] PWSTR   Buffer,
    //  [in]  ULONG   BufferLen,
    //  [in]  ULONG   ulFlags
    //);
    public static int CM_Get_Device_IDW(int dnDevInst, Addressable buffer, int bufferLen, int ulFlags) {
        try {
            return (int) CM_Get_Device_IDW$Func.invokeExact(dnDevInst, buffer, bufferLen, ulFlags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
