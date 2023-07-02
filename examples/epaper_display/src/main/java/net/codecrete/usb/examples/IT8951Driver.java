//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import net.codecrete.usb.USB;
import net.codecrete.usb.USBDevice;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * Driver for the IT8951 e-paper display controller.
 */
public class IT8951Driver {

    private static final int ENDPOINT_IN = 1;
    private static final int ENDPOINT_OUT = 2;

    private static final byte[] GET_SYS_CMD = {
            (byte)0xfe, 0, 0x38, 0x39, 0x35, 0x31, (byte)0x80, 0, 0x01, 0, 0x02, 0, 0, 0, 0, 0
    };
    private static final byte[] LD_IMG_AREA_CMD = {
            (byte)0xfe, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xa2, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };
    private static final byte[] DPY_AREA_CMD = {
            (byte)0xfe, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x94, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    private USBDevice device;
    private int sequenceNo = 1;

    private DisplayInfo displayInfo;

    /**
     * Opens the controller for communication.
     *
     * @throws IllegalStateException if no IT8951 device is found
     */
    public void open() {
        device = USB.getDevice(dev -> dev.vendorId() == 0x048d && dev.productId() == 0x8951);
        if (device == null)
            throw new IllegalStateException("No IT8951 device found");

        device.detachStandardDrivers();
        device.open();
        try {
            device.claimInterface(0);
            var sysInfoBytes = readCommand(GET_SYS_CMD, DisplayInfo.LENGTH);
            displayInfo = DisplayInfo.from(sysInfoBytes);

        } catch (Throwable t) {
            device.close();
            throw t;
        }
    }

    /**
     * Displays the provided image at the specified position.
     * <p>
     * The provided image must be an 8-bit grayscale image,
     * and it must fully fit within the bounds of the display.
     * </p>
     * <p>
     * The image is rendered in 4-bit grayscale mode. For the
     * reduction to 4-bit, Floyd-Steinberg dithering is used.
     * </p>
     *
     * @param image image
     * @param x x-position
     * @param y y-position
     */
    public void displayImage(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();
        var stride = image.getRaster().getDataBuffer().getSize() / image.getHeight();
        int address = info().imageBufBase;

        // split into bands to no exceed 60KB transfer size
        int bandHeight = (60000 - 20) / width;

        var errors = createInitialDitheringErrors(width);

        for (int yOffset = 0; yOffset < height; yOffset += bandHeight) {
            bandHeight = Math.min(bandHeight, height - yOffset);
            var pixelData = pixelsFromImage(image, 0, yOffset, width, bandHeight);
            errors = dither(pixelData, stride, errors);
            loadImageArea(new Area(address, x, y + yOffset, width, bandHeight), pixelData);
        }

        displayArea(new DisplayArea(address, 2, x, y, width, height, 1));
    }

    /**
     * Close the communication with the controller.
     */
    public void close() {
        device.close();
        device.attachStandardDrivers();
    }

    /**
     * Gets information about the display and controller.
     *
     * @return information
     */
    public DisplayInfo info() {
        return displayInfo;
    }

    /**
     * Execute a read command.
     * @param command command
     * @param expectedLength expected length of received data (in bytes)
     * @return received data
     */
    private byte[] readCommand(byte[] command, int expectedLength) {
        var cmd = createCommandBlock(command, expectedLength, true);
        device.transferOut(ENDPOINT_OUT, cmd);
        byte[] result = device.transferIn(ENDPOINT_IN, 1000);
        readStatus();
        return result;
    }

    /**
     * Execute a write command.
     * @param command command
     * @param data1 part 1 of data to be written
     * @param data2 part 2 of data to be written (or {@code null} if there is no second part)
     */
    private void writeCommand(byte[] command, byte[] data1, byte[] data2) {
        var cmd = createCommandBlock(command, data1.length + ((data2 != null) ? data2.length : 0), false);
        device.transferOut(ENDPOINT_OUT, cmd);
        device.transferOut(ENDPOINT_OUT, data1);
        if (data2 != null)
            device.transferOut(ENDPOINT_OUT, data2);
        readStatus();
    }

    /**
     * Execute a write command.
     * @param command command
     * @param data data to be written
     */
    private void writeCommand(byte[] command, byte[] data) {
        writeCommand(command, data, null);
    }

    /**
     * Wraps the given command in a command block.
     * @param command command
     * @param dataLength length of data to be read or written (in bytes)
     * @param isDirectionIn indicates if data direction is in (from device to host)
     * @return the command block
     */
    private byte[] createCommandBlock(byte[] command, int dataLength, boolean isDirectionIn) {
        var cmd = ByteBuffer.allocate(15 + command.length).order(ByteOrder.LITTLE_ENDIAN);
        cmd.putInt(0x43425355); // signature
        cmd.putInt(sequenceNo);
        sequenceNo += 1;
        cmd.putInt(dataLength); // data transfer length
        cmd.put((byte)(isDirectionIn ? 0x80 : 0x00)); // flags
        cmd.put((byte)0); // logical unit number
        cmd.put((byte)command.length);
        cmd.put(command);
        return cmd.array();
    }

    private void loadImageArea(Area area, byte[] pixelData) {
        writeCommand(LD_IMG_AREA_CMD, area.toByteArray(), pixelData);
    }

    private void displayArea(DisplayArea area) {
        writeCommand(DPY_AREA_CMD, area.toByteArray());
    }

    private byte[] pixelsFromImage(BufferedImage image, int x, int y, int w, int h) {
        var buffer = (DataBufferByte) image.getRaster().getDataBuffer();
        var data = buffer.getData();
        var stride = buffer.getSize() / image.getHeight();

        byte[] pixels = new byte[w * h];
        for (int iy = 0; iy < h; iy += 1)
            System.arraycopy(data, (iy + y) * stride + x, pixels, iy * w, w);

        return pixels;
    }

    /**
     * Initializes the errors for dithering.
     * @param width image width (in pixels)
     * @return initial errors
     */
    private int[] createInitialDitheringErrors(int width) {
        // initialize errors with random values in the range [-127, 128].
        int[] errors = new int[width + 2];
        var random = new Random();

        for (int i = 0; i < errors.length; i++)
            errors[i] = random.nextInt(256) - 127;

        return errors;
    }

    /**
     * Quantizes the provided grayscale pixel data to 16 levels of gray
     * using Floyd-Steinberg dithering.
     * <p>
     * The pixel data is quantized in place. The resulting grayscale values
     * are 0, 16, 32, ... 240.
     * </p>
     * <p>
     * The pixel data consists of a single byte per pixel. A new pixel line
     * starts every <i>stride</i> bytes. The actual image width can be
     * slightly shorter.
     * </p>
     * @param pixels array with pixel data to be modified
     * @param stride the
     * @param errors errors carried forward from the previous band
     * @return errors to be carried forward to the next band
     */
    private int[] dither(byte[] pixels, int stride, int[] errors) {
        int[] currentErrors = errors;
        int[] nextErrors = new int[errors.length];

        for (int offset = 0; offset < pixels.length; offset += stride) {
            Arrays.fill(nextErrors, 0);
            ditherRow(pixels, offset, currentErrors, nextErrors);

            // swap error arrays
            int[] errs = currentErrors;
            currentErrors = nextErrors;
            nextErrors = errs;
        }

        return currentErrors;
    }

    private void ditherRow(byte[] pixels, int offset, int[] currentErrors, int[] nextErrors) {
        int w = currentErrors.length - 2;
        for (int i = 0; i < w; i += 1) {
            int targetValue = (pixels[offset + i] & 0xff) + (currentErrors[i + 1] + 7) / 16;
            int quantizedValue = targetValue < 0 ? 0 : (targetValue > 0xf0 ? 0xf0 : (targetValue + 7) & 0xf0);
            pixels[offset + i] = (byte) quantizedValue;
            int quantizationError = targetValue - quantizedValue;
            currentErrors[i + 2] += quantizationError * 7;
            nextErrors[i] += quantizationError * 3;
            nextErrors[i + 1] += quantizationError * 5 ;
            nextErrors[i + 2] += quantizationError;
        }
    }

    /**
     * Read the status block.
     * @return status block
     */
    private Status readStatus() {
        var result = device.transferIn(ENDPOINT_IN, 1000);
        if (result.length == 13)
            return Status.from(result);

        throw new RuntimeException(String.format("Unexpected length of status block (%d)", result.length));
    }

    /**
     * Display and controller information
     *
     * @param standardCmdNo standard command number 2T-con Communication Protocol
     * @param extendedCmdNo extended command number
     * @param signature signature (0x31 0x35 0x39 0x38 (8951))
     * @param version command table version
     * @param width display width (in pixels)
     * @param height display height (in pixels)
     * @param updateBufBase update buffer base address
     * @param imageBufBase image buffer base address (index 0)
     * @param temperatureNo temperature segment number
     * @param modeNo display mode number
     * @param frameCount frame count for each mode (8 elements)
     * @param numImgBuf number of image buffers
     */
    public record DisplayInfo(
            int standardCmdNo,
            int extendedCmdNo,
            int signature,
            int version,
            int width,
            int height,
            int updateBufBase,
            int imageBufBase,
            int temperatureNo,
            /// Display mode
            int modeNo,
            int[] frameCount, // 8 elements
            int numImgBuf
    ) {
        static DisplayInfo from(byte[] bytes) {
            var buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            int standardCmdNo = buf.getInt();
            int extendedCmdNo = buf.getInt();
            int signature = buf.getInt();
            int version = buf.getInt();
            int width = buf.getInt();
            int height = buf.getInt();
            int updateBufBase = buf.getInt();
            int imageBufBase = buf.getInt();
            int temperatureNo = buf.getInt();
            int modeNo = buf.getInt();
            int[] frameCount = new int[8];
            for (int i = 0; i < 8; i += 1)
                frameCount[i] = buf.getInt();
            int numImgBuf = buf.getInt();
            return new DisplayInfo(standardCmdNo, extendedCmdNo, signature, version, width, height,
                    updateBufBase, imageBufBase, temperatureNo, modeNo, frameCount, numImgBuf);
        }

        static final int LENGTH = 112;
    }

    record Status(int sequence, int dataRemaining, byte status) {
        static Status from(byte[] bytes) {
            var buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            int sequence = buf.getInt();
            int dataLeft = buf.getInt();
            byte status = buf.get();
            return new Status(sequence, dataLeft, status);
        }
    }

    record Area(int address, int x, int y, int w, int h) {
        byte[] toByteArray() {
            var buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
            buf.putInt(address);
            buf.putInt(x);
            buf.putInt(y);
            buf.putInt(w);
            buf.putInt(h);
            return buf.array();
        }
    }

    record DisplayArea(int address, int mode, int x, int y, int w, int h, int wait_ready) {
        byte[] toByteArray() {
            var buf = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN);
            buf.putInt(address);
            buf.putInt(mode);
            buf.putInt(x);
            buf.putInt(y);
            buf.putInt(w);
            buf.putInt(h);
            buf.putInt(wait_ready);
            return buf.array();
        }
    }
}
