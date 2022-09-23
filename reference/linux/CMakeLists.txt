cmake_minimum_required(VERSION 3.10)

project(linux)

set (CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

set(SOURCES
    assertion.cpp assertion.hpp
    main.cpp
    scope.hpp
    tests.cpp tests.hpp
    usb_device.cpp usb_device.hpp
    usb_error.cpp usb_error.hpp
    usb_registry.cpp usb_registry.hpp
)

add_executable(linux_usb ${SOURCES})
target_link_libraries(linux_usb Threads::Threads)
target_link_libraries(linux_usb -ludev)