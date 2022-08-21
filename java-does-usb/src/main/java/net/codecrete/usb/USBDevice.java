//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB device.
 *
 * <p>
 * Instances of this class are associated with operating system resources
 * and must be close when they are no longer used.
 * </p>
 */
public interface USBDevice extends AutoCloseable {

    /**
     * USB product ID.
     *
     * @return product ID
     */
    int getProductId();

    /**
     * USB vendor ID.
     *
     * @return vendor ID
     */
    int getVendorId();

    /**
     * Product name.
     *
     * @return product name or {@code null} if not provided by the device
     */
    String getProduct();

    /**
     * Manufacturer name
     *
     * @return manufacturer name or {@code null} if not provided by the device
     */
    String getManufacturer();

    /**
     * Serial number
     * <p>
     * Even though this is supposed to be a human-readable string,
     * some devices are known to provide binary data.
     * </p>
     *
     * @return serial number or {@code null} if not provided by the device
     */
    String getSerial();

    /**
     * USB device class code ({@code bDeviceClass} from device descriptor).
     *
     * @return class code
     */
    int getClassCode();

    /**
     * USB device subclass code ({@code bDeviceSubClass} from device descriptor).
     *
     * @return subclass code
     */
    int getSubclassCode();

    /**
     * USB device protocol ({@code bDeviceProtocol} from device descriptor).
     *
     * @return protocol code
     */
    int getProtocolCode();

    /**
     * Claims the specified interface for exclusive use.
     *
     * @param interfaceNumber the interface number
     */
    void claimInterface(int interfaceNumber);

    /**
     * Releases the specified interface from exclusive use.
     *
     * @param interfaceNumber the interface number
     */
    void releaseInterface(int interfaceNumber);

    /**
     * Requests data from the control endpoint.
     * <p>
     * This method blocks until the device has responded or an error has occurred.
     * </p>
     * <p>
     * The control transfer request is sent to endpoint 0. The transfer is expected to
     * have a Data In stage.
     * </p>
     *
     * @param setup  control transfer setup parameters
     * @param length maximum length of expected data
     * @return received data.
     */
    byte[] controlTransferIn(USBControlTransfer setup, int length);

    /**
     * Executes a control transfer request and optionally sends data.
     * <p>
     * This method blocks until the device has acknowledge the request or an error has occurred.
     * </p>
     * <p>
     * The control transfer request is sent to endpoint 0. The transfer is expected to either have
     * no data stage or a Data Out stage.
     * </p>
     *
     * @param setup control transfer setup parameters
     * @param data  data to send, or {@code null} if the transfer has no data stage.
     */
    void controlTransferOut(USBControlTransfer setup, byte[] data);

    /**
     * Sends data to this device.
     * <p>
     * This method blocks until the data has been sent or an error has occurred.
     * </p>
     * <p>
     * This method is suitable for bulk and interrupt endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127)
     * @param data           data to send
     */
    void transferOut(int endpointNumber, byte[] data);

    /**
     * Receives data to this device.
     * <p>
     * This method blocks until at least a packet has been received or an error has occurred.
     * The minimum value for {@code maxLength} is the maximum size of packets sent on the endpoint.
     * </p>
     * <p>
     * This method is suitable for bulk and interrupt endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127, i.e. without the direction bit)
     * @param maxLength      the maximum data length to receive (in number of bytes)
     * @return received data
     */
    byte[] transferIn(int endpointNumber, int maxLength);
}
