//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code common for Linux / macOS / Windows
//

#include "assertion.hpp"

#include <cstdio>
#include <iostream>

static void failed(const char* check, const char* message) {
    if (message == nullptr)
        message = "Check failed";
    std::cerr << message << ": " << check << std::endl;
    throw new check_failed_error();
}

void assert_equals(int expected, int actual, const char* message) {
    if (actual != expected) {
        char buf[50];
        snprintf(buf, sizeof(buf), "expected: %d, actual: %d", expected, actual);
        failed(buf, message);
   }
}

void assert_equals(long expected, long actual, const char* message) {
    if (actual != expected) {
        char buf[50];
        snprintf(buf, sizeof(buf), "expected: %ld, actual: %ld", expected, actual);
        failed(buf, message);
   }
}

void assert_equals(unsigned long expected, unsigned long actual, const char* message) {
    if (actual != expected) {
        char buf[50];
        snprintf(buf, sizeof(buf), "expected: %lud, actual: %lud", expected, actual);
        failed(buf, message);
    }
}

void assert_equals(size_t expected, size_t actual, const char* message) {
    if (actual != expected) {
        char buf[50];
        snprintf(buf, sizeof(buf), "expected: %lld, actual: %lld", expected, actual);
        failed(buf, message);
    }
}

void assert_equals(const std::vector<uint8_t>& expected, const std::vector<uint8_t>& actual, const char* message) {
    assert_equals(expected.size(), actual.size(), message);
    for (int i = 0; i < expected.size(); i++) {
        assert_equals(expected[i], actual[i], message);
    }
}


check_failed_error::check_failed_error() noexcept { }
check_failed_error::~check_failed_error() noexcept { }
const char* check_failed_error::what() const noexcept { return "check failed"; }
