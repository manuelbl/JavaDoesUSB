//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.InputStream;
import java.io.OutputStream;
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
public interface UsbDevice {

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
    String getSerialNumber();

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
     * USB protocol version supported by this device.
     *
     * @return version
     */
    @NotNull Version getUsbVersion();

    /**
     * Device version (as declared by the manufacturer).
     *
     * @return version
     */
    @NotNull Version getDeviceVersion();

    /**
     * Detaches the standard operating-system drivers of this device.
     * <p>
     * By detaching the standard drivers, the operating system releases the exclusive access to the device
     * and/or some or all of the device's interfaces. This allows the application to open the device and claim
     * interfaces. It is relevant for device and interfaces implementing standard USB classes, such as HID, CDC
     * and mass storage.
     * </p>
     * <p>
     * This method should be called before the device is opened. After the device has been closed,
     * {@link #attachStandardDrivers()} should be called to restore the previous state.
     * </p>
     * <p>
     * On macOS, all device drivers are immediately detached from the device. To execute it, the application must
     * be run as <i>root</i>. Without <i>root</i> privileges, the method does nothing.
     * </p>
     * <p>
     * On Linux, this method changes the behavior of {@link #claimInterface(int)} for this device. The standard drivers
     * will be detached interface by interface when the interface is claimed.
     * </p>
     * <p>
     * On Windows, this method does nothing. It is not possible to temporarily change the drivers.
     * </p>
     */
    void detachStandardDrivers();

    /**
     * Reattaches the standard operating-system drivers to this device.
     * <p>
     * By attaching the standard drivers, the operating system claims the device and/or its interfaces if they
     * implement standard USB classes, such as HID, CDC and mass storage. It is used to restore the state before
     * calling {@link #detachStandardDrivers()}.
     * </p>
     * <p>
     * This method should be called after the device has been closed.
     * </p>
     * <p>
     * On macOS, the application must be run as <i>root</i>. Without <i>root</i> privileges, the method does nothing.
     * </p>
     * <p>
     * On Linux, this method changes the behavior of {@link #claimInterface(int)}. Standard drivers will no longer be
     * detached when the interface is claimed. Standard drivers are automatically reattached when the interfaces
     * are released, at the lasted when the device is closed.
     * </p>
     * <p>
     * On Windows, this method does nothing.
     * </p>
     */
    void attachStandardDrivers();

    /**
     * Indicates if the device is connected.
     * <p>
     * When a {@link UsbDevice} instance is initially returned by {@link Usb#getDevices()} and related methods,
     * it is connected. When the user unplugs the device, the application can still hold on to instance of
     * {@link UsbDevice} even though the actual USB device is gone. This method can be used to check if the
     * device is still connected.
     * </p>
     * @return {@code true} if the device is connected, {@code false} if it is no longer connected
     */
    boolean isConnected();

    /**
     * Opens the device for communication.
     */
    void open();

    /**
     * Indicates if the device is open.
     *
     * @return {@code true} if the device is open, {@code false} if it is closed.
     */
    boolean isOpened();

    /**
     * Closes the device.
     */
    void close();

    /**
     * Gets the interfaces of this device.
     * <p>
     * The returned list is sorted by interface number.
     * </p>
     *
     * @return a list of USB interfaces
     */
    @NotNull @Unmodifiable List<UsbInterface> getInterfaces();

    /**
     * Gets the interface with the specified number.
     *
     * @param interfaceNumber the interface number
     * @return the interface
     * @exception UsbException if the interface does not exist
     */
    @NotNull UsbInterface getInterface(int interfaceNumber);

    /**
     * Gets the endpoint with the specified number.
     *
     * @param direction      the endpoint direction
     * @param endpointNumber the endpoint number (between 1 and 127)
     * @return the endpoint
     * @exception UsbException if the endpoint does not exist
     */
    @NotNull UsbEndpoint getEndpoint(UsbDirection direction, int endpointNumber);

    /**
     * Claims the specified interface for exclusive use.
     *
     * @param interfaceNumber the interface number
     */
    void claimInterface(int interfaceNumber);

