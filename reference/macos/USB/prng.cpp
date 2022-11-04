//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "prng.hpp"

prng::prng(uint32_t init) : state(init), nbytes(0), bits(0) {}

void prng::reset(uint32_t init)
{
    state = init;
    nbytes = 0;
    bits = 0;
}

uint32_t prng::next()
{
    uint32_t x = state;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    state = x;
    return x;
}

void prng::fill(uint8_t *buf, int len)
{
    for (int i = 0; i < len; i++)
    {
        if (nbytes == 0)
        {
            bits = next();
            nbytes = 4;
        }
        buf[i] = bits;
        bits >>= 8;
        nbytes--;
    }
}

void prng::fill(std::vector<uint8_t>& buf, int len)
{
    if (len == -1 || len > buf.size())
        len = static_cast<int>(buf.size());
    
    fill(buf.data(), len);
}

int prng::verify(const uint8_t *buf, int len)
{
    for (int i = 0; i < len; i++)
    {
        if (nbytes == 0)
        {
            bits = next();
            nbytes = 4;
        }

        if (buf[i] != (uint8_t)bits)
            return i;

        bits >>= 8;
        nbytes--;
    }

    return -1;
}

int prng::verify(const std::vector<uint8_t> &buf, int len)
{
    if (len == -1 || len > buf.size())
        len = static_cast<int>(buf.size());
    
    return verify(buf.data(), len);
}
