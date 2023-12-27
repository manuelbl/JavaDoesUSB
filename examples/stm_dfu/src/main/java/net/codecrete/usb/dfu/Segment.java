//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

import net.codecrete.usb.UsbControlTransfer;
import net.codecrete.usb.UsbDevice;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static net.codecrete.usb.UsbRecipient.DEVICE;
import static net.codecrete.usb.UsbRequestType.STANDARD;

/**
 * Represents a memory segment of the USB device, be it flash memory, RAM or any other type.
 */
public class Segment {

    /**
     * Derives the segments from the USB interface description.
     * @param device the USB device
     * @param interfaceNumber the number of the DFU USB interface
     * @return list of segments
     */
    public static List<Segment> getSegments(UsbDevice device, int interfaceNumber) {
        var result = new ArrayList<Segment>();

        // STM uses multiple alternate interface settings to represent segments.
        // The alternate interface settings name describes the sectors within the segment.
        var configDesc = device.getConfigurationDescriptor();
        int offset = 0;
        while (offset < configDesc.length) {
            if (configDesc[offset + 1] == 4 && (configDesc[offset + 2] & 0xff) == interfaceNumber) {
                int altSetting = configDesc[offset + 3] & 0xff;
                int stringIndex = configDesc[offset + 8] & 0xff;
                var altSettingName = getStringDescriptor(device, stringIndex);
                result.add(new Segment(altSetting, altSettingName));
            }
            offset += configDesc[offset] & 0xff;
        }

        return result;
    }

    /**
     * Retrieves a USB string.
     * @param device the USB device
     * @param index the index of the string descriptor
     * @return the string
     */
    private static String getStringDescriptor(UsbDevice device, int index) {
        var setup = new UsbControlTransfer(STANDARD, DEVICE, 6, (3 << 8) | index, 0);
        byte[] stringDesc = device.controlTransferIn(setup, 255);
        int descLen = stringDesc[0] & 0xff;
        return new String(stringDesc, 2, descLen - 2, StandardCharsets.UTF_16LE);
    }

    /**
     * Gets the page for the specified address.
     * @param segments the list of segments
     * @param address the address
     * @return the page, or {@code null} if not found
     */
    public static Page findPage(List<Segment> segments, int address) {
        for (var seg : segments) {
            for (var sec : seg.sectors()) {
                if (address >= sec.startAddress() && address < sec.endAddress()) {
                    int offset = address - sec.startAddress();
                    int pageNum = offset / sec.pageSize();
                    return new Page(seg, sec.startAddress() + pageNum * sec.pageSize(), 1, sec.pageSize(), sec.attributes());
                }
            }
        }

        return null;
    }

    private final int altSetting_;
    private final String name_;

    private final List<Page> sectors_;


    /**
     * Creates a new instance.
     * <p>
     * The segment descriptor is the name of the USB alternate interface setting.
     * </p>
     * @param altSetting alternate interface setting number
     * @param segmentDesc segment descriptor
     */
    private Segment(int altSetting, String segmentDesc) {
        // The format is described in "UM0424 STM32 USB-FS-Device development kit", ch. 10.3.2
        altSetting_ = altSetting;
        sectors_ = new ArrayList<>();
        int offset = segmentDesc.indexOf('/', 1);
        name_ = segmentDesc.substring(1, offset).trim();

        int startAddress = 0;
        while (offset < segmentDesc.length()) {
            // parse start address
            if (segmentDesc.charAt(offset) == '/') {
                int addressEnd = segmentDesc.indexOf('/', offset + 1);
                startAddress = (int) Long.parseLong(segmentDesc.substring(offset + 3, addressEnd), 16);
                offset = addressEnd + 1;
                continue;
            }

            // skip comma
            if (segmentDesc.charAt(offset) == ',')
                offset += 1;

            // parse count
            int countEnd = segmentDesc.indexOf('*', offset);
            int count = Integer.parseInt(segmentDesc.substring(offset, countEnd));
            offset = countEnd + 1;

            // parse sector size
            int sizeEnd = offset;
            while (Character.isDigit(segmentDesc.charAt(sizeEnd)))
                sizeEnd += 1;
            int size = Integer.parseInt(segmentDesc.substring(offset, sizeEnd));
            offset = sizeEnd;

            // skip whitespace
            while (segmentDesc.charAt(offset) == ' ')
                offset += 1;

            // parse unit
            char unitChar = segmentDesc.charAt(offset);
            if (unitChar == 'B') {
                offset += 1;
            } else if (unitChar == 'K') {
                size *= 1024;
                offset += 1;
            } else if (unitChar == 'M') {
                size *= 1024 * 1024;
                offset += 1;
            }

            // parse sector attributes
            int sectorAttrs = segmentDesc.charAt(offset) - 0x40;
            offset += 1;

            sectors_.add(new Page(this, startAddress, count, size, sectorAttrs));
            startAddress += size;
        }
    }

    /**
     * Gets the alternative interface setting number
     * @return the setting number
     */
    public int altSetting() {
        return altSetting_;
    }

    /**
     * Gets the segment name.
     * @return the name
     */
    public String name() {
        return name_;
    }

    /**
     * Gets the sectors withing the segment
     * @return list of sectors
     */
    public List<Page> sectors() {
        return sectors_;
    }
}
