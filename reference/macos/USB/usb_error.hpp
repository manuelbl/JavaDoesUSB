//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for macOS
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
     * @param message error message
     * @param code a Mach error code, or 0 if no Mach code is available
     */
    usb_error(const char* message, int code = 0) noexcept;
    
    virtual ~usb_error() noexcept;

    virtual const char* what() const noexcept;
    /// Mach error code
    long error_code();
    
    /**
     * Throws a USB error exception if the code indicates an error.
     *
     * @param code Mach error code
     * @param message additional information for the error message
     */
    static void check(int code, const char* message);
    
private:
    static std::string full_message(const char* message, int code);

    std::string _message;
    long _code;
};
