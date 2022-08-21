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

#include "circ_buf.h"

#include <stdint.h>
#include <string.h>

#define BUF_SIZE 1025

// 0 <= head < BUF_SIZE
// 0 <= tail < BUF_SIZE
// head == tail: buffer is empty
// Therefore, the buffer must never be filled completely.
static volatile int buf_head = 0;  // updated when adding data
static volatile int buf_tail = 0;  // updated when removing data

static uint8_t buffer[BUF_SIZE];

int circ_buf_avail_size() {
    int head = buf_head;
    int tail = buf_tail;

    if (head >= tail) {
        return BUF_SIZE - (head - tail) - 1;
    } else {
        return tail - head - 1;
    }
}

int circ_buf_data_size() {
    int head = buf_head;
    int tail = buf_tail;

    if (head >= tail) {
        return head - tail;
    } else {
        return BUF_SIZE - (tail - head);
    }
}

int circ_buf_get_data(uint8_t *buf, int max_len) {
    int tail = buf_tail;
    int head = buf_head;

    if (tail == head)
        return 0;

    // get available data (without wrap around)
    int len = (head > tail ? head : BUF_SIZE) - tail;

    // limit data to max_len
    if (len > max_len)
        len = max_len;

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
    return circ_buf_get_data(buf + len, max_len - len) + len;
}

void circ_buf_add_data(const uint8_t *buf, int len) {
    int head = buf_head;

    // copy first part (from head to end of circular buffer)
    int n = len;
    if (n > BUF_SIZE - head)
        n = BUF_SIZE - head;
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

void circ_buf_reset() {
    buf_head = 0;
    buf_tail = 0;
}
