//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.special;

import net.codecrete.usb.TestDeviceConfig;
import net.codecrete.usb.Usb;
import net.codecrete.usb.UsbDevice;
import net.codecrete.usb.UsbException;

import java.io.IOException;
import java.util.HashMap;

import static java.time.Duration.*;

/**
 * Test for robustness when USB devices is unplugged during operation.
 *
 * <p>
 * Requires use of test device.
 * </p>
 */
public class Unplug {
    private static final HashMap<UsbDevice, DeviceWorker> activeDevices = new HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Plug and unplug test device multiple times.");
        System.out.println("Hit ENTER to exit.");

        Usb.setOnDeviceConnected(Unplug::onPluggedDevice);
        Usb.setOnDeviceDisconnected(Unplug::onUnpluggedDevice);
        Usb.getDevices().forEach(Unplug::onPluggedDevice);

        //noinspection ResultOfMethodCallIgnored
        System.in.read();
    }

    private static void onPluggedDevice(UsbDevice device) {
        var config = TestDeviceConfig.getConfig(device);
        if (config.isEmpty())
            return;

        var worker = new DeviceWorker(device, config.get());
        activeDevices.put(device, worker);
        worker.start();
    }

    private static void onUnpluggedDevice(UsbDevice device) {
        var config = TestDeviceConfig.getConfig(device);
        if (config.isEmpty())
            return;

        var worker = activeDevices.remove(device);
        worker.setDisconnectTime(System.currentTimeMillis());
        worker.join();
    }

    static class DeviceWorker {

        private final UsbDevice device;
        private final TestDeviceConfig config;

        private final int seed;

        private long disconnectTime;

        private final HashMap<Thread, Work> workTracking = new HashMap<>();

        DeviceWorker(UsbDevice device, TestDeviceConfig config) {
            this.device = device;
            this.config = config;
            this.seed = (int) System.currentTimeMillis();
        }

        void start() {
            System.out.println("Device connected");

            device.open();
            device.claimInterface(config.interfaceNumber());

            // start loopback sender and receiver
            startThread((seed & 1) != 0 ? this::sendLoopbackDataStream : this::sendLoopbackData);
            startThread((seed & 2) != 0 ? this::receiveLoopbackDataStream : this::receiveLoopbackData);

            // start echo sender and receiver
            if (config.endpointEchoOut() > 0) {
                startThread(this::sendEcho);
                startThread(this::receiveEcho);
            }
        }

        private void startThread(Runnable action) {
            var thread = new Thread(() -> runAction(action));
            var work = new Work();
            workTracking.put(thread, work);
            thread.start();
        }

        void join() {
            // wait for threads to finish
            for (var thread : workTracking.keySet()) {
                try {
                    boolean terminated = thread.join(ofSeconds(5));
                    if (!terminated)
                        System.err.printf("Thread \"%s\" failed to join within 5s%n", thread.getName());

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            device.close();

            // check achieved work
            for (var e : workTracking.entrySet()) {
                var work = e.getValue();
                var expectedWork = work.expectedWorkPerSec * 0.001 * (work.finishTime - work.startTime);
                if (work.actualWork < expectedWork)
                    System.err.printf("Thread \"%s\" achieved insufficient work executed. Expected: %.0f, achieved: %d%n",
                            e.getKey().getName(), expectedWork, work.actualWork);
            }

            // check that the threads haven't terminated early
            for (var e : workTracking.entrySet()) {
                long duration = Math.abs(e.getValue().finishTime - disconnectTime);
                if (duration > 500)
                    System.err.printf("Thread \"%s\" has likely crashed early%n", e.getKey().getName());
            }

            System.out.println("Device disconnected");
        }

        void setDisconnectTime(long time) {
            disconnectTime = time;
        }

        private synchronized void logFinish() {
            var work = workTracking.get(Thread.currentThread());
            work.finishTime = System.currentTimeMillis();
        }

        private void logStart(String operation, long expectedWorkPerSec) {
            Thread.currentThread().setName(operation);
            var work = workTracking.get(Thread.currentThread());
            work.startTime = System.currentTimeMillis();
            work.expectedWorkPerSec = expectedWorkPerSec;
        }

        private void logWork(long amount) {
            var work = workTracking.get(Thread.currentThread());
            work.actualWork += amount;
        }

        private void runAction(Runnable action) {
            try {
                action.run();
            } catch (UsbException e) {
                logFinish();
            }
        }

        private void sendLoopbackData() {
            logStart("sending loopback data", 300_000);
            var prng = new PRNG();
            var data = new byte[5000];
            //noinspection InfiniteLoopStatement
            while (true) {
                prng.fill(data);
                device.transferOut(config.endpointLoopbackOut(), data, 1000);
                logWork(data.length);
            }
        }

        private void receiveLoopbackData() {
            logStart("receiving loopback data", 300_000);
            var prng = new PRNG();
            //noinspection InfiniteLoopStatement
            while (true) {
                byte[] data = device.transferIn(config.endpointLoopbackIn());
                int index = prng.verify(data);
                if (index >= 0)
                    throw new RuntimeException("invalid data received");
                logWork(data.length);
            }
        }

        private void sendLoopbackDataStream() {
            logStart("sending loopback data with output stream", 300_000);
            var prng = new PRNG();
            var data = new byte[5000];
            try (var os = device.openOutputStream(config.endpointLoopbackOut())) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    prng.fill(data);
                    os.write(data);
                    logWork(data.length);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void receiveLoopbackDataStream() {
            logStart("receiving loopback data with input stream", 300_000);
            var prng = new PRNG();
            try (var is = device.openInputStream(config.endpointLoopbackIn())) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    var data = new byte[2000];
                    int n = is.read(data);
                    int index = prng.verify(data, n);
                    if (index >= 0)
                        throw new RuntimeException("invalid data received");
                    logWork(n);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendEcho() {
            logStart("sending echo", 7);
            var data = new byte[] { 0x03, 0x45, 0x73, (byte)0xb3, (byte)0x9f, 0x3f, 0x00, 0x6a };
            //noinspection InfiniteLoopStatement
            while (true) {
                device.transferOut(config.endpointEchoOut(), data);
                logWork(1);
                sleep(100);
            }
        }

        private void receiveEcho() {
            logStart("receiving echo", 14);
            //noinspection InfiniteLoopStatement
            while (true) {
                device.transferIn(config.endpointEchoIn());
                logWork(1);
            }
        }

        @SuppressWarnings({"SameParameterValue", "java:S2925"})
        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Work {
        long startTime;
        long expectedWorkPerSec;
        long actualWork;
        long finishTime;
    }

    /**
     * Pseudo random number generator
     */
    static class PRNG {
        private int state;
        private int nBytes;
        private int bits;

        int next() {
            int x = state;
            x ^= x << 13;
            x ^= x >>> 17;
            x ^= x << 5;
            state = x;
            return x;
        }

        void fill(byte[] data) {
            int len = data.length;
            for (int i = 0; i < len; i++) {
                if (nBytes == 0) {
                    bits = next();
                    nBytes = 4;
                }
                data[i] = (byte) bits;
                bits >>>= 8;
                nBytes--;
            }
        }

        int verify(byte[] data) {
            return verify(data, data.length);
        }

        int verify(byte[] data, int len) {
            for (int i = 0; i < len; i++) {
                if (nBytes == 0) {
                    bits = next();
                    nBytes = 4;
                }
                if (data[i] != (byte) bits)
                    return i;
                bits >>>= 8;
                nBytes--;
            }
            return -1;
        }
    }
}
