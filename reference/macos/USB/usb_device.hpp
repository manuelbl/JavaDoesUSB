//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#pragma once

#include <IOKit/IOKitLib.h>
#include <IOKit/usb/IOUSBLib.h>

#include <memory>
#include <string>
#include <vector>
#include <stdint.h>


/**
 * USB control request type.
 *
 * Bitmask values for direction (bit 7), type (bits 6..5) and recipient (bits 4..0)
 */
enum class usb_request_type {
    // bit 7 : direction
    /// Direction device to host
    direction_in = 0x80,
    /// Direction host to device
    direction_out = 0x00,
    // bits 6..5 : type
    /// Standard request
    type_standard = 0x00,
    /// Class-specific request
    type_class = 0x20,
    /// Vendor-defined request
    type_vendor = 0x40,
    // bits 4..0 : recipient
    /// Request for device
    recipient_device = 0x00,
    /// Request for interface
    recipient_interface = 0x01,
    /// Request for endpoint
    recipient_endpoint = 0x02,
    /// Request for other recipient
    recipient_other = 0x03
};


/**
 * USB control request structure.
 */
struct usb_control_request {
    /// Request type
    uint8_t bmRequestType;
    /// Specific request number
    uint8_t bRequest;
    /// Value (request specific)
    uint16_t wValue;
    /// Index (request specific)
    uint16_t wIndex;
    /// Number of bytes to transfer if there is a data stage
    uint16_t wLength;
    
    static uint8_t request_type(usb_request_type direction, usb_request_type type, usb_request_type recipient) {
        return static_cast<uint8_t>(direction) + static_cast<uint8_t>(type) + static_cast<uint8_t>(recipient);
    }
};

struct pipe_info {
    UInt8 pipe_index;
    uint8_t endpoint_address;
    uint8_t transfer_type;
};


/**
 * USB device.
 *
 * Must be used with `std::shared_ptr`.
 */
class usb_device {
public:
    ~usb_device();
    
    /// USB vendor ID
    int vendor_id() const { return vendor_id_; }
    /// USB product ID
    int product_id() const { return product_id_; }
    /// Manufacturer name
    std::string manufacturer() const { return manufacturer_; }
    /// Product name
    std::string product() const { return product_; }
    /// Serial number
    std::string serial_number() const { return serial_number_; }
    /// Descriptive string including VID, PID, manufacturer, product name and serial number
    std::string description() const;
    
    /// Opens the device for communication
    void open();
    
    /// Closes the device
    void close();
    
    /// Indicates if device is open
    bool is_open() const;
    
    /**
     * Claims an interface
     *
     * A single interface can be claimed.
     *
     * @param interface_number interface number
     */
    void claim_interface(int interface_number);
    
    /**
     * Releases the claimed interface.
     */
    void release_interface();
    
    /**
     * Select an alternate interface setting.
     *
     * The affected interface must have been claimed.
     *
     *@param alternate_setting alternate setting number
     */
    void select_alternate_interface(int alternate_setting);
    
    /**
     * Receives data from a bulk or interrupt endpoint.
     *
     * The amount of bytes read will be influced by the underlying USB packets.
     * If a short packet is sent, the function will return after having read fewer bytes
     * than specified. The function will fail if a bigger packet has been received than
     * will fit into the given buffer. So the specified data length should be big enough for
     * the maximum packet size (64 bytes for full-speed USB).
     *
     * The timeout specifies the maximum time it may take to complete the operation.
     * If the operation does not complete within that time, the function returns after reading
     * less or no data at all.
     *
     * Interrupt endpoints do not support timeouts. Thus, 0 has to be specified.
     *
     * @param endpoint_number endpoint number (between 1 and 127)
     * @param data_len maximum length to read (in bytes)
     * @param timeout timeout (in ms, 0 for no timeout)
     * @return received data
     */
    std::vector<uint8_t> transfer_in(int endpoint_number, int data_len, int timeout = 0);

    /**
     * Transmits data to a bulk or interrupt endpoint.
     *
     * The timeout specifies the maximum time it may take to complete the operation.
     * If the operation does not complete within that time, the function returns after writing
     * less or no data at all.
     *
     * Interrupt endpoints do not support timeouts. Thus, 0 has to be specified.
     *
     * @param endpoint_number endpoint number (between 1 and 127)
     * @param data data to transmit
     * @param timeout timeout (in ms, 0 for no timeout)
     */
    void transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int timeout = 0);
    
    /**
     * Send a control request with no Data phase.
     *
     * The request is always sent to endpoint 0.
     *
     * @param request request to send
     * @param timeout timeout (in ms, 0 for no timeout)
     */
    void control_transfer(const usb_control_request& request, int timeout = 0);

    /**
     * Send a control request with a DATA OUT phase.
     *
     * The request is always sent to endpoint 0.
     *
     * The length of the passed data vector and `request.wLength` must be equal.
     *
     * @param request request to send
     * @param data buffer containing data to be sent in DATA phase.
     * @param timeout timeout (in ms, 0 for no timeout)
     */
    void control_transfer_out(const usb_control_request& request, const std::vector<uint8_t>& data, int timeout = 0);

    /**
     * Send a control request with a DATA IN phase.
     *
     * The request is always sent to endpoint 0.
     *
     * The data is received in the specified data buffer. The length of buffer is specified in `request.wLength`.
     *
     * @param request request to send
     * @param timeout timeout (in ms, 0 for no timeout)
     * @return received data
     */
    std::vector<uint8_t> control_transfer_in(const usb_control_request& request, int timeout = 0);

private:
    usb_device(io_service_t service, IOUSBDeviceInterface** device, uint64_t entry_id, int vendor_id, int product_id);
    uint64_t entry_id() const { return entry_id_; }
    void build_pipe_info(IOUSBInterfaceInterface** intf);
    const pipe_info* get_pipe(int endpoint_address);
    const pipe_info* ep_in_pipe(int endpoint_address);
    const pipe_info* ep_out_pipe(int endpoint_address);
    int control_transfer_core(const usb_control_request& request, uint8_t* data, int timeout);

    uint64_t entry_id_;
    IOUSBDeviceInterface** device_;
    bool is_open_;
    IOUSBInterfaceInterface** interface_;
    std::vector<pipe_info> pipes_;

    int product_id_;
    int vendor_id_;
    std::string manufacturer_;
    std::string product_;
    std::string serial_number_;
    
    friend class usb_registry;
};

typedef std::shared_ptr<usb_device> usb_device_ptr;
