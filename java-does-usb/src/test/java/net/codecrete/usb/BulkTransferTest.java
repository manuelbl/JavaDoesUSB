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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BulkTransferTest extends TestDeviceBase {

    @Test
    void smallTransfer_succeeds() {
        var sampleData = generateRandomBytes(12, 293872394);
        writeBytes(sampleData);
        var data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void mediumTransfer_succeeds() {
        // This synchronous approach should work as the test device
        // has an internal buffer of about 500 bytes.
        var sampleData = generateRandomBytes(140, 97333894);
        writeBytes(sampleData);
        var data = readBytes(sampleData.length);
        assertArrayEquals(sampleData, data);
    }

    @Test
    void transferWithZLP_succeeds() {
        var inEndpoint = testDevice.getEndpoint(UsbDirection.IN, config.endpointLoopbackIn());
        var sampleData = generateRandomBytes(inEndpoint.getPacketSize(), 97333894);
        testDevice.transferOut(config.endpointLoopbackOut(), sampleData);
        testDevice.transferOut(config.endpointLoopbackOut(), new byte[0]);
        var data = testDevice.transferIn(config.endpointLoopbackIn());
        assertArrayEquals(sampleData, data);
        data = testDevice.transferIn(config.endpointLoopbackIn());
        assertNotNull(data);
        assertEquals(0, data.length);
    }

    @Test
    void largeTransfer_succeeds() throws Throwable {
        final var numBytes = 230763;
        var sampleData = generateRandomBytes(numBytes, 3829007493L);
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        CompletableFuture.allOf(writer, reader).join();
        if (reader.isCompletedExceptionally())
            throw reader.exceptionNow();
        assertArrayEquals(sampleData, reader.resultNow());
    }

    static void writeBytes(byte[] data) {
        final var chunkSize = 100;
        var numBytes = 0;
        while (numBytes < data.length) {
            var size = Math.min(chunkSize, data.length - numBytes);
            testDevice.transferOut(config.endpointLoopbackOut(), Arrays.copyOfRange(data, numBytes, numBytes + size));
            numBytes += size;
        }
    }

    static byte[] readBytes(int numBytes) {
        var buffer = new ByteArrayOutputStream();
        var bytesRead = 0;
        while (bytesRead < numBytes) {
            var data = testDevice.transferIn(config.endpointLoopbackIn());
            buffer.writeBytes(data);
            bytesRead += data.length;
        }
        return buffer.toByteArray();
    }
}
