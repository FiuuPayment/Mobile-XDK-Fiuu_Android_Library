cmake_minimum_required(VERSION 3.22.1)

project("fiuu-sec")

add_library(
    fiuu-sec
    SHARED
    fiuu_security.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    fiuu-sec
    ${log-lib}
)

# Enforce 16 KB ELF page alignment for Android 15+ compatibility
target_link_options(
    fiuu-sec
    PRIVATE
    "-Wl,-z,max-page-size=16384"
)
