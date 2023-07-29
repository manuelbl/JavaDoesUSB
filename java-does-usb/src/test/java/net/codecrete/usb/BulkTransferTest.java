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

import static org.junit.jupiter.api.Assertions.*;

class BulkTransferTest extends TestDeviceBase {

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
    void transferWithZLP_succeeds() {
        var inEndpoint = testDevice.getEndpoint(USBDirection.IN, LOOPBACK_EP_IN);
        byte[] sampleData = generateRandomBytes(inEndpoint.packetSize(), 97333894);
        testDevice.transferOut(LOOPBACK_EP_OUT, sampleData);
        testDevice.transferOut(LOOPBACK_EP_OUT, new byte[0]);
        byte[] data = testDevice.transferIn(LOOPBACK_EP_IN);
        assertArrayEquals(sampleData, data);
        data = testDevice.transferIn(LOOPBACK_EP_IN);
        assertNotNull(data);
        assertEquals(0, data.length);
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
            testDevice.transferOut(LOOPBACK_EP_OUT, Arrays.copyOfRange(data, numBytes, numBytes + size));
            numBytes += size;
        }
    }
    static byte[] readBytes(int numBytes) {
        var buffer = new ByteArrayOutputStream();
        int bytesRead = 0;
        while (bytesRead < numBytes) {
            byte[] data = testDevice.transferIn(LOOPBACK_EP_IN);
            buffer.writeBytes(data);
            bytesRead += data.length;
        }
        return buffer.toByteArray();
    }
}
