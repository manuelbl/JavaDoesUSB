//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include <utility>

template <class EF>
class scope_exit
{
public:
    template<class Fn>
    explicit scope_exit(Fn&& fn) noexcept
        : exit_fn(std::forward<EF>(fn)), exec_on_destruction(true)
    { }

    scope_exit(scope_exit&& other) noexcept
        : exit_fn(std::forward<EF>(other.exit_fn)),
        exec_on_destruction(other.exec_on_destruction)
    {
        other.release();
    }

    ~scope_exit() noexcept {
        if (exec_on_destruction)
            exit_fn();
    }

    void release() noexcept {
        exec_on_destruction = false;
    }

    scope_exit(scope_exit const&) = delete;

    scope_exit& operator=(scope_exit const&) = delete;
    scope_exit& operator=(scope_exit&&) = delete;

private:
    EF exit_fn;
    bool exec_on_destruction;
};

template <class EF>
scope_exit<typename std::decay<EF>::type> make_scope_exit(EF&& exit_function) {
    return scope_exit<typename std::decay<EF>::type>(std::forward<EF>(exit_function));
}
