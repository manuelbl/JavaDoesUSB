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

#ifndef CIRC_BUF_H
#define CIRC_BUF_H

#include <stdint.h>

#define BUF_SIZE 1025

#ifdef __cplusplus
extern "C" {
#endif

/// Returns the maximum number of bytes that can be added to the buffer
int circ_buf_avail_size();

/// Returns the number of bytes in the buffer
int circ_buf_data_size();

/**
 * Gets the oldest data from the buffer and removes it.
 * @param buf buffer to copy data to
 * @param max_len maximum number of bytes to copy
 * @return the effective number of bytes
 */
int circ_buf_get_data(uint8_t *buf, int max_len);

/**
 * Adds data to the buffer
 *
 * @param buf the buffer with the data
 * @param len the number of bytes to add
 */
void circ_buf_add_data(const uint8_t *buf, int len);

/// Resets (empties) the circular buffer
void circ_buf_reset();

#ifdef __cplusplus
}
#endif

#endif
