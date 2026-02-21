cmake_minimum_required(VERSION 3.20)
project(CnCRenegade CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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

add_subdirectory(Code/wwdebug)
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
