//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for bulk transfer with input/output streams
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamTest extends TestDeviceBase {

    @Test
    void smallTransfer_succeeds() {
        var sampleData = generateRandomBytes(12, 293872394);
        writeBytes(sampleData, 100);
        var data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void mediumTransfer_succeeds() {
        // This synchronous approach should work as the test device
        // has an internal buffer of about 500 bytes.
        var sampleData = generateRandomBytes(140, 97333894);
        writeBytes(sampleData, 30);
        var data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void transferWithZLP_succeeds() {
        var maxPacketSize = testDevice.getEndpoint(UsbDirection.OUT, config.endpointLoopbackOut()).getPacketSize();
        final var sampleData = generateRandomBytes(2 * maxPacketSize, 197007894);
        var writer = CompletableFuture.runAsync(() -> {
            testDevice.transferOut(config.endpointLoopbackOut(), Arrays.copyOfRange(sampleData, 0, maxPacketSize));
            sleep(200);
            testDevice.transferOut(config.endpointLoopbackOut(), Arrays.copyOfRange(sampleData, maxPacketSize, 2 * maxPacketSize));
        });

        var reader = CompletableFuture.supplyAsync(() -> readBytes(sampleData.length));

        CompletableFuture.allOf(writer, reader).join();

        assertArrayEquals(sampleData, reader.resultNow());
    }

    @Test
    void largeTransferSmallChunks_succeeds() {
        final var numBytes = 23076;
        var sampleData = generateRandomBytes(numBytes, 3829007493L);
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData, 20));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        var allFutures = CompletableFuture.allOf(writer, reader);
        allFutures.join();
        assertArrayEquals(sampleData, reader.resultNow());
    }

    @Test
    void largeTransferBigChunks_succeeds() {
        final var numBytes = 230763;
        var sampleData = generateRandomBytes(numBytes, 3829007493L);
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData, 150));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        CompletableFuture.allOf(writer, reader).join();
        assertArrayEquals(sampleData, reader.resultNow());
    }

    @Test
    @SuppressWarnings({"java:S2925", "BusyWait"})
    void blockedWriter_canBeAborted() throws InterruptedException {
        // A writer that fills the pipe faster than it is drained eventually blocks in write().
        // Aborting the outstanding transfers from another thread must terminate it promptly and
        // safely (with an IOException wrapping the USB error) rather than leave it wedged forever.

        final var data = generateRandomBytes(1_000_000, 0x5c7f10ebL);

        final var bytesWritten = new AtomicLong(0);
        final var writerError = new AtomicReference<Throwable>();
        final var readerStream = new AtomicReference<InputStream>();

        // Thread 1: write to the loopback OUT endpoint until it blocks (nothing keeps draining it).
        var writer = new Thread(() -> {
            try (var os = testDevice.openOutputStream(config.endpointLoopbackOut())) {
                var offset = 0;
                while (offset < data.length) {
                    var size = Math.min(100, data.length - offset);
                    os.write(data, offset, size);
                    offset += size;
                    bytesWritten.set(offset);
                }
            } catch (Throwable t) {
                writerError.set(t);
            }
        }, "loopback-writer");

        // Thread 2: read a limited amount from the loopback IN endpoint, then stop draining
        // (the stream stays open, modelling a stalled consumer that applies back pressure).
        var reader = new Thread(() -> {
            try {
                var is = testDevice.openInputStream(config.endpointLoopbackIn());
                readerStream.set(is);
                var buffer = new byte[64];
                var n = is.read(buffer);
                assertTrue(n > 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "loopback-reader");

        try {
            writer.start();
            reader.start();
            reader.join();

            // Wait until the writer is actually blocked: its progress must stall for 300 ms.
            var giveUp = System.currentTimeMillis() + 5000;
            var lastCount = -1L;
            var stableSince = System.currentTimeMillis();
            while (writer.isAlive() && System.currentTimeMillis() < giveUp) {
                var count = bytesWritten.get();
                var now = System.currentTimeMillis();
                if (count != lastCount) {
                    lastCount = count;
                    stableSince = now;
                } else if (now - stableSince >= 300) {
                    break;
                }
                Thread.sleep(20);
            }
            assertTrue(writer.isAlive(), "writer terminated before it could block");

            // Abort the outstanding OUT transfers from this thread; the blocked writer must unwind.
            testDevice.abortTransfers(UsbDirection.OUT, config.endpointLoopbackOut());

            writer.join(1500);
            assertFalse(writer.isAlive(), "writer thread did not terminate within 1.5 s after abort");

            // It must have unwound because of the abort, not by finishing or dying some other way.
            // The stream surfaces the USB error as an IOException (with the UsbException as cause).
            var err = writerError.get();
            assertInstanceOf(IOException.class, err);
            assertInstanceOf(UsbException.class, err.getCause());

        } finally {
            // Restore a clean device state so subsequent tests are not affected.
            var is = readerStream.get();
            if (is != null) {
                try {
                    is.close();
                } catch (IOException _) {
                    // ignore
                }
            }
            if (writer.isAlive()) {
                try {
                    testDevice.abortTransfers(UsbDirection.OUT, config.endpointLoopbackOut());
                } catch (Exception _) {
                    // ignore
                }
                writer.join(1500);
            }
            resetBuffers();
            drainData(config.endpointLoopbackIn());
        }
    }

    static void writeBytes(byte[] data, int chunkSize) {
        try (var os = testDevice.openOutputStream(config.endpointLoopbackOut())) {
            var numBytes = 0;
            while (numBytes < data.length) {
                var size = Math.min(chunkSize, data.length - numBytes);
                os.write(data, numBytes, size);
                numBytes += size;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] readBytes(int numBytes) {
        var buffer = new byte[numBytes];
        try (var is = testDevice.openInputStream(config.endpointLoopbackIn())) {
            var bytesRead = 0;
            while (bytesRead < numBytes) {
                var n = is.read(buffer, bytesRead, numBytes - bytesRead);
                assertTrue(n > 0);
                bytesRead += n;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buffer;
    }

    @SuppressWarnings({"java:S2925", "SameParameterValue"})
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
