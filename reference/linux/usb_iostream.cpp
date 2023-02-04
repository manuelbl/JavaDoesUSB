//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Linux
//

#include "usb_iostream.hpp"
#include "usb_error.hpp"
#include <cstring>

// --- usb_istreambuf ---

usb_istreambuf::usb_istreambuf(usb_device_ptr device, int endpoint_number)
: device_(device), ep_num_(endpoint_number), is_closed_(false), submitted_index_(0), completed_index_(0), processed_index_(-1) {
    setg(nullptr, nullptr, nullptr);
    
    buffer_size_ = 8 * device->get_endpoint(usb_direction::in, endpoint_number).packet_size();

    // create completion handler lambda
    io_completion = [this]() { on_completed(); };

    // initialize the URBs
    for (int i = 0; i < num_outstanding_requests; i++) {
        usbdevfs_urb* urb = urbs_ + i;
        memset(urb, 0, sizeof(*urb));
        urb->type = USBDEVFS_URB_TYPE_BULK;
        urb->endpoint = 128 + ep_num_;
        urb->buffer = new uint8_t[buffer_size_];
        urb->buffer_length = buffer_size_;
        urb->usercontext = &io_completion;
    }
    
    // start requests except for one
    const std::lock_guard lock(io_mutex);
    for (int i = 0; i < num_outstanding_requests - 1; i++)
        submit_request();
}
         
usb_istreambuf::~usb_istreambuf() {
    close();

    // free buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        delete [] reinterpret_cast<uint8_t*>(urbs_[i].buffer);
}

void usb_istreambuf::close() {
    const std::lock_guard lock(io_mutex);

    is_closed_ = true;
    io_condition.notify_all();

    while (submitted_index_ > completed_index_) {
        submitted_index_ -= 1;
        int index = submitted_index_ % num_outstanding_requests;
        device_->cancel_urb(&urbs_[index]);
    }
}

void usb_istreambuf::submit_request() {
    int index = submitted_index_ % num_outstanding_requests;
    submitted_index_ += 1;

    device_->submit_urb(&urbs_[index]);
}

void usb_istreambuf::on_completed() {
    const std::lock_guard lock(io_mutex);
    if (is_closed_) {
        completed_index_ += 1;
        if (completed_index_ == submitted_index_)
            delete this;
        return;
    }
    
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
        usbdevfs_urb* urb = urbs_ + index;
        usb_error::check(urb->status, "error reading from USB endpoint");
        
        // set stream buffer to buffer from completed request
        char* buf = reinterpret_cast<char*>(urb->buffer);
        int size = urb->actual_length;
        
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
    
    buffer_size_ = 8 * device->get_endpoint(usb_direction::out, endpoint_number).packet_size();
    
    // create completion handler lambda
    io_completion = [this]() { on_completed(); };

    // initialize the URBs
    for (int i = 0; i < num_outstanding_requests; i++) {
        usbdevfs_urb* urb = urbs_ + i;
        memset(urb, 0, sizeof(urb));
        urb->type = USBDEVFS_URB_TYPE_BULK;
        urb->endpoint = ep_num_;
        urb->buffer = new uint8_t[buffer_size_];
        urb->buffer_length = buffer_size_;
        urb->usercontext = &io_completion;
    }
    
    char* buf = reinterpret_cast<char*>(urbs_[0].buffer);
    setp(buf, buf + buffer_size_);
}
         
usb_ostreambuf::~usb_ostreambuf() {
    // flush all data
    sync();

    // free buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        delete [] reinterpret_cast<uint8_t*>(urbs_[i].buffer);
}

int usb_ostreambuf::sync() {
    std::unique_lock wait_lock(io_mutex);

    // submit request if there is any data in the current buffer
    auto size = pptr() - pbase();
    if (size > 0) {
        int index = processing_index_ % num_outstanding_requests;
        usbdevfs_urb* urb = urbs_ + index;
        urb->buffer_length = size;
        device_->submit_urb(urb);
        processing_index_ += 1;
        needs_zlp_ = size == buffer_size_;
    }
    
    // send a zero-length packet if required
    if (needs_zlp_) {
        // wait until an unused buffer is available
        io_condition.wait(wait_lock, [this] { return processing_index_ - completed_index_ < num_outstanding_requests; });
        check_for_errors();
        
        int index = processing_index_ % num_outstanding_requests;
        usbdevfs_urb* urb = urbs_ + index;
        urb->buffer_length = 0;
        device_->submit_urb(urb);
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
    usbdevfs_urb* urb = urbs_ + index;
    urb->buffer_length = size;
    device_->submit_urb(urb);
    processing_index_ += 1;
    needs_zlp_ = size == buffer_size_;
    
    // wait until an unused buffer is available
    io_condition.wait(wait_lock, [this] { return processing_index_ - completed_index_ < num_outstanding_requests; });
    check_for_errors();
    
    // configure stream buffer
    index = processing_index_ % num_outstanding_requests;
    urb = urbs_ + index;
    char* buf = reinterpret_cast<char*>(urb->buffer);
    setp(buf, buf + buffer_size_);

    // insert char
    if (c != traits_type::eof()) {
        *pptr() = (char)c;
        pbump( 1 );
    }
    
    return c;
}

void usb_ostreambuf::on_completed() {
    const std::lock_guard lock(io_mutex);
    completed_index_ += 1;
    io_condition.notify_all();
}

void usb_ostreambuf::check_for_errors() {
    while (completed_index_ - checked_index_ > 0) {
        int index = completed_index_ % num_outstanding_requests;
        usbdevfs_urb* urb = urbs_ + index;
        usb_error::check(urb->status, "error writing to USB endpoint");
        checked_index_ += 1;
    }
}


// --- usb_istream ---

usb_istream::usb_istream(usb_device_ptr device, int ep_num)
    : std::istream(new usb_istreambuf(device, ep_num)) {}

usb_istream::~usb_istream() {
    // delete buffer
    delete rdbuf();
}


// --- usb_ostream ---

usb_ostream::usb_ostream(usb_device_ptr device, int ep_num)
    : std::ostream(new usb_ostreambuf(device, ep_num)) {}

usb_ostream::~usb_ostream() {
    // delete buffer
    delete rdbuf();
}
