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
import java.util.regex.Pattern;

import static net.codecrete.usb.UsbRecipient.DEVICE;
import static net.codecrete.usb.UsbRequestType.STANDARD;

/**
 * Represents a memory segment of the USB device, be it flash memory, RAM or any other type.
 */
public class Segment {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("@([^/]+)/0x([0-9A-Fa-f]+)/");
    private static final Pattern SECTOR_PATTERN = Pattern.compile(",?(\\d+)\\*(\\d+) ?([BKM]?)(.)");

    /**
     * Decodes the segment information from the USB interface description.
     * @param device the USB device
     * @param interfaceNumber the number of the DFU USB interface
     * @return list of segments
     */
    public static List<Segment> getSegments(UsbDevice device, int interfaceNumber) {
        var result = new ArrayList<Segment>();

        // STM uses multiple alternate interface settings to represent segments.
        // The alternate interface setting name describes the sectors within the segment.
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
            for (var sec : seg.getSectors()) {
                if (address >= sec.startAddress() && address < sec.getEndAddress()) {
                    int offset = address - sec.startAddress();
                    int pageNum = offset / sec.pageSize();
                    return new Page(seg, sec.startAddress() + pageNum * sec.pageSize(), 1, sec.pageSize(), sec.attributes());
                }
            }
        }

        return null;
    }

    private final int altSetting;
    private final String name;

    private final List<Page> sectors;


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
        this.altSetting = altSetting;
        sectors = new ArrayList<>();

        var match = SEGMENT_PATTERN.matcher(segmentDesc);
        if (!match.find())
            throw new DFUException("Invalid segment description: " + segmentDesc);
        this.name = match.group(1).trim();
        var startAddress = (int) Long.parseLong(match.group(2), 16);

        match = SECTOR_PATTERN.matcher(segmentDesc.substring(match.end()));
        while (match.find()) {
            var count = Integer.parseInt(match.group(1));
            var size = Integer.parseInt(match.group(2));
            var multiplier = match.group(3);
            var attributes = match.group(4).charAt(0) - 0x60;

            if (multiplier.equals("K")) {
                size *= 1024;
            } else if (multiplier.equals("M")) {
                size *= 1024 * 1024;
            }

            sectors.add(new Page(this, startAddress, count, size, attributes));
            startAddress += size;
        }
    }

    /**
     * Gets the alternative interface setting number
     * @return the setting number
     */
    public int getAltSetting() {
        return altSetting;
    }

    /**
     * Gets the segment name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the sectors withing the segment
     * @return list of sectors
     */
    public List<Page> getSectors() {
        return sectors;
    }
}
