cmake_minimum_required(VERSION 3.22.1)

# ── Fix untuk Android IDE (rv2ide) on-device build ──────────────────────────
# rv2ide tidak meng-set ANDROID_HOST_TAG sehingga path clang++ jadi:
#   .../prebuilt//bin/clang++  ← double slash, file tidak ditemukan
# Solusi: hardcode host tag sebelum CMake mencoba detect compiler.
if(DEFINED ANDROID_NDK AND NOT CMAKE_CXX_COMPILER)
    set(_HOST_TAG "linux-x86_64")
    set(_COMPILER "${ANDROID_NDK}/toolchains/llvm/prebuilt/${_HOST_TAG}/bin/clang++")
    if(EXISTS "${_COMPILER}")
        set(CMAKE_CXX_COMPILER "${_COMPILER}" CACHE STRING "C++ compiler" FORCE)
        message(STATUS "[rv2ide-fix] Using compiler: ${_COMPILER}")
    else()
        message(WARNING "[rv2ide-fix] Compiler not found at ${_COMPILER}, falling back to CMake default")
    endif()
endif()
# ────────────────────────────────────────────────────────────────────────────

project("aether-x" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── Compiler flags ──────────────────────────────────────────────────────────
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} \
    -O2 \
    -fvisibility=hidden \
    -fstack-protector-strong \
    -Wall \
    -Wextra \
    -Wno-unused-parameter")

# ── Target: libaether-x.so ──────────────────────────────────────────────────
add_library(
    aether-x
    SHARED
    aether_exec.cpp
)

# ── Link libraries ──────────────────────────────────────────────────────────
find_library(log-lib log)

target_link_libraries(
    aether-x
    ${log-lib}
)

# ── Strip di release ────────────────────────────────────────────────────────
target_link_options(aether-x PRIVATE
    -Wl,--hash-style=gnu
    -Wl,--exclude-libs,ALL
    -Wl,--strip-all
)
