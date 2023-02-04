//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Linux
//

#pragma  once

#include <condition_variable>
#include <functional>
#include <iostream>
#include <mutex>
#include <linux/usbdevice_fs.h>
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
    
    /// Closes this buffer. This stream buffer will automatically free itself.
    void close();
    
protected:
    /// Called when the internal buffer has no further data to read.
    virtual int_type underflow();
    
    void on_completed();
    
private:
    void submit_request();

    /// Maximum number of concurrently outstanding requests
    static constexpr int num_outstanding_requests = 4;
    
    /// USB device
    usb_device_ptr device_;
    /// endpoint number
    int ep_num_;
    /// Indicates that this stream buffer is closed
    bool is_closed_;
    /// buffer size
    int buffer_size_;
    /// Mutex for synchronization between input stream caller and background thread handling IO completion.
    std::mutex io_mutex;
    /// Condition for synchronization between input stream caller and background thread handling IO completion
    std::condition_variable io_condition;
    /// Index after last submitted IO request
    unsigned int submitted_index_;
    /// Index after last completed IO request
    unsigned int completed_index_;
    /// Index after last processed IO request
    unsigned int processed_index_;
    /// USB request buffers
    usbdevfs_urb urbs_[num_outstanding_requests];
    /// IO completion lambda
    std::function<void()> io_completion;
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
    
    void on_completed();
    
    void check_for_errors();
    
private:
    /// Maximum number of concurrently outstanding requests
    static constexpr int num_outstanding_requests = 4;
    
    /// USB device
    usb_device_ptr device_;
    /// endpoint number
    int ep_num_;
    /// Indicates that this stream buffer is closed
    bool is_closed_;
    /// Indicates if a zero-length packet is required
    bool needs_zlp_;
    /// buffer size
    int buffer_size_;
    /// Mutex for synchronization between output stream caller and background thread handling IO completion.
    std::mutex io_mutex;
    /// Condition for synchronization between output stream caller and background thread handling IO completion
    std::condition_variable io_condition;
    /// Index of buffer being currently used by base class
    unsigned int processing_index_;
    /// Index after last completed IO request
    unsigned int completed_index_;
    /// Index after last completed IO request that has been checked for errors
    unsigned int checked_index_;
    /// USB request buffers
    usbdevfs_urb urbs_[num_outstanding_requests];
    /// IO completion lambda
    std::function<void()> io_completion;
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
