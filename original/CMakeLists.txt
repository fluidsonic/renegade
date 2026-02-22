cmake_minimum_required(VERSION 3.20)
project(CnCRenegade CXX C)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Debug build: no optimizations, full debug symbols, AddressSanitizer
set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Build type" FORCE)
set(CMAKE_CXX_FLAGS_DEBUG "-O0 -g3 -fno-omit-frame-pointer -fsanitize=address" CACHE STRING "" FORCE)
set(CMAKE_C_FLAGS_DEBUG   "-O0 -g3 -fno-omit-frame-pointer -fsanitize=address" CACHE STRING "" FORCE)
set(CMAKE_EXE_LINKER_FLAGS_DEBUG "-fsanitize=address" CACHE STRING "" FORCE)

set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

# Global compile definitions
add_compile_definitions(
    _UNIX
    NOMINMAX
)

# Compat layer is FIRST on include path so it intercepts system headers
set(COMPAT_DIR "${CMAKE_SOURCE_DIR}/compat")

# Global include directories: compat FIRST, then original include roots
include_directories(
    BEFORE
    "${COMPAT_DIR}"
)

# Force-include windef.h so Windows types and calling-convention macros are
# available even in headers that don't #include <windows.h>
add_compile_options(-include "${COMPAT_DIR}/windef.h")

# Clang/Apple Silicon specific flags
add_compile_options(
    -Wno-pragma-once-outside-header
    -Wno-unknown-pragmas
    -Wno-register
    -Wno-writable-strings
    -Wno-invalid-offsetof
    -Wno-int-to-pointer-cast
    -Wno-pointer-to-int-cast
    -Wno-comment
    -Wno-microsoft-include
    -Wno-nonportable-include-path
    -Wno-deprecated-declarations
    -Wno-unused-value
    -Wno-format
    -Wno-parentheses-equality
    -Wno-tautological-compare
    -Wno-unused-variable
    -Wno-infinite-recursion
    -Wno-self-assign
    -Wno-c++11-extensions
    -Wno-dangling-else
    -Wno-macro-redefined
    -Werror=non-pod-varargs
    -Wconversion
    -Wsign-conversion
    -Wdouble-promotion
)

# Thread support for pthread
find_package(Threads REQUIRED)

# SDL2 — prefer system install (Homebrew on macOS), fall back to FetchContent
find_package(SDL2 QUIET)
if(NOT SDL2_FOUND)
    include(FetchContent)
    FetchContent_Declare(SDL2
        GIT_REPOSITORY https://github.com/libsdl-org/SDL.git
        GIT_TAG        release-2.30.11
        GIT_SHALLOW    TRUE
    )
    set(SDL_SHARED OFF CACHE BOOL "" FORCE)
    set(SDL_STATIC ON CACHE BOOL "" FORCE)
    set(SDL_TEST OFF CACHE BOOL "" FORCE)
    FetchContent_MakeAvailable(SDL2)
    include_directories("${sdl2_SOURCE_DIR}/include")
else()
    # System SDL2 (Homebrew)
    include_directories(${SDL2_INCLUDE_DIRS})
    # Create SDL2::SDL2-static alias if not present (system SDL2 uses SDL2::SDL2)
    if(NOT TARGET SDL2::SDL2-static)
        add_library(SDL2::SDL2-static ALIAS SDL2::SDL2)
    endif()
endif()

add_subdirectory(Code/wwmath)
add_subdirectory(Code/wwbitpack)
add_subdirectory(Code/wwlib)
add_subdirectory(Code/wwutil)
add_subdirectory(Code/wwsaveload)
add_subdirectory(Code/wwtranslatedb)
add_subdirectory(Code/wwnet)
add_subdirectory(Code/ww3d2)
add_subdirectory(Code/wwphys)
add_subdirectory(Code/WWAudio)
add_subdirectory(Code/BinkMovie)
add_subdirectory(Code/wwui)
add_subdirectory(Code/SControl)
add_subdirectory(Code/Scripts)
add_subdirectory(Code/Combat)
add_subdirectory(Code/Commando)