    /**
     * Selects the alternate settings for the specified interface.
     * <p>
     * The device must be open and the interface must be claimed for exclusive access.
     * </p>
     *
     * @param interfaceNumber interface number
     * @param alternateNumber alternate setting number
     */
    void selectAlternateSetting(int interfaceNumber, int alternateNumber);

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
     * @param transfer control transfer setup parameters
     * @param length maximum length of expected data
     * @return received data.
     */
    byte @NotNull [] controlTransferIn(@NotNull UsbControlTransfer transfer, int length);

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
     * @param transfer control transfer setup parameters
     * @param data  data to send, or {@code null} if the transfer has no data stage.
     */
    void controlTransferOut(@NotNull UsbControlTransfer transfer, byte[] data);

    /**
     * Sends data to this device.
     * <p>
     * This method blocks until the data has been sent or an error has occurred.
     * </p>
     * <p>
     * This method can send data to bulk and interrupt endpoints.
     * </p>
     * <p>
     * If the sent data length is a multiple of the packet size, it is often
     * required to send an additional zero-length packet (ZLP) for the device
     * to actually process the data. This method will not do it automatically.
     * </p>
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127)
     * @param data           data to send
     */
    void transferOut(int endpointNumber, byte @NotNull [] data);

    /**
     * Sends data to this device.
     * <p>
     * This method blocks until the data has been sent, the timeout period has expired
     * or an error has occurred. If the timeout expires, a {@link UsbTimeoutException} is thrown.
     * </p>
     * <p>
     * This method can send data to bulk and interrupt endpoints.
     * </p>
     * <p>
     * If the sent data length is a multiple of the packet size, it is often
     * required to send an additional zero-length packet (ZLP) for the device
     * to actually process the data. This method will not do it automatically.
     * </p>
     *
     * @param endpointNumber the endpoint number (in the range between 1 and 127)
     * @param data           data to send
     * @param timeout        the timeout period, in milliseconds (0 for no timeout)
     */
    void transferOut(int endpointNumber, byte @NotNull [] data, int timeout);

    /**
     * Sends data to this device.
     * <p>
     * This method blocks until the data has been sent, the timeout period has expired
     * or an error has occurred. If the timeout expires, a {@link UsbTimeoutException} is thrown.
     * </p>
     * <p>
     * This method can send data to bulk and interrupt endpoints.
     * </p>
     * <p>
     * If the sent data length is a multiple of the packet size, it is often
     * required to send an additional zero-length packet (ZLP) for the device
     * to actually process the data. This method will not do it automatically.
     * </p>
     *
     * @param endpointNumber the endpoint number (in the range between 1 and 127)
     * @param data           buffer containing data to send
     * @param offset         offset of the first byte to send
     * @param length         number of bytes to send
     * @param timeout        the timeout period, in milliseconds (0 for no timeout)
     */
    void transferOut(int endpointNumber, byte @NotNull [] data, int offset, int length, int timeout);

    /**
     * Receives data from this device.
     * <p>
     * This method blocks until at least a packet has been received or an error has occurred.
     * </p>
     * <p>
     * The returned data is the payload of a packet. It can have a length of 0 if the USB device
     * sends zero-length packets to indicate the end of a data unit.
     * </p>
     * <p>
     * This method can receive data from bulk and interrupt endpoints.
     * </p>
     *
     * @param endpointNumber endpoint number (in the range between 1 and 127, i.e. without the direction bit)
     * @return received data
     */
    byte @NotNull [] transferIn(int endpointNumber);

    /**
     * Receives data from this device.
     * <p>
     * This method blocks until at least a packet has been received, the timeout period has expired
     * or an error has occurred. If the timeout expired, a {@link UsbTimeoutException} is thrown.
     * </p>
     * <p>
     * The returned data is the payload of a packet. It can have a length of 0 if the USB device
     * sends zero-length packets to indicate the end of a data unit.
     * </p>
     * <p>
     * This method can receive data from bulk and interrupt endpoints.
     * </p>
     *
     * @param endpointNumber the endpoint number (in the range between 1 and 127, i.e. without the direction bit)
     * @param timeout        the timeout period, in milliseconds (0 for no timeout)
     * @return received data
     */
    byte @NotNull [] transferIn(int endpointNumber, int timeout);

