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
#include <string>

/// USB error exception
class usb_error : public std::exception {
public:
    /**
     * Creates a new instance.
     *
     *@param message error message
     *@param code a Windows error code, or 0 if no Windows code is available
     */
    usb_error(const char* message, int code = 0) noexcept;
    
    virtual ~usb_error() noexcept;

    virtual const char* what() const noexcept;
    /// Mach error code
    long error_code();
    
    /**
     * Throws a USB error exception with the `GetLastError()` code.
     *
     * @param message additional information for the error message
     */
    __declspec(noreturn) static void throw_error(const char* message);
    
private:
    static std::string full_message(const char* message, int code);

    std::string _message;
    long _code;
};
