//
// Java Does USB
// Loopback device for testing
//
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Circular buffer for raw binary data.
//
// The circular buffer allows a reader and writer to
// use the buffer concurrently.
//

#pragma once

#include <stdint.h>
#include <string.h>

#include <algorithm>

/**
 * Circular buffer for raw binary data.
 *
 * The circular buffer allows a reader and writer to
 * use the buffer concurrently.
 *
 * @param N number of bytes that fit into the buffer
 */
template <int N>
struct circ_buf {
   private:
    static constexpr int BUF_SIZE = N + 1;

    // 0 <= head < BUF_SIZE
    // 0 <= tail < BUF_SIZE
    // head == tail: buffer is empty
    // Therefore, the buffer must never be filled completely.
    volatile int buf_head = 0;  // updated when adding data
    volatile int buf_tail = 0;  // updated when removing data

    uint8_t buffer[BUF_SIZE];

   public:
    /// Creates a new instance
    circ_buf();

    /// Returns the maximum number of bytes that can be added to the buffer
    int avail_size();

    /// Returns the number of bytes in the buffer
    int data_size();

    /**
     * Gets the oldest data from the buffer and removes it.
     * @param buf buffer to copy data to
     * @param max_len maximum number of bytes to copy
     * @return the effective number of bytes
     */
    int get_data(uint8_t *buf, int max_len);

    /**
     * Adds data to the buffer
     *
     * @param buf the buffer with the data
     * @param len the number of bytes to add
     */
    void add_data(const uint8_t *buf, int len);

    /// Resets (empties) the circular buffer
    void reset();
};

template <int N>
circ_buf<N>::circ_buf() : buf_head(0), buf_tail(0) {}

template <int N>
int circ_buf<N>::avail_size() {
    int head = buf_head;
    int tail = buf_tail;

    if (head >= tail) {
        return BUF_SIZE - (head - tail) - 1;
    } else {
        return tail - head - 1;
    }
}

template <int N>
int circ_buf<N>::data_size() {
    int head = buf_head;
    int tail = buf_tail;

    if (head >= tail) {
        return head - tail;
    } else {
        return BUF_SIZE - (tail - head);
    }
}

template <int N>
int circ_buf<N>::get_data(uint8_t *buf, int max_len) {
    int tail = buf_tail;
    int head = buf_head;

    if (tail == head)
        return 0;

    // get available data (without wrap around)
    int len = (head > tail ? head : BUF_SIZE) - tail;

    // limit data to max_len
    len = std::min(len, max_len);

    // copy data
    memcpy(buf, buffer + tail, len);

    // update tail
    tail += len;
    if (tail >= BUF_SIZE)
        tail -= BUF_SIZE;
    buf_tail = tail;

    // sufficient data or no more data
    if (len == max_len || tail != 0)
        return len;

    // copy more data
    return get_data(buf + len, max_len - len) + len;
}

template <int N>
void circ_buf<N>::add_data(const uint8_t *buf, int len) {
    int head = buf_head;

    // copy first part (from head to end of circular buffer)
    int n = std::min(len, BUF_SIZE - head);
    memcpy(buffer + head, buf, n);

    // copy second part if needed (to start of circular buffer)
    if (n < len)
        memcpy(buffer, buf + n, len - n);

    // update head
    head += len;
    if (head >= BUF_SIZE)
        head -= BUF_SIZE;
    buf_head = head;
}

template <int N>
void circ_buf<N>::reset() {
    buf_head = 0;
    buf_tail = 0;
}
