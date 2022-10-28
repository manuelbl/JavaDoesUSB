//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Linux
//

#include "usb_error.hpp"

#include <errno.h>
#include <string.h>


usb_error::usb_error(const char* message, int code) noexcept
    : _message(full_message(message, code)), _code(code) { }

usb_error::~usb_error() noexcept { }

const char* usb_error::what() const noexcept {
    return _message.c_str();
}

long usb_error::error_code() {
    return _code;
}

void usb_error::throw_error(const char* message) {
    throw usb_error(message, errno);
}

std::string usb_error::full_message(const char* message, int code) {
    if (code == 0)
        return message;
    
    std::string msg(message);
    msg += " (";
    msg += strerror(code);
    msg += ")";
    
    return msg;
}
