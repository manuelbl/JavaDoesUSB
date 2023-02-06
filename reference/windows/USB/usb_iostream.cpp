//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#include "usb_iostream.hpp"
#include "usb_error.hpp"
#include <cassert>

// --- usb_istreambuf ---

usb_istreambuf::usb_istreambuf(usb_device_ptr device, int endpoint_number)
: device(device), endpoint_number(endpoint_number), is_closed(false), submitted_index(0), completed_index(0), processed_index(-1) {

    setg(nullptr, nullptr, nullptr);

    device->configure_for_async_io(usb_direction::in, endpoint_number);
    
    buffer_size = 4 * device->get_endpoint(usb_direction::in, endpoint_number).packet_size();

    // create buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        buffers[i] = new uint8_t[buffer_size];
    
    // initialize OUTLAPPED structure and submit request
    memset(overlapped_requests, 0, sizeof(overlapped_requests));

    // create and register IO completion handler
    io_completion_handler = [this](DWORD result, DWORD size) { on_io_completion(result, size); };
    for (int i = 0; i < num_outstanding_requests; i++)
        device->add_completion_handler(&overlapped_requests[i], &io_completion_handler);

    // start requests except for one
    const std::lock_guard lock(io_mutex);
    for (int i = 0; i < num_outstanding_requests - 1; i++)
        submit_request();
}
         
usb_istreambuf::~usb_istreambuf() {
    close();

    for (int i = 0; i < num_outstanding_requests; i++)
        delete[] buffers[i];
}

void usb_istreambuf::close() {
    {
        const std::lock_guard lock(io_mutex);

        is_closed = true;

        for (int i = completed_index; submitted_index - i > 0; i += 1) {
            int index = i % num_outstanding_requests;
            device->cancel_transfer(usb_direction::in, endpoint_number, &overlapped_requests[index]);
        }
    }

    io_condition.notify_all();

    // wait until all completion handlers have been called
    std::unique_lock wait_lock(io_mutex);
    io_condition.wait(wait_lock, [this] { return completed_index == submitted_index; });
}

void usb_istreambuf::submit_request() {
    int index = submitted_index % num_outstanding_requests;    
    submitted_index += 1;

    device->submit_transfer_in(endpoint_number, buffers[index], buffer_size, &overlapped_requests[index]);
}

void usb_istreambuf::on_io_completion(DWORD result, DWORD size) {
    const std::lock_guard lock(io_mutex);

    int index = completed_index % num_outstanding_requests;
    request_sizes[index] = size;
    request_results[index] = result;

    completed_index += 1;
    io_condition.notify_all();
}

usb_istreambuf::int_type usb_istreambuf::underflow() {
    if (is_closed)
        return traits_type::eof();

    if (gptr() < egptr())
        return traits_type::to_int_type(*gptr());

    // loop until no ZLP has been received
    while (true) {
        std::unique_lock wait_lock(io_mutex);
        processed_index += 1;
        submit_request();

        // wait until a completed request is available
        io_condition.wait(wait_lock, [this] { return completed_index - processed_index > 0 || is_closed; });

        if (is_closed)
            return traits_type::eof();

        int index = processed_index % num_outstanding_requests;
        if (request_results[index] != S_OK)
            throw new usb_error("transfer IN failed", request_results[index]);

        // set stream buffer to buffer from completed request
        char* buf = reinterpret_cast<char*>(buffers[index]);
        int size = request_sizes[index];

        if (size != 0) {
            setg(buf, buf, buf + size);
            return traits_type::to_int_type(*gptr());
        }
    }
}


// --- usb_ostreambuf ---

