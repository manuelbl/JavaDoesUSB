//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import java.util.List;

/**
 * USB device.
 * <p>
 * In order to make control requests and transfer data, the device must be
 * opened and an interface must be claimed. In the open state, this current
 * process has exclusive access to the device.
 * </p>
 * <p>
 * Information about the device can be queried in both the open and the
 * closed state.
 * </p>
 */
public interface USBDevice {

    /**
     * USB product ID.
     *
     * @return product ID
     */
    int productId();

    /**
     * USB vendor ID.
     *
     * @return vendor ID
     */
    int vendorId();

    /**
     * Product name.
     *
     * @return product name or {@code null} if not provided by the device
     */
    String product();

    /**
     * Manufacturer name
     *
     * @return manufacturer name or {@code null} if not provided by the device
     */
    String manufacturer();

    /**
     * Serial number
     * <p>
     * Even though this is supposed to be a human-readable string,
     * some devices are known to provide binary data.
     * </p>
     *
     * @return serial number or {@code null} if not provided by the device
     */
    String serialNumber();

    /**
     * USB device class code ({@code bDeviceClass} from device descriptor).
     *
     * @return class code
     */
    int classCode();

    /**
     * USB device subclass code ({@code bDeviceSubClass} from device descriptor).
     *
     * @return subclass code
     */
    int subclassCode();

    /**
     * USB device protocol ({@code bDeviceProtocol} from device descriptor).
     *
     * @return protocol code
     */
    int protocolCode();

    /**
     * USB protocol version supported by this device.
     *
     * @return version
     */
    Version usbVersion();

    /**
     * Device version (as declared by the manufacturer).
     *
     * @return version
     */
    Version deviceVersion();

    /**
     * Opens the device for communication.
     */
    void open();

    /**
     * Indicates if the device is open.
     *
     * @return {@code true} if the device is open, {@code false} if it is closed.
     */
    boolean isOpen();

    /**
     * Closes the device.
     */
    void close();

    /**
     * Gets the interfaces of this device.
     *
     * @return a list of USB interfaces
     */
    List<USBInterface> interfaces();

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
     * <p>
     * Requests with an interface or an endpoint as recipient are expected to
     * have the interface and endpoint number, respectively, in the lower byte of
     * {@code wIndex}. This convention is enforced by Windows. The addressed interface
     * or the interface of the addressed endpoint must have been claimed.
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
     * <p>
     * Requests with an interface or an endpoint as recipient are expected to
     * have the interface and endpoint number, respectively, in the lower byte of
     * {@code wIndex}. This convention is enforced by Windows. The addressed interface
     * or the interface of the addressed endpoint must have been claimed.
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
     * This method can send data to bulk and interrupt endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127)
     * @param data           data to send
     */
    void transferOut(int endpointNumber, byte[] data);

    /**
     * Receives data from this device.
     * <p>
     * This method blocks until at least a packet has been received or an error has occurred.
     * The minimum value for {@code maxLength} is the maximum size of packets sent on the endpoint.
     * </p>
     * <p>
     * This method can receive data from bulk and interrupt endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127, i.e. without the direction bit)
     * @param maxLength      the maximum data length to receive (in number of bytes)
     * @return received data
     */
    byte[] transferIn(int endpointNumber, int maxLength);

    /**
     * Gets the configuration descriptor.
     *
     * @return the configuration descriptor (as a byte array)
     */
    byte[] configurationDescriptor();
}
