//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#pragma once

#include <cstdint>
#include <vector>

/**
 * Pseudo Random Number Generator
 */
struct prng
{
    /**
     * Constructs a new instance
     * @param init initial value
     */
    prng(uint32_t init);
    
    /**
     * Returns the next pseudo random value
     * @return pseudo random value
     */
    uint32_t next();
    
    /**
     * Fills the buffer with pseudo random data
     * @param buf buffer receiving the random data
     * @param len length of the buffer (in bytes)
     */
    void fill(uint8_t *buf, int len);
    
    /**
     * Fills the buffer with pseudo random data
     * @param buf buffer receiving the random data
     * @param len number of bytes to fill (-1 for entire buffer)
     */
    void fill(std::vector<uint8_t>& buf, int len = -1);

    /**
     * Verifies that the passed data matches the next bytes of the sequence.
     * @param buf buffer with data to verify
     * @param len length of the buffer (in bytes)
     * @return -1 if they match, otherwise the position of the difference
     */
    int verify(const uint8_t *buf, int len);
    
    /**
     * Verifies that the passed data matches the next bytes of the sequence.
     * @param buf buffer with data to verify
     * @param len number of bytes to verify (-1 for entire buffer)
     * @return -1 if they match, otherwise the position of the difference
     */
    int verify(const std::vector<uint8_t>& buf, int len = -1);

    /**
     * Resets the generator to its initial state.
     * @param init initial value
     */
    void reset(uint32_t init);

private:
    uint32_t state;
    int nbytes;
    uint32_t bits;
};
