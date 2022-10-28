//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include <exception>
#include <vector>


void assert_equals(int expected, int actual, const char* message = nullptr);
void assert_equals(long expected, long actual, const char* message = nullptr);
void assert_equals(unsigned long expected, unsigned long actual, const char* message = nullptr);
void assert_equals(size_t expected, size_t actual, const char* message = nullptr);
void assert_equals(const std::vector<uint8_t>& expected, const std::vector<uint8_t>& actual, const char* message = nullptr);

class check_failed_error : public std::exception {
public:
    check_failed_error() noexcept;
    virtual ~check_failed_error() noexcept;
    virtual const char* what() const noexcept;
};
