//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

import net.codecrete.usb.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DFU device.
 * <p>
 * Implements the main DFU operations like download, upload etc.
 * </p>
 */
public class DFUDevice {

    private final USBDevice usbDevice_;
    private final int interfaceNumber_;
    private final int transferSize_;
    private final Version dfuVersion_;

    private List<Segment> segments_;

    /**
     * Gets all connected DFU devices
     * @return List of DFU devices
     */
    public static List<DFUDevice> getAll() {
        return USB.getAllDevices().stream()
                .filter(DFUDevice::hasDFUDescriptor)
                .map(DFUDevice::new)
                .collect(Collectors.toList());
    }

    /**
     * Checks if the device has a DFU functional descriptor.
     * @param device USB device
     * @return {@code true} if it has a DFU descriptor, {@code false} otherwise
     */
    public static boolean hasDFUDescriptor(USBDevice device) {
        return getDFUDescriptorOffset(device.configurationDescriptor()) > 0
                && getDFUInterfaceNumber(device) >= 0;
    }

    /**
     * Gets the offset of the DFU functional descriptor within the USB configuration descriptor.
     * @param descriptor USB configuration descriptor
     * @return descriptor offset (in bytes), or -1 if not found
     */
    public static int getDFUDescriptorOffset(byte[] descriptor) {
        int offset = 0;
        while (offset < descriptor.length) {
            if (descriptor[offset + 1] == 0x21)
                return offset;
            offset += descriptor[offset] & 255;
        }
        return -1;
    }

    /**
     * Gets the DFU interface number.
     * @param device the USB device
     * @return the interface number, of -1 if not found
     */
    public static int getDFUInterfaceNumber(USBDevice device) {
        for (var intf : device.interfaces()) {
            var alt = intf.alternate();
            if (alt.classCode() == 0xFE && alt.subclassCode() == 0x01 && alt.protocolCode() == 0x02)
                return intf.number();
        }

        return -1;
    }

    /**
     * Creates a new DFUDevice instance.
     * <p>
     * The specified USB device must have a DFU descriptor and a DFU interface.
     * </p>
     * @param usbDevice the USB device
     */
    public DFUDevice(USBDevice usbDevice) {
        usbDevice_ = usbDevice;
        interfaceNumber_ = getDFUInterfaceNumber(usbDevice);

        var configDesc = usbDevice.configurationDescriptor();
        int offset = getDFUDescriptorOffset(configDesc);
        assert offset > 0;

        transferSize_ = getInt16(configDesc, offset + 5);
        dfuVersion_ = new Version(getInt16(configDesc, offset + 7));
    }

    /**
     * Gets the DFU protocol version.
     * @return the protocol version
     */
    public Version dfuVersion() {
        return dfuVersion_;
    }

    /**
     * Gets the device serial number.
     * @return the serial number
     */
    public String serialNumber() {
        return usbDevice_.serialNumber();
    }

    /**
     * Opens the DFU device for communication.
     */
    public void open() {
        usbDevice_.open();
        usbDevice_.claimInterface(interfaceNumber_);
        segments_ = Segment.getSegments(usbDevice_, interfaceNumber_);
        clearErrorIfNeeded();
    }

    /**
     * Closes the DFU device.
     */
    public void close() {
        usbDevice_.close();
    }

