//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test for bulk transfer
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class BulkTransferTest extends TestDeviceBase {

    private static final int LOOPBACK_EP_OUT = 1;
    private static final int LOOPBACK_EP_IN = 2;
    private static final int MAX_PACKET_SIZE = 64;

    @Test
    void smallTransfer_succeeds() {
        byte[] sampleData = generateRandomBytes(12, 293872394);
        writeBytes(sampleData);
        byte[] data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void mediumTransfer_succeeds() {
        // This synchronous approach should work as the test device
        // has an internal buffer of about 500 bytes.
        byte[] sampleData = generateRandomBytes(140, 97333894);
        writeBytes(sampleData);
        byte[] data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void largeTransfer_succeeds() throws Throwable {
        final int numBytes = 230763;
        byte[] sampleData = generateRandomBytes(numBytes, 3829007493L);
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        CompletableFuture.allOf(writer, reader).join();
        if (reader.isCompletedExceptionally())
            throw reader.exceptionNow();
        assertArrayEquals(sampleData, reader.resultNow());
    }

    static void writeBytes(byte[] data) {
        final int chunkSize = 100;
        int numBytes = 0;
        while (numBytes < data.length) {
            int size = Math.min(chunkSize, data.length - numBytes);
            device.transferOut(LOOPBACK_EP_OUT, Arrays.copyOfRange(data, numBytes, numBytes + size));
            numBytes += size;
        }
    }
    static byte[] readBytes(int numBytes) {
        var buffer = new ByteArrayOutputStream();
        int bytesRead = 0;
        while (bytesRead < numBytes) {
            byte[] data = device.transferIn(LOOPBACK_EP_IN, MAX_PACKET_SIZE);
            buffer.writeBytes(data);
            bytesRead += data.length;
        }
        return buffer.toByteArray();
    }

    static byte[] generateRandomBytes(int numBytes, long seed) {
        var random = new Random(seed);
        var bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }
}
