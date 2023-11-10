//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Firmware for logic analyzer is from Sigrok project
// and licensed under GNU GPL (version 2, or later).
//

package net.codecrete.usb.special;

import net.codecrete.usb.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sample program for sampling data with a logic analyzer.
 * <p>
 * This sample assumes that a Saleae 8 clone with the VID 0x0925 and PID
 * 0x3881 is connected.
 * </p>
 * <p>
 * The USB communication of Saleae 8 clones (and any other logic analyzer
 * based on a similar Cypress chip) is very sensitive as the chip only has
 * a tiny internal buffer and – for the maximum 24 MHz sample rate – operates
 * rather close to the practical limit of USB 2.0 high-speed. If the USB bus
 * has other traffic from other devices or if the JVM takes a GC time-out,
 * the internal buffer overruns and the logic analyzer stops data acquisition.
 * </p>
 * <p>
 * The firmware for the Cypress chips needs to be uploaded once after the
 * device has been connected. After the firmware upload, the device disconnect
 * and reconnect after about 2 to 3 seconds. Vendor and product ID do not
 * change but the manufacturer name and serial number do.
 * </p>
 */
public class LogicAnalyzer implements Closeable {

    /// USB vendor ID
    final static int VID = 0x0925;
    /// USB product ID
    final static int PID = 0x3881;

    // bulk endpoint number
    final static int EP = 2;

    /// Effective sample rate (in Hz)
    private int effSampleRate;
    /// Sample period (in clock ticks)
    private int period;
    /// Sample duration (in ms)
    private int duration;
    /// Flag to use 48 MHz clock (instead of 30 MHz)
    private boolean use48Mhz;
    /// Flag that buffer overrun has been detected
    private volatile boolean bufferOverrunDetected;
    /// Flag that acquisition should/has stopped
    private volatile boolean stopped;
    /// Filename for saving sample data
    private String filename;
    /// Variable is updated while new data is received
    private volatile long activityValue;
    /// Indicates a dry run (to suppress output)
    private boolean isDryRun;
    /// Buffer size for input stream (good for approx. 0.2s of data)
    private int bufferSize;

    public static void main(String[] args) {
        try (var logicAnalyzer = new LogicAnalyzer()) {
            logicAnalyzer.sampleData(24000000, 5000, "sample.bin");
        }
    }

    private UsbDevice device;

    LogicAnalyzer() {
        var optionalDevice = Usb.findDevice(VID, PID);
        if (optionalDevice.isEmpty())
            throw new IllegalStateException("no logic analyzer connected");

        device = optionalDevice.get();

        checkFirmware();

        device.open();
        device.claimInterface(0);

        dryRun(12000000, 100);
        dryRun(10000000, 200);
    }

    @Override
    public void close() {
        device.close();
    }

    /**
     * Sample data and save it to the file
     * @param sampleRate sample rate (in Hz)
     * @param duration duration (in ms)
     * @param filename filename to save to, or {@code null} to not save it
     */
    void sampleData(int sampleRate, int duration, String filename) {
        if (sampleRate < 20000 || sampleRate > 24000000) {
            System.err.println("Sample rate outside the supported range of 10kHz to 24Mhz");
            return;
        }

        stopped = false;
        bufferOverrunDetected = false;
        this.filename = filename;
        prepareSampling(sampleRate, duration);
        var acquirer = CompletableFuture.runAsync(this::saveSamples);
        sleep(50); // give thread time to start
        startAcquisition();
        var watchdog = CompletableFuture.runAsync(this::detectBufferOverrun);
        CompletableFuture.allOf(acquirer, watchdog).join();
    }

    void dryRun(int sampleRate, int duration) {
        isDryRun = true;
        sampleData(sampleRate, duration, null);
        isDryRun = false;
    }

    void prepareSampling(int sampleRate, int duration) {
        this.duration = duration;

        // calculate the optimal sample rate and if to use the 48 or 30 MHz clock
        int ticks48Mhz = (48000000 + sampleRate / 2) / sampleRate;
        int ticks30Mhz = (30000000 + sampleRate / 2) / sampleRate;
        double err48Mhz = Math.abs(48000000.0 / ticks48Mhz - sampleRate);
        double err30Mhz = Math.abs(30000000.0 / ticks30Mhz - sampleRate);

        if (ticks48Mhz <= 0x0600 && err48Mhz <= err30Mhz) {
            use48Mhz = true;
            period = ticks48Mhz;
            effSampleRate = 48000000 / period;
        } else {
            use48Mhz = false;
            period = ticks30Mhz;
            effSampleRate = 30000000 / period;
        }

        bufferSize = (int) Math.round(effSampleRate * 0.2);
        bufferSize = Math.max(bufferSize, 16 * 4096);
    }

