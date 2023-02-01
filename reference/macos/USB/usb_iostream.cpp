//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "usb_iostream.hpp"
#include "usb_error.hpp"

// --- usb_istreambuf ---

usb_istreambuf::usb_istreambuf(usb_device_ptr device, int endpoint_number)
: device_(device), ep_num_(endpoint_number), is_closed_(false), submitted_index_(0), completed_index_(0), processed_index_(-1) {
    setg(nullptr, nullptr, nullptr);
    
    packet_size_ = device->get_endpoint(usb_direction::in, endpoint_number).packet_size();
    
    // allocate the buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        request_buffers[i] = new uint8_t[packet_size_];
    
    // create completion handler lambda
    io_completion = [this](IOReturn result, int size) { on_completed(result, size); };
    
    // start requests except for one
    const std::lock_guard lock(io_mutex);
    for (int i = 0; i < num_outstanding_requests - 1; i++)
        submit_request();
}
         
usb_istreambuf::~usb_istreambuf() {
    // free buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        delete [] request_buffers[i];
}

void usb_istreambuf::close() {
    is_closed_ = true;
    io_condition.notify_all();
    device_->abort_transfer(usb_direction::in, ep_num_);
}

void usb_istreambuf::submit_request() {
    int index = submitted_index_ % num_outstanding_requests;
    submitted_index_ += 1;
    
    device_->submit_transfer_in(ep_num_, request_buffers[index], packet_size_, io_completion);
}

void usb_istreambuf::on_completed(IOReturn result, int size) {
    const std::lock_guard lock(io_mutex);
    if (is_closed_) {
        completed_index_ += 1;
        if (completed_index_ == submitted_index_)
            delete this;
        return;
    }
    
    int index = completed_index_ % num_outstanding_requests;
    request_sizes[index] = size;
    request_results[index] = result;
    
    completed_index_ += 1;
    io_condition.notify_all();
}

usb_istreambuf::int_type usb_istreambuf::underflow() {
    if (is_closed_)
        return traits_type::eof();
    
    if (gptr() < egptr())
        return traits_type::to_int_type(*gptr());
    
    // loop until no ZLP has been received
    while (true) {
        std::unique_lock wait_lock(io_mutex);
        processed_index_ += 1;
        submit_request();
        
        // wait until a completed request is available
        io_condition.wait(wait_lock, [this] { return completed_index_ - processed_index_ > 0 || is_closed_; });
        
        if (is_closed_)
            return traits_type::eof();
        
        int index = processed_index_ % num_outstanding_requests;
        usb_error::check(request_results[index], "error reading from USB endpoint");
        
        // set stream buffer to buffer from completed request
        char* buf = reinterpret_cast<char*>(request_buffers[index]);
        int size = request_sizes[index];
        
        if (size != 0) {
            setg(buf, buf, buf + size);
            return traits_type::to_int_type(*gptr());
        }
    }
}


// --- usb_ostreambuf ---

usb_ostreambuf::usb_ostreambuf(usb_device_ptr device, int endpoint_number)
: device_(device), ep_num_(endpoint_number), is_closed_(false), processing_index_(0),
    completed_index_(0), checked_index_(0), needs_zlp_(false) {
    
    packet_size_ = device->get_endpoint(usb_direction::out, endpoint_number).packet_size();
    
    // allocate the buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        request_buffers[i] = new uint8_t[packet_size_];
    
    char* buf = reinterpret_cast<char*>(request_buffers[0]);
    setp(buf, buf + packet_size_);
    
    // create completion handler lambda
    io_completion = [this](IOReturn result, int size) { on_completed(result, size); };
}
         
usb_ostreambuf::~usb_ostreambuf() {
    // free buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        delete [] request_buffers[i];
}

int usb_ostreambuf::sync() {
    std::unique_lock wait_lock(io_mutex);

    // submit request if there is any data in the current buffer
    auto size = pptr() - pbase();
    if (size > 0) {
        int index = processing_index_ % num_outstanding_requests;
        device_->submit_transfer_out(ep_num_, request_buffers[index], (int)size, io_completion);
        processing_index_ += 1;
        needs_zlp_ = size == packet_size_;
    }
    
    // send a zero-length packet if required
    if (needs_zlp_) {
        // wait until an unused buffer is available
        io_condition.wait(wait_lock, [this] { return processing_index_ - completed_index_ < num_outstanding_requests; });
        check_for_errors();
        
        device_->submit_transfer_out(ep_num_, nullptr, 0, io_completion);
        processing_index_ += 1;
        needs_zlp_ = false;
    }

    // wait until all buffers have been transmitted
    io_condition.wait(wait_lock, [this] { return processing_index_ == completed_index_; });
    check_for_errors();
    
    return 0;
}

int usb_ostreambuf::overflow (int c) {
    std::unique_lock wait_lock(io_mutex);

    // submit request
    auto size = pptr() - pbase();
    int index = processing_index_ % num_outstanding_requests;
    device_->submit_transfer_out(ep_num_, request_buffers[index], (int)size, io_completion);
    processing_index_ += 1;
    needs_zlp_ = size == packet_size_;
    
    // wait until an unused buffer is available
    io_condition.wait(wait_lock, [this] { return processing_index_ - completed_index_ < num_outstanding_requests; });
    check_for_errors();
    
    // configure stream buffer
    index = processing_index_ % num_outstanding_requests;
    char* buf = reinterpret_cast<char*>(request_buffers[index]);
    setp(buf, buf + packet_size_);

    // insert char
    if (c != traits_type::eof()) {
        *pptr() = (char)c;
        pbump( 1 );
    }
    
    return c;
}

void usb_ostreambuf::on_completed(IOReturn result, int size) {
    const std::lock_guard lock(io_mutex);
    int index = completed_index_ % num_outstanding_requests;
    request_results[index] = result;    
    completed_index_ += 1;
    io_condition.notify_all();
}

void usb_ostreambuf::check_for_errors() {
    while (completed_index_ - checked_index_ > 0) {
        int index = completed_index_ % num_outstanding_requests;
        usb_error::check(request_results[index], "error writing to USB endpoint");
        checked_index_ += 1;
    }
}


// --- usb_istream ---

usb_istream::usb_istream(usb_device_ptr device, int ep_num)
    : std::istream(new usb_istreambuf(device, ep_num)) {}

usb_istream::~usb_istream() {
    // close buffer (it will deallocate itself)
    static_cast<usb_istreambuf*>(rdbuf())->close();
}


// --- usb_ostream ---

usb_ostream::usb_ostream(usb_device_ptr device, int ep_num)
    : std::ostream(new usb_ostreambuf(device, ep_num)) {}

usb_ostream::~usb_ostream() {
    // flush all data
    static_cast<usb_ostreambuf*>(rdbuf())->pubsync();
}
