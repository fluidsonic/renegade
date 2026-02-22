cmake_minimum_required(VERSION 3.20)
project(CnCRenegade CXX C)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Export compile commands so clang-tidy / lint targets can find all sources
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Debug build: no optimizations, full debug symbols, AddressSanitizer
set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Build type" FORCE)
set(CMAKE_CXX_FLAGS_DEBUG "-O0 -g3 -fno-omit-frame-pointer -fsanitize=address" CACHE STRING "" FORCE)
set(CMAKE_C_FLAGS_DEBUG   "-O0 -g3 -fno-omit-frame-pointer -fsanitize=address" CACHE STRING "" FORCE)
set(CMAKE_EXE_LINKER_FLAGS_DEBUG "-fsanitize=address" CACHE STRING "" FORCE)

set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

# Compat layer is FIRST on include path so it intercepts system headers
set(COMPAT_DIR "${CMAKE_SOURCE_DIR}/compat")

# Global include directories: compat FIRST, then original include roots
include_directories(
    BEFORE
    "${CMAKE_SOURCE_DIR}"    # global.h lives at source root
    "${COMPAT_DIR}"
)

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
    -Werror=return-type
    -Werror=delete-incomplete
    -Werror=delete-abstract-non-virtual-dtor
    -Werror=null-conversion
    -Werror=array-bounds
    -Wconversion
    -Wsign-conversion
    -Wdouble-promotion
    -Wunused-function
    -Wunused-private-field
    -Wunneeded-internal-declaration
    -Wunreachable-code
    -Wunreachable-code-return
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

# ── Primitive-type lint (requires brew install llvm) ──────────────────────────
find_program(CLANG_TIDY_EXE NAMES clang-tidy
    HINTS /opt/homebrew/opt/llvm/bin NO_DEFAULT_PATH)
find_program(RUN_CLANG_TIDY NAMES run-clang-tidy
    HINTS /opt/homebrew/opt/llvm/bin NO_DEFAULT_PATH)

if(CLANG_TIDY_EXE AND RUN_CLANG_TIDY)
    add_subdirectory(tools/primitive-type-check)

    # LLVM clang-tidy doesn't inherit Apple Clang's implicit sysroot —
    # pass it explicitly so system headers (string.h, etc.) resolve correctly.
    execute_process(
        COMMAND xcrun --show-sdk-path
        OUTPUT_VARIABLE MACOS_SDK_PATH
        OUTPUT_STRIP_TRAILING_WHITESPACE
    )

    add_custom_target(lint
        COMMAND "${RUN_CLANG_TIDY}"
                -clang-tidy-binary "${CLANG_TIDY_EXE}"
                -load "$<TARGET_FILE:PrimitiveTypeCheck>"
                -checks=-*,ccr-primitive-type
                -header-filter=".*/Code/.*"
                -p "${CMAKE_BINARY_DIR}"
                --extra-arg="-isysroot ${MACOS_SDK_PATH}"
                -j8
        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}"
        DEPENDS PrimitiveTypeCheck
        COMMENT "Running CCR primitive-type lint across Code/"
    )
else()
    message(STATUS "clang-tidy not found at /opt/homebrew/opt/llvm/bin — lint target unavailable")
endif()
