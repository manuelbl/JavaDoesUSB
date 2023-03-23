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
: device(device), endpoint_number(endpoint_number), is_closed(false), num_outstanding_requests(0) {

    setg(nullptr, nullptr, nullptr);
    
    buffer_size = 8 * device->get_endpoint(usb_direction::in, endpoint_number).packet_size();

    // allocate the buffers and submit requests
    memset(requests, 0, sizeof(requests));
    for (int i = 0; i < max_outstanding_requests; i++) {
        transfer_request* request = &requests[i];
        request->io_completion = [this, request]() { on_completed(request); };
        request->urb.type = USBDEVFS_URB_TYPE_BULK;
        request->urb.endpoint = 128 + endpoint_number;
        request->urb.buffer = new uint8_t[buffer_size];
        request->urb.buffer_length = buffer_size;
        request->urb.usercontext = &request->io_completion;

        if (i == 0)
            current_request = request;
        else
            submit_transfer(request);
    }
}
         
usb_istreambuf::~usb_istreambuf() {
    close();

    // free buffers
    for (int i = 0; i < max_outstanding_requests; i++)
        delete[] reinterpret_cast<uint8_t*>(requests[i].urb.buffer);
}

void usb_istreambuf::close() {
    is_closed = true;
    setg(nullptr, nullptr, nullptr);

    // cancel outstanding requests
    for (int i = 0; i < max_outstanding_requests; i++) {
        if (!requests[i].is_completed)
            device->cancel_urb(&requests[i].urb);
    }

    // wait until completion handlers have been called
    while (num_outstanding_requests > 0)
        wait_for_request_completion();
}

void usb_istreambuf::submit_transfer(transfer_request* request) {
    request->is_completed = false;
    device->submit_urb(&request->urb);
    num_outstanding_requests += 1;
}

void usb_istreambuf::on_completed(transfer_request* request) {
    request->is_completed = true;
    completed_request_queue.put(request);
}

usb_istreambuf::transfer_request* usb_istreambuf::wait_for_request_completion() {
    transfer_request* request = completed_request_queue.take();
    num_outstanding_requests -= 1;
    return request;
}

usb_istreambuf::int_type usb_istreambuf::underflow() {
    if (is_closed)
        return traits_type::eof();

    if (gptr() < egptr())
        return traits_type::to_int_type(*gptr());

    // loop until non-ZLP has been received
    do {
        submit_transfer(current_request);

        current_request = wait_for_request_completion();
        usb_error::check(current_request->result_code(), "error reading from USB endpoint");

        char* buf = reinterpret_cast<char*>(current_request->urb.buffer);
        int size = current_request->result_size();
        setg(buf, buf, buf + size);

    } while (current_request->result_size() == 0);

    return traits_type::to_int_type(*gptr());
}


// --- usb_ostreambuf ---

usb_ostreambuf::usb_ostreambuf(usb_device_ptr device, int endpoint_number)
: device(device), endpoint_number(endpoint_number), is_closed(false), needs_zlp(false) {
    
    packet_size = device->get_endpoint(usb_direction::out, endpoint_number).packet_size();
    buffer_size = 1 * packet_size;

    // create requests
    memset(requests, 0, sizeof(requests));
    for (int i = 0; i < max_outstanding_requests; i++) {
        transfer_request* request = &requests[i];
        request->io_completion = [this, request](void) { on_completed(request); };
        request->urb.type = USBDEVFS_URB_TYPE_BULK;
        request->urb.endpoint = endpoint_number;
        request->urb.buffer = new uint8_t[buffer_size];
        request->urb.usercontext = &request->io_completion;
    }

    fill_queue();
}

usb_ostreambuf::~usb_ostreambuf() {
    // flush all data
    sync();

    // free buffers
    for (int i = 0; i < max_outstanding_requests; i++)
        delete[] reinterpret_cast<uint8_t*>(requests[i].urb.buffer);
}

void usb_ostreambuf::fill_queue() {
    for (int i = 1; i < max_outstanding_requests; i++)
        available_request_queue.put(&requests[i]);

    // configure stream buffer for first request
    current_request = &requests[0];
    char* buf = reinterpret_cast<char*>(current_request->urb.buffer);
    setp(buf, buf + buffer_size);
}
         
int usb_ostreambuf::sync() {
    // submit request if there is any data in the current buffer
    auto size = pptr() - pbase();
    if (size > 0)
        submit_transfer((int)size);

    // send a zero-length packet if required
    if (needs_zlp)
        submit_transfer(0);

    // Wait until all buffers have been transmitted by removing them from the
    // queue and reinserting them. One request is the current request.
    // So the queue only contains max_outstanding_requests - 1 requests.
    for (int i = 0; i < max_outstanding_requests - 1; i++)
        wait_for_available_transfer();

    fill_queue();

    return 0;
}

int usb_ostreambuf::overflow (int c) {
    // submit request
    auto size = pptr() - pbase();
    submit_transfer((int)size);

    // insert char
    if (c != traits_type::eof()) {
        *pptr() = (char)c;
        pbump(1);
    }

    return c;
}

void usb_ostreambuf::submit_transfer(int size) {
    current_request->urb.buffer_length = size;
    device->submit_urb(&current_request->urb);
    needs_zlp = size == packet_size;

    current_request = wait_for_available_transfer();

    // configure stream buffer
    char* buf = reinterpret_cast<char*>(current_request->urb.buffer);
    setp(buf, buf + buffer_size);
}

void usb_ostreambuf::on_completed(transfer_request* request) {
    available_request_queue.put(request);
}

usb_ostreambuf::transfer_request* usb_ostreambuf::wait_for_available_transfer() {
    auto request = available_request_queue.take();

    // check for error
    usb_error::check(request->result_code(), "error writing to USB endpoint");

    return request;
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
