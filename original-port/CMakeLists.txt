cmake_minimum_required(VERSION 3.20)
project(CnCRenegade CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Original source root (absolute path, shared across worktrees)
set(ORIG_SRC "/Users/marc/Documents/ccr/original/Code")

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
    -Wno-narrowing
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
    -Wno-non-pod-varargs
    -fdelayed-template-parsing
)

# Thread support for pthread
find_package(Threads REQUIRED)

add_subdirectory(wwdebug)
add_subdirectory(wwmath)
add_subdirectory(wwbitpack)
add_subdirectory(wwlib)
add_subdirectory(wwutil)
add_subdirectory(wwsaveload)
add_subdirectory(wwtranslatedb)
add_subdirectory(wwnet)
add_subdirectory(ww3d2)
add_subdirectory(wwphys)
add_subdirectory(WWAudio)
add_subdirectory(BinkMovie)
add_subdirectory(wwui)
add_subdirectory(SControl)
add_subdirectory(Scripts)
add_subdirectory(Combat)
add_subdirectory(Commando)
