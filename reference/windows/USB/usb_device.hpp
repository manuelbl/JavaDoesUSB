//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include <functional>
#include <iostream>
#include <map>
#include <memory>
#include <string>
#include <vector>
#include <stdint.h>

#include <Windows.h>
#undef min
#undef max
#undef LowSpeed
#include <winusb.h>

#include "configuration.hpp"

typedef std::function<void(void)> usb_io_callback;

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


class usb_registry;

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
    /// List of interfaces
    const std::vector<usb_interface>& interfaces() const;

    /**
     * Get the USB interface.
     *
     * @param interface_number interface number
     * @return interface or `nullptr` if no such interface exists
     */
    const usb_interface& get_interface(int interface_number) const;
    
    /**
     * Get a USB endpoint.
     *
     * @param direction endpoint direction
     * @param endpoint_number endpoint number (between 1 and 127)
     * @return endpoint or `nullptr` if endpoint does not exist
     */
    const usb_endpoint& get_endpoint(usb_direction direction, int endpoint_number) const;

    /// Opens the device for communication
    void open();
    
    /// Closes the device
    void close();
    
    /// Indicates if device is open
    bool is_open() const;
    
    /**
     * Claims an interface
     *
     * @param interface_number interface number
     */
    void claim_interface(int interface_number);
    
    /**
     * Releases a claimed interface.
     *
     * @param interface_number interface number
     */
    void release_interface(int interface_number);
    
    /**
     * Receives data from a bulk or interrupt endpoint.
     *
     * The amount of bytes read will be influced by the underlying USB packets.
     * It can be 0 (if the device sends a ZLP) up to the maximum packet size.
     *
     * The timeout specifies the maximum time it may take to complete the operation.
     * If the operation does not complete within that time, the function returns after reading
     * less or no data at all.
     *
     * Interrupt endpoints do not support timeouts. Thus, 0 has to be specified.
     *
     * @param endpoint_number endpoint number (between 1 and 127)
     * @param timeout timeout (in ms, 0 for no timeout)
     * @return received data
     */
    std::vector<uint8_t> transfer_in(int endpoint_number, int timeout = 0);

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
     * @param len data length, in bytes (-1 for entire data vector)
     * @param timeout timeout (in ms, 0 for no timeout)
     */
    void transfer_out(int endpoint_number, const std::vector<uint8_t>& data, int len = -1, int timeout = 0);
    
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

    /**
     * Open a new input stream for a bulk endpoint.
     *
     * The input stream is optimized for maximum throughput.
     *
     * Do not use the input stream concurrently with other transfer operations on the same endpoint. The input stream
     * buffers data for high throughput. When the stream is closed, any data in the buffers will be lost.
     *
     * @param endpoint_number endpoint number (between 1 and 127)
     */
    std::unique_ptr<std::istream> open_input_stream(int endpoint_number);

    /**
     * Open a new output stream for a bulk endpoint.
     *
     * The output stream is optimized for maximum throughput.
     *
     * Do not use the output stream concurrently with other transfer operations on the same endpoint.
     *
     * @param endpoint_number endpoint number (between 1 and 127)
     */
    std::unique_ptr<std::ostream> open_output_stream(int endpoint_number);

private:
    struct interface_handle {
        int interface_num;
        int first_interface_num;
        HANDLE device_handle;
        WINUSB_INTERFACE_HANDLE winusb_handle;
        int device_open_count;

        interface_handle(int intf_num, int first_num, std::wstring&& path);
    };

    usb_device(usb_registry* registry, std::wstring&& device_path, int vendor_id, int product_id, const std::vector<uint8_t>& config_desc);
    void set_product_names(const std::string& manufacturer, const std::string& product, const std::string& serial_number);
    void build_handles(const std::wstring& device_path);
    std::wstring get_interface_device_path(int interface_num);
    int get_child_device_path(const std::wstring& child_id, int interface_num, std::wstring& device_path);
    static int extract_interface_number(const std::vector<std::wstring>& hardware_ids);

    int control_transfer_core(const usb_control_request& request, uint8_t* data, int timeout);
    usb_composite_function* get_function(int intf_number);

    interface_handle* get_interface_handle(int intf_number);
    usb_interface* get_intf_ptr(int intf_number);
    usb_interface* get_endpoint_interface(usb_direction direction, int endpoint_number);
    const usb_endpoint* get_endpoint_ptr(usb_direction direction, int endpoint_number);
    interface_handle* get_control_transfer_interface_handle(const usb_control_request& request);
    interface_handle* check_valid_endpoint(usb_direction direction, int endpoint_number);
    static uint8_t ep_address(usb_direction direction, int endpoint_number) {
        return static_cast<uint8_t>(direction) | static_cast<uint8_t>(endpoint_number);
    }

    void configure_for_async_io(usb_direction direction, int endpoint_number);
    void add_completion_handler(OVERLAPPED* overlapped, usb_io_callback* completion_handler);
    void remove_completion_handler(OVERLAPPED* overlapped);
    void submit_transfer_in(int endpoint_number, uint8_t* buffer, int buffer_len, OVERLAPPED* overlapped);
    void submit_transfer_out(int endpoint_number, uint8_t* data, int data_len, OVERLAPPED* overlapped);
    void cancel_transfer(usb_direction direction, int endpoint_number, OVERLAPPED* overlapped);

    usb_registry* registry_;
    bool is_open_;

    int product_id_;
    int vendor_id_;
    std::wstring device_path_;
    std::string manufacturer_;
    std::string product_;
    std::string serial_number_;

    std::vector<usb_interface> interfaces_;
    std::vector<usb_composite_function> functions_;
    std::vector<interface_handle> interface_handles_;
    std::map<int, std::wstring> interface_device_paths_;

    friend class usb_registry;
    friend class usb_istreambuf;
    friend class usb_ostreambuf;
};

typedef std::shared_ptr<usb_device> usb_device_ptr;
