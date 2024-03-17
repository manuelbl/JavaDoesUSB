//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Unit test to measure loopback speed
//

package net.codecrete.usb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedTest extends TestDeviceBase {

    @Test
    void loopback_isFast() throws Throwable {
        final var isHighSpeed = testDevice.getEndpoint(UsbDirection.IN, config.endpointLoopbackIn()).getPacketSize() == 512;
        final var numBytes = isHighSpeed ? 5000000 : 500000;

        var sampleData = generateRandomBytes(numBytes, 7219937602343L);

        var start = System.currentTimeMillis();
        var writer = CompletableFuture.runAsync(() -> writeBytes(sampleData));
        var reader = CompletableFuture.supplyAsync(() -> readBytes(numBytes));
        CompletableFuture.allOf(writer, reader).join();
        var end = System.currentTimeMillis();
        if (reader.isCompletedExceptionally())
            throw reader.exceptionNow();

        assertArrayEquals(sampleData, reader.resultNow());

        var throughput = 2.0 * numBytes / (end - start);
        var expectedThroughput = isHighSpeed ? 15000.0 : 500.0;
        System.out.printf("Throughput: expected ≥%,.0f KB/s, actual %,.0f KB/s%n", expectedThroughput, throughput);

        assertTrue(throughput >= expectedThroughput, "Expected throughput not achieved");
    }

    static void writeBytes(byte[] data) {
        try (var os = testDevice.openOutputStream(config.endpointLoopbackOut())) {
            os.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static byte[] readBytes(int numBytes) {
        try (var is = testDevice.openInputStream(config.endpointLoopbackIn())) {
           var buffer = new byte[numBytes];
            var bytesRead = 0;
            while (bytesRead < numBytes) {
                var n = is.read(buffer, bytesRead, numBytes - bytesRead);
                if (n <= 0)
                    throw new RuntimeException("unexpected end of input stream");
                bytesRead += n;
            }
            return buffer;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }
}