    /**
     * Clears an error status.
     */
    public void clearStatus() {
        var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.CLEAR_STATUS.value(), 0, interfaceNumber_);
        usbDevice_.controlTransferOut(setup, null);
    }

    /**
     * Aborts the download mode.
     */
    public void abort() {
        var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.ABORT.value(), 0, interfaceNumber_);
        usbDevice_.controlTransferOut(setup, null);
    }

    /**
     * Gets the full DFU status
     * @return the status
     */
    public DFUStatus getStatus() {
        var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.GET_STATUS.value(), 0, interfaceNumber_);
        return DFUStatus.fromBytes(usbDevice_.controlTransferIn(setup, 6));
    }

    /**
     * Reads from the device flash memory.
     * @param address the memory start address
     * @param length the length of memory to read
     * @return the read data
     */
    public byte[] read(int address, int length) {
        expectState(DeviceState.DFU_IDLE, DeviceState.DFU_DNLOAD_IDLE);
        setAddress(address);
        exitDownloadMode();

        expectState(DeviceState.DFU_IDLE, DeviceState.DFU_UPLOAD_IDLE);

        var result = new byte[length];

        // read full chunks
        int offset = 0;
        int blockNum = 2;
        while (offset < length) {
            int chunkSize = Math.min(transferSize_, length - offset);
            var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.UPLOAD.value(), blockNum, interfaceNumber_);
            var chunk = usbDevice_.controlTransferIn(setup, chunkSize);
            System.arraycopy(chunk, 0, result, offset, chunkSize);
            offset += chunkSize;
            blockNum += 1;
        }

        // request zero lenght chunk to exit out of upload mode
        var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.UPLOAD.value(), blockNum, interfaceNumber_);
        usbDevice_.controlTransferIn(setup, 0);

        return result;
    }

    public void verify(byte[] firmware) {
        byte[] firmware2 = read(STM32.FLASH_BASE_ADDRESS, firmware.length);
        if (!Arrays.equals(firmware, firmware2))
            throw new DFUException("Verification failed - content differs");
    }

    public void download(byte[] firmware) {
        int length = firmware.length;

        // validate start and end address exist and are writable
        int startAddress = STM32.FLASH_BASE_ADDRESS;
        var firstPage = getWritablePage(startAddress);
        getWritablePage(startAddress + length);

        // TODO: select alternate interface settings
        System.out.printf("Target memory segment: %s%n", firstPage.segment().name());

        // erase if needed
        if (firstPage.isErasable())
            erase(startAddress, length);

        // download firmware
        setAddress(startAddress);

        int offset = 0;
        int transaction = 2;
        while (offset < length) {
            int chunkSize = Math.min(length - offset, transferSize_);

            byte[] chunk = new byte[chunkSize];
            System.arraycopy(firmware, offset, chunk, 0, chunkSize);

            System.out.printf("Writing data at 0x%x (size 0x%x)%n", startAddress + offset, chunkSize);
            var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.DOWNLOAD.value(), transaction, interfaceNumber_);
            usbDevice_.controlTransferOut(setup, chunk);

            finishDownloadCommand("writing data");

            offset += chunkSize;
            transaction += 1;
        }

        exitDownloadMode();
    }

    /**
     * Erases the specified range.
     * <p>
     * Only applicable to erasable sector, i.e. flash memory.
     * </p>
     * <p>
     * Only entire pages can be erase. If start and end address to not fall onto
     * page boundaries, this method will extend the range to be erased.
     * </p>
     * @param startAddress the start address of the range
     * @param length the length of the range
     */
    public void erase(int startAddress, int length) {
        int endAddress = startAddress + length;

        while (startAddress < endAddress) {
            var page = findPage(startAddress);
            if (page == null)
                throw new DFUException(String.format("No valid memory segment at address 0x%x", startAddress));
            if (!page.isErasable())
                throw new DFUException(String.format("Page at address 0x%x is not erasable", startAddress));

            System.out.printf("Erasing page at 0x%x (size 0x%x)%n", page.startAddress(), page.pageSize());
            erasePage(page.startAddress());
            startAddress = page.endAddress();
        }
    }

    public void erasePage(int address) {
        execDownloadCommandWithAddress((byte) 0x41, "erasing page", address);
    }

    public void setAddress(int address) {
        execDownloadCommandWithAddress((byte) 0x21, "setting address", address);
    }

    private void execDownloadCommandWithAddress(byte command, String action, int address) {
        var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.DOWNLOAD.value(), 0, interfaceNumber_);
        var data = new byte[] {
                command,
                (byte) address,
                (byte) (address >> 8),
                (byte) (address >> 16),
                (byte) (address >> 24)
        };
        usbDevice_.controlTransferOut(setup, data);

        finishDownloadCommand(action);
    }

    private void finishDownloadCommand(String action) {
        var status = getStatus();
        if (status.state() != DeviceState.DFU_DNBUSY)
            throw new DFUException("Unexpected state for " + action);

        sleep(status.pollTimeout());

        status = getStatus();
        if (status.status() != DeviceStatus.OK)
            throw new DFUException("Unexpected state after " + action);

        sleep(status.pollTimeout());
    }

    private Page getWritablePage(int address) {
        var page = findPage(address);
        if (page == null)
            throw new DFUException(String.format("No valid memory segment at address 0x%x", address));
        if (!page.isWritable())
            throw new DFUException(String.format("Page at address 0x%x is not writable", address));
        return page;
    }

    private Page findPage(int address) {
        return Segment.findPage(segments_, address);
    }

    private void exitDownloadMode() {
        abort();

        var status = getStatus();
        if (status.state() != DeviceState.DFU_IDLE)
            throw new DFUException("Unexpected state after exiting from download mode");

        sleep(status.pollTimeout());
    }

    public void startApplication() {
        expectState(DeviceState.DFU_IDLE, DeviceState.DFU_DNLOAD_IDLE);

        var setup = new USBControlTransfer(USBRequestType.CLASS, USBRecipient.INTERFACE, DFURequest.DOWNLOAD.value(), 2, interfaceNumber_);
        usbDevice_.controlTransferOut(setup, null);

        var status = getStatus();
        if (status.state() != DeviceState.DFU_MANIFEST)
            throw new DFUException("Exiting DFU mode and starting firmware has failed");
    }

    private void clearErrorIfNeeded() {
        var status = getStatus();
        if (status.status() != DeviceStatus.OK) {
            clearStatus();
            sleep(status.pollTimeout());
            status = getStatus();
            if (status.status() != DeviceStatus.OK)
                throw new DFUException("Cannot clear error status");
        }
    }

    private void expectState(DeviceState state1, DeviceState state2) {
        var status = getStatus();
        if (status.state() != state1 && status.state() != state2)
            throw new DFUException(
                    String.format("Expected state %s or %s but got %s", state1, state2, status.state()));
    }

    private int getInt16(byte[] config, int offset) {
        return (config[offset] & 0xff) + 256 * (config[offset + 1] & 0xff);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new DFUException("Sleep failed", e);
        }
    }
}
