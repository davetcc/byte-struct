cmake_minimum_required(VERSION 3.20)

project(MockSimNative LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(MockSimNative SHARED
        TradingSystem.cpp
        NativeInterop.cpp
        NativeInterop.h)

target_include_directories(MockSimNative
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR})

target_compile_definitions(MockSimNative PRIVATE MOCKSIMNATIVE_EXPORTS)

if(WIN32)
    target_link_libraries(MockSimNative PRIVATE bcrypt)
endif()

set_target_properties(MockSimNative PROPERTIES
        OUTPUT_NAME "mocksimnative")