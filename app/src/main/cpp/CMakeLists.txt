cmake_minimum_required(VERSION 3.22.1)
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
