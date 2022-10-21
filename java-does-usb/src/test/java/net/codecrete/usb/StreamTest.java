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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class StreamTest extends TestDeviceBase {

    @Test
    void smallTransfer_succeeds() {
        byte[] sampleData = generateRandomBytes(12, 293872394);
        writeBytes(sampleData, 100);
        byte[] data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void mediumTransfer_succeeds() {
        // This synchronous approach should work as the test device
        // has an internal buffer of about 500 bytes.
        byte[] sampleData = generateRandomBytes(140, 97333894);
        writeBytes(sampleData, 30);
        byte[] data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void largeTransferSmallChunks_succeeds() throws Throwable {
        final int numBytes = 230763;
        byte[] sampleData = generateRandomBytes(numBytes, 3829007493L);
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData, 20));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        CompletableFuture.allOf(writer, reader).join();
        if (reader.isCompletedExceptionally())
            throw reader.exceptionNow();
        assertArrayEquals(sampleData, reader.resultNow());
    }

    @Test
    void largeTransferBigChunks_succeeds() throws Throwable {
        final int numBytes = 230763;
        byte[] sampleData = generateRandomBytes(numBytes, 3829007493L);
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData, 150));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        CompletableFuture.allOf(writer, reader).join();
        if (reader.isCompletedExceptionally())
            throw reader.exceptionNow();
        assertArrayEquals(sampleData, reader.resultNow());
    }

    static void writeBytes(byte[] data, int chunkSize) {
        try (var os = testDevice.openOutputStream(LOOPBACK_EP_OUT)) {
            int numBytes = 0;
            while (numBytes < data.length) {
                int size = Math.min(chunkSize, data.length - numBytes);
                os.write(data, numBytes, size);
                numBytes += size;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static byte[] readBytes(int numBytes) {
        var buffer = new byte[numBytes];
        try (var is = testDevice.openInputStream(LOOPBACK_EP_IN)) {
            int bytesRead = 0;
            while (bytesRead < numBytes) {
                bytesRead += is.read(buffer, bytesRead, numBytes - bytesRead);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buffer;
    }
}
