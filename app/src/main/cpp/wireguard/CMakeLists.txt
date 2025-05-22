# WireGuard Native Library Build Configuration
# WG-001: Cross-compile wireguard-go for arm64, x86_64

cmake_minimum_required(VERSION 3.22.1)
project("marfanet-wireguard")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add WireGuard Go native library
add_library(wg SHARED
    wg_native.cpp
    wg_bridge.cpp
)

# Include directories
target_include_directories(wg PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link libraries
target_link_libraries(wg
    android
    log
)

# Compiler flags for optimization
target_compile_options(wg PRIVATE
    -O3
    -fvisibility=hidden
    -Wall
    -Wextra
)