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

# Clang/Apple Silicon specific flags
add_compile_options(
    -Wno-pragma-once-outside-header
    -Wno-unknown-pragmas
    -Wno-register
    -Wno-writable-strings
    -Wno-invalid-offsetof
    -Wno-narrowing
    -Wno-int-to-pointer-cast
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
)

# Thread support for pthread
find_package(Threads REQUIRED)

add_subdirectory(wwdebug)
# add_subdirectory(WWMath)
# add_subdirectory(wwbitpack)
# add_subdirectory(wwlib)
