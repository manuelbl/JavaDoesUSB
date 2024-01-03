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
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
