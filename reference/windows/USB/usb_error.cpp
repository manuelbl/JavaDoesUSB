//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
//

#include "usb_error.hpp"

#include <Windows.h>


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
    throw usb_error(message, GetLastError());
}

std::string usb_error::full_message(const char* message, int code) {
    if (code == 0)
        return message;

    LPSTR messageBuffer = nullptr;

    size_t size = FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
        NULL, code, 0, (LPSTR)&messageBuffer, 0, NULL);

    std::string msg(message);
    msg += " (";
    msg.append(messageBuffer, size);
    msg += ")";

    // free the allocated memory
    LocalFree(messageBuffer);

    return msg;
}
