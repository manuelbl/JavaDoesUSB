//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.sample;

import net.codecrete.usb.*;

/**
 * Interacts with a USB CDC device (serial device) directly, without using the
 * standard USB CDC driver.
 *
 * <p>
 * This test is aimed at Linux, which requires to unload and restore the
 * kernel driver. It will neither work on macOS (the USB CDC driver claims the
 * interface with exclusive access) nor on Windows (only devices with the
 * WinUSB driver can be opened).
 * </p>
 * <p>
 * For the test to work on Linux, the user must have sufficient permission to
 * access the device at the USB level. So check the permissions under
 * /dev/bus/usb/... The permissions of the serial device (/dev/tty...) do not
 * apply for this test.
 * </p>
 */
public class USBSerialTest {
    public static void main(String[] args) {

        for (var device : USB.getAllDevices()) {
            int commInterfaceNum = getCDCCommInterfaceNum(device);
            if (commInterfaceNum >= 0) {
                System.out.printf("USB CDC device: %s%n", device);
                interact(device, commInterfaceNum);
            }
        }
    }

    static void interact(USBDevice device, int commInterfaceNum) {
        // communication and data interface must have consecutive numbers
        int dataInterfaceNum = commInterfaceNum + 1;

        // open device and interfaces
        device.open();
        device.claimInterface(commInterfaceNum);
        device.claimInterface(dataInterfaceNum);

        // set line coding (9600bps, 8 bit)
        byte[] coding = { (byte)0x80, 0x25, 0, 0, 0, 0, 8 };
        device.controlTransferOut(
                new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, 0x20, 0, commInterfaceNum),
                coding);

        // send some data
        int dataOutEp = getDataOutEndpointNum(device, dataInterfaceNum);
        byte[] data = { 'H', 'e', 'l', 'l', 'o', '\r', '\n' };
        device.transferOut(dataOutEp, data);

        // close device and interfaces
        device.releaseInterface(dataInterfaceNum);
        device.releaseInterface(commInterfaceNum);
        device.close();
    }

    static int getCDCCommInterfaceNum(USBDevice device) {
        // CDC ACM implementations consist of two consecutive interfaces
        // with certain class, subclass and protocol codes
        int numInterfaces = device.interfaces().size();
        if (numInterfaces < 2)
            return -1;

        for (int i = 0; i < numInterfaces - 1; i += 1) {
            var commIntf = device.getInterface(i).alternate();
            var dataIntf = device.getInterface(i + 1).alternate();

            if (commIntf.classCode() == 2 && commIntf.subclassCode() == 2 && commIntf.protocolCode() == 1 && dataIntf.classCode() == 10)
                return i;
        }

        return -1;
    }

    static int getDataOutEndpointNum(USBDevice device, int dataInterfaceNum) {
        var altInterface = device.getInterface(dataInterfaceNum).alternate();
        for (var endpoint : altInterface.endpoints()) {
            if (endpoint.direction() == USBDirection.OUT)
                return endpoint.number();
        }
        return -1;
    }
}