    /**
     * Start acquisition with the specified sample rate
     */
    void startAcquisition() {
        // Command structure (3 bytes)
        //   Bit 5: 0 - 8 bit samples, 1 - 16 bit samples
        //   Bit 6: 0 - 30 MHz clock, 1 - 48 MHz clock
        // Byte 0: flags (not needed here)
        // Byte 1-2: clock ticks (-1) between two samples (16 bit, big endian)

        int ticks = period - 1;
        byte flags = use48Mhz ? (byte)(1 << 6) : 0;

        var cmd = new byte[3];
        cmd[0] = flags;
        cmd[1] = (byte)(ticks >> 8);
        cmd[2] = (byte)(ticks & 0xff);

        // send the start command
        final int commandCodeStart = 0xb1;
        device.controlTransferOut(new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.DEVICE, commandCodeStart, 0, 0), cmd);
    }

    void saveSamples() {

        // retrieve the sample data from the bulk endpoint
        int expectedSize = (int)(((long)duration * effSampleRate + 500) / 1000);

        byte[] sampleData = new byte[expectedSize];

        int size = 0;
        try (var is = device.openInputStream(EP, bufferSize)) {

            while (size < expectedSize) {
                int n = is.read(sampleData, size, sampleData.length - size);
                if (n <= 0)
                    break;
                size += n;
                activityValue = size;
            }

        } catch (UsbException e) {
            if (!stopped && !bufferOverrunDetected)
                throw e;

        } catch (IOException e) {
            System.err.println("Retrieving samples failed");
            e.printStackTrace(System.err);
            return;
        }

        if (bufferOverrunDetected) {
            System.err.println("Buffer overflow, acquisition has stopped");
        } else if (!stopped) {
            stopAcquisition();
        } else {
            stopped = true;
        }

        if (!isDryRun)
            System.out.printf("%,d samples retrieved with %,d sample/s%n", size, effSampleRate);

        if (filename != null) {
            try {
                Files.write(Path.of(filename), sampleData);
            } catch (IOException e) {
                System.err.printf("Saving samples to %s failed%n", filename);
                e.printStackTrace(System.err);
            }
        }
    }

    void stopAcquisition() {
        // stop the acquisition by halting the bulk endpoint and clearing the halt
        stopped = true;
        device.abortTransfers(UsbDirection.IN, EP);
    }

    void detectBufferOverrun() {
        sleep(10);
        // if the 'activityValue' hasn't changed within 20ms, the logic analyzer
        // has stopped sending data (likely due to a buffer overrun)
        long lastValue = 0;
        while (true) {
            sleep(5);

            if (stopped)
                break;

            if (lastValue == activityValue) {
                bufferOverrunDetected = true;
                stopAcquisition();
                return;
            }

            lastValue = activityValue;
        }
    }

    void checkFirmware() {
        if (device.getManufacturer() != null) {
            System.out.println("Device ready");
            return;
        }

        System.out.println("Uploading firmware...");

        byte[] firmware;
        // load open-source firmware from Sigrok project (see http://sigrok.org/wiki/Fx2lafw)
        try (var is = getClass().getClassLoader().getResourceAsStream("fx2lafw-saleae-logic.fw")) {
            firmware = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        device.open();
        device.claimInterface(0);

        byte[] cmd = new byte[] { 1 };
        device.controlTransferOut(new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.DEVICE, 0xa0, 0xe600, 0x0000), cmd);

        final int len = firmware.length;
        int offset = 0;
        while (offset < len) {
            int n = Math.min(len - offset, 0x1000);
            byte[] chunk = Arrays.copyOfRange(firmware, offset, offset + n);
            device.controlTransferOut(new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.DEVICE, 0xa0, offset, 0x0000), chunk);
            offset += n;
        }

        cmd = new byte[] { 0 };
        device.controlTransferOut(new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.DEVICE, 0xa0, 0xe600, 0x0000), cmd);

        device.close();
        DeviceMonitor.instance().awaitDevice(false);

        System.out.println("Waiting for device to reconnect...");

        DeviceMonitor.instance().awaitDevice(true);
        sleep(200);

        var optionalDevice = Usb.findDevice(VID, PID);
        if (optionalDevice.isEmpty())
            throw new IllegalStateException("no logic analyzer connected");
        device = optionalDevice.get();
        if (device.getManufacturer() == null)
            throw new IllegalStateException("firmware upload failed");

        System.out.println("Device is ready");
    }

    void sleep(long milliseconds) {
        while (true) {
            try {
                //noinspection BusyWait
                Thread.sleep(milliseconds); // NOSONAR
                return;
            } catch (InterruptedException e) {
                // ignore and try again
            }
        }
    }


    static class DeviceMonitor {

        private static DeviceMonitor singleInstance;

        private final Lock deviceLock = new ReentrantLock();
        private final Condition deviceConnected = deviceLock.newCondition();
        private boolean isDeviceConnected;

        static synchronized DeviceMonitor instance() {
            if (singleInstance == null) {
                singleInstance = new DeviceMonitor();
                singleInstance.start();
            }
            return singleInstance;
        }

        private DeviceMonitor() { }

        private void start() {
            Usb.setOnDeviceConnected((device) -> onDeviceConnected(device, true));
            Usb.setOnDeviceDisconnected((device) -> onDeviceConnected(device, false));
            isDeviceConnected = Usb.findDevice(VID, PID).isPresent();
        }

        private void onDeviceConnected(UsbDevice device, boolean connected) {
            if (device.getVendorId() == VID && device.getProductId() == device.getProductId()) {
                try {
                    deviceLock.lock();
                    isDeviceConnected = connected;
                    deviceConnected.signalAll();

                } finally {
                    deviceLock.unlock();
                }
            }
        }

        void awaitDevice(boolean connected) {
            try {
                deviceLock.lock();
                while (isDeviceConnected != connected)
                    deviceConnected.awaitUninterruptibly();

            } finally {
                deviceLock.unlock();
            }
        }
    }
}
