//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma  once

#include <iostream>
#include <mutex>
#include <vector>
#include "usb_device.hpp"

/**
 * Input stream buffer for USB bulk or interrupt endpoint.
 *
 * The stream buffer is internally used by an input stream. It submits multiple asynchronous IO requests to
 * achieve maximum throughput.
 */
class usb_istreambuf : public std::streambuf {
public:
    /// Constructor
    usb_istreambuf(usb_device_ptr device, int ep_num);
    /// Destructor
    virtual ~usb_istreambuf();
    
protected:
    /// Called when the internal buffer has no further data to read.
    virtual int_type underflow();

private:
    void submit_request();

    void close();
    void on_io_completion(DWORD result, DWORD size);

    /// Maximum number of concurrently outstanding requests
    static constexpr int num_outstanding_requests = 4;
    
    /// USB device
    usb_device_ptr device;
    /// endpoint number
    int endpoint_number;
    /// Indicates that this stream buffer is closed
    bool is_closed;
    /// buffer size
    int buffer_size;
    /// Mutex for synchronization between input stream caller and background thread handling IO completion.
    std::mutex io_mutex;
    /// Condition for synchronization between input stream caller and background thread handling IO completion
    std::condition_variable io_condition;
    /// Index after last submitted IO request
    int submitted_index;
    /// Index after last completed IO request
    int completed_index;
    /// Index after last processed IO request
    int processed_index;
    /// Async IO request information
    OVERLAPPED overlapped_requests[num_outstanding_requests];
    /// Data buffers
    uint8_t* buffers[num_outstanding_requests];
    /// Size of received data
    DWORD request_sizes[num_outstanding_requests];
    /// Result code of IO request
    DWORD request_results[num_outstanding_requests];
    /// Async IO completion handler
    std::function<void(DWORD result, DWORD size)> io_completion_handler;

    friend class usb_istream;
};

/**
 * Output stream buffer for USB bulk or interrupt endpoint.
 *
 * The stream buffer is internally used by an output stream. It submits multiple asynchronous IO requests to
 * achieve maximum throughput.
 */
class usb_ostreambuf : public std::streambuf {
public:
    /// Constructor
    usb_ostreambuf(usb_device_ptr device, int ep_num);
    /// Destructor
    virtual ~usb_ostreambuf();
    
    virtual int sync();
    
protected:
    /// Called when the internal buffer has no space left to add more data.
    virtual int overflow (int c);

private:
    void submit_transfer(int size);
    void check_for_errors();

    void on_io_completion(DWORD result, DWORD size);

    /// Maximum number of concurrently outstanding requests
    static constexpr int num_outstanding_requests = 4;
    
    /// USB device
    usb_device_ptr device;
    /// endpoint number
    int endpoint_number;
    /// Indicates if a zero-length packet is required
    bool needs_zlp;
    /// packet size
    int packet_size;
    /// buffer size
    int buffer_size;
    /// Mutex for synchronization between output stream caller and background thread handling IO completion.
    std::mutex io_mutex;
    /// Condition for synchronization between output stream caller and background thread handling IO completion
    std::condition_variable io_condition;
    /// Index of buffer being currently used by base class
    int processing_index;
    /// Index after last completed IO request
    int completed_index;
    /// Index after last completed IO request that has been checked for errors
    int checked_index;
    /// Async IO request information
    OVERLAPPED overlapped_requests[num_outstanding_requests];
    /// Data buffers for transmitting data
    uint8_t* buffers[num_outstanding_requests];
    /// Result code of IO request
    DWORD request_results[num_outstanding_requests];
    /// Async IO completion handler
    std::function<void(DWORD result, DWORD size)> io_completion_handler;
};

/**
 * Input stream for reading from a USB bulk endpoint
 */
class usb_istream : public std::istream {
public:
    /// Constructor
    usb_istream(usb_device_ptr device, int ep_num);
    /// Destructor
    ~usb_istream();
};

/**
 * Output stream for writing to a USB bulk endpoint
 */
class usb_ostream : public std::ostream {
public:
    /// Constructor
    usb_ostream(usb_device_ptr device, int ep_num);
    /// Destructor
    ~usb_ostream();
};