    /**
     * Opens a new output stream to send data to a bulk endpoint.
     * <p>
     * All data written to this output stream is sent to the specified bulk endpoint.
     * Buffering and concurrent IO requests are used to achieve a high throughput.
     * </p>
     * <p>
     * The stream will insert zero-length packets if {@link OutputStream#flush()} is called
     * and the last packet size was equal to maximum packet size of the endpoint.
     * </p>
     * <p>
     * If {@link #transferOut(int, byte[])} and a output stream or multiple output streams
     * are used concurrently for the same endpoint, the behavior is unpredictable.
     * </p>
     *
     * @param endpointNumber bulk endpoint number (in the range between 1 and 127)
     * @param bufferSize approximate buffer size (in bytes)
     * @return the new output stream
     */
    @NotNull OutputStream openOutputStream(int endpointNumber, int bufferSize);

    /**
     * Opens a new output stream to send data to a bulk endpoint.
     * <p>
     * The buffer is configured with minimal size. In all other aspects, this method
     * works like {@link #openOutputStream(int, int)}.
     * </p>
     * @param endpointNumber bulk endpoint number (in the range between 1 and 127)
     * @return the new output stream
     */
    default @NotNull OutputStream openOutputStream(int endpointNumber) {
        return openOutputStream(endpointNumber, 1);
    }

    /**
     * Opens a new input stream to receive data from a bulk endpoint.
     * <p>
     * All data received from the specified bulk endpoint can be read using this input stream.
     * Buffering and concurrent IO requests are used to achieve a high throughput.
     * </p>
     * <p>
     * If the buffers contain data when the stream is closed, this data will be discarded.
     * If {@link #transferIn(int)} and an input stream or multiple input streams
     * are used concurrently for the same endpoint, the behavior is unpredictable.
     * </p>
     *
     * @param endpointNumber bulk endpoint number (in the range between 1 and 127, i.e. without the direction bit)
     * @param bufferSize approximate buffer size (in bytes)
     * @return the new input stream
     */
    @NotNull InputStream openInputStream(int endpointNumber, int bufferSize);

    /**
     * Opens a new input stream to receive data from a bulk endpoint.
     * <p>
     * The buffer is configured with minimal size. In all other aspects, this method
     * works like {@link #openInputStream(int, int)}.
     * </p>
     *
     * @param endpointNumber bulk endpoint number (in the range between 1 and 127, i.e. without the direction bit)
     * @return the new input stream
     */
    default @NotNull InputStream openInputStream(int endpointNumber) {
        return openInputStream(endpointNumber, 1);
    }

    /**
     * Aborts all transfers on an endpoint.
     * <p>
     * This operation is not valid on the control endpoint 0.
     * </p>
     *
     * @param direction      endpoint direction
     * @param endpointNumber endpoint number (in the range between 1 and 127)
     */
    void abortTransfers(UsbDirection direction, int endpointNumber);

    /**
     * Clears an endpoint's halt condition.
     * <p>
     * An endpoint is halted (aka stalled) if an error occurs in the communication. Before the
     * communication can resume, the halt condition must be cleared. A halt condition can exist
     * in a single direction only.
     * </p>
     * <p>
     * Control endpoint 0 will never be halted.
     * </p>
     *
     * @param direction      endpoint direction
     * @param endpointNumber endpoint number (in the range between 1 and 127)
     */
    void clearHalt(UsbDirection direction, int endpointNumber);

    /**
     * Gets the device descriptor.
     *
     * @return the device descriptor (as a byte array)
     */
    byte @NotNull [] getDeviceDescriptor();

    /**
     * Gets the configuration descriptor.
     *
     * @return the configuration descriptor (as a byte array)
     */
    byte @NotNull [] getConfigurationDescriptor();
}
