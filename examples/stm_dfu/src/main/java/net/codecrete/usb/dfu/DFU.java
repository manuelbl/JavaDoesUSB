//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command line application for uploading firmware to STM32 microcontrollers.
 * <p>
 * Only the STM32 variant of the DFU protocol is supported.
 * Only binary firmware format is supported (no .hex or .dfu files).
 * </p>
 */
public class DFU {

    /**
     * Main function
     * @param args arguments (path to firmware)
     */
    public static void main(String[] args) {
        // check for single parameter
        if (args.length != 1) {
            System.err.println("Usage: dfu_upload <firmware_file>");
            System.exit(1);
            return;
        }

        // read firmware file
        byte[] firmware;
        try {
            firmware = Files.readAllBytes(Path.of(args[0]));
        } catch (IOException e) {
            System.err.printf("Error: Cannot read firmware file %s%n", args[0]);
            System.exit(2);
            return;
        }

        // check for single DFU device
        var devices = DFUDevice.getAll();
        if (devices.isEmpty()) {
            System.err.println("Error: No STM32 DFU device connected (or not in DFU mode)");
            System.exit(4);
            return;
        } else if (devices.size() > 1) {
            System.err.println("Error: Multiple STM32 DFU devices connected. Please connect only one.");
            System.exit(4);
            return;
        }
        var device = devices.getFirst();
        System.out.printf("DFU device found with serial %s.%n", device.getSerialNumber());

        // download and verify firmware
        try {
            device.open();
            device.download(firmware);
            device.verify(firmware);
            System.out.println("Firmware successfully downloaded and verified");

            device.startApplication();
            System.out.println("DFU mode ended and firmware started");

            device.close();

        } catch (DFUException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(3);
        }
    }
}