usb_ostreambuf::usb_ostreambuf(usb_device_ptr device, int endpoint_number)
: device(device), endpoint_number(endpoint_number), processing_index(0), completed_index(0), checked_index(0), needs_zlp(false) {
    
    device->configure_for_async_io(usb_direction::out, endpoint_number);

    packet_size = device->get_endpoint(usb_direction::out, endpoint_number).packet_size();
    buffer_size = 1 * packet_size;

    // create buffers
    for (int i = 0; i < num_outstanding_requests; i++)
        buffers[i] = new uint8_t[buffer_size];

    char* buf = reinterpret_cast<char*>(buffers[0]);
    setp(buf, buf + buffer_size);

    // initialize OUTLAPPED structure and submit request
    memset(overlapped_requests, 0, sizeof(overlapped_requests));

    // create and register IO completion handler
    io_completion_handler = [this](DWORD result, DWORD size) { on_io_completion(result, size); };
    for (int i = 0; i < num_outstanding_requests; i++)
        device->add_completion_handler(&overlapped_requests[i], &io_completion_handler);
}
         
usb_ostreambuf::~usb_ostreambuf() {
    sync();

    for (int i = 0; i < num_outstanding_requests; i++)
        delete[] buffers[i];
}

int usb_ostreambuf::sync() {
    std::unique_lock wait_lock(io_mutex);

    // submit request if there is any data in the current buffer
    auto size = static_cast<int>(pptr() - pbase());
    if (size > 0)
        submit_transfer(size);
    
    // send a zero-length packet if required
    if (needs_zlp) {
        // wait until an unused buffer is available
        io_condition.wait(wait_lock, [this] { return processing_index - completed_index < num_outstanding_requests; });
        check_for_errors();

        submit_transfer(0);
    }

    // wait until all buffers have been transmitted
    io_condition.wait(wait_lock, [this] { return processing_index == completed_index; });
    check_for_errors();

    int index = processing_index % num_outstanding_requests;
    char* buf = reinterpret_cast<char*>(buffers[index]);
    setp(buf, buf + buffer_size);

    return 0;
}

int usb_ostreambuf::overflow (int c) {
    std::unique_lock wait_lock(io_mutex);

    // submit request
    auto size = static_cast<int>(pptr() - pbase());
    submit_transfer(size);

    // wait until an unused buffer is available
    io_condition.wait(wait_lock, [this] { return processing_index - completed_index < num_outstanding_requests; });
    check_for_errors();

    // configure stream buffer
    int index = processing_index % num_outstanding_requests;
    char* buf = reinterpret_cast<char*>(buffers[index]);
    setp(buf, buf + buffer_size);

    // insert char
    if (c != traits_type::eof()) {
        *pptr() = (char)c;
        pbump(1);
    }

    return c;
}

void usb_ostreambuf::on_io_completion(DWORD result, DWORD size) {
    const std::lock_guard lock(io_mutex);
    int index = completed_index % num_outstanding_requests;
    request_results[index] = result;
    completed_index += 1;
    io_condition.notify_all();
}

void usb_ostreambuf::check_for_errors() {

    while (completed_index - checked_index > 0) {
        int index = completed_index % num_outstanding_requests;
        if (request_results[index] != S_OK)
            throw new usb_error("transfer OUT failed", request_results[index]);
        checked_index += 1;
    }
}

void usb_ostreambuf::submit_transfer(int size) {
    int index = processing_index % num_outstanding_requests;
    device->submit_transfer_out(endpoint_number, buffers[index], size, &overlapped_requests[index]);
    processing_index += 1;
    needs_zlp = size == packet_size;
}


// --- usb_istream ---

usb_istream::usb_istream(usb_device_ptr device, int ep_num)
    : std::istream(new usb_istreambuf(device, ep_num)) {}

usb_istream::~usb_istream() {
    // deallocate stream buffer
    delete rdbuf();
}


// --- usb_ostream ---

usb_ostream::usb_ostream(usb_device_ptr device, int ep_num)
    : std::ostream(new usb_ostreambuf(device, ep_num)) {}

usb_ostream::~usb_ostream() {
    // deallocate stream buffer
    delete rdbuf();
}
