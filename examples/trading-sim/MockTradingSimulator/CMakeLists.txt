cmake_minimum_required(VERSION 4.1)
project(MockTradingSimulator)

set(CMAKE_CXX_STANDARD 23)

# Build MockSimNative as a dynamic library (shared library/DLL)
set(BUILD_SHARED_LIBS ON)
add_subdirectory(MockSimNative)
set(BUILD_SHARED_LIBS OFF)

add_executable(MockTradingSimulator main.cpp)

target_link_libraries(MockTradingSimulator
        PRIVATE
        MockSimNative)

if(WIN32)
    add_custom_command(TARGET MockTradingSimulator POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
            $<TARGET_FILE:MockSimNative>
            $<TARGET_FILE_DIR:MockTradingSimulator>)
endif()