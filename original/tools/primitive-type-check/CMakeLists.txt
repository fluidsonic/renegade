# Primitive-type clang-tidy check plugin
# Requires: brew install llvm  (LLVM at /opt/homebrew/opt/llvm)
set(LLVM_PREFIX "/opt/homebrew/opt/llvm" CACHE PATH "LLVM installation prefix")

add_library(PrimitiveTypeCheck MODULE
    PrimitiveTypeCheck.cpp
)

target_include_directories(PrimitiveTypeCheck PRIVATE
    "${LLVM_PREFIX}/include"
)

# The root CMakeLists applies global flags (force-include windef.h,
# -Wconversion, -Wsign-conversion, -Wdouble-promotion, -fsanitize=address,
# -Werror=non-pod-varargs) via add_compile_options / CMAKE_CXX_FLAGS_DEBUG.
# LLVM's own headers are not compatible with those flags.
# Override by explicitly setting COMPILE_OPTIONS to only what this target needs.
set_property(TARGET PrimitiveTypeCheck PROPERTY COMPILE_OPTIONS
    -fno-rtti           # LLVM is built without RTTI; all plugins must match
    -fPIC
    -fno-sanitize=address   # dylib dlopen'd into clang-tidy; ASan cannot be late-loaded
)

# Also remove the force-include of windef.h that is injected globally.
# We do this by fetching the directory INCLUDE_DIRECTORIES and removing compat/.
set_property(TARGET PrimitiveTypeCheck PROPERTY INCLUDE_DIRECTORIES
    "${LLVM_PREFIX}/include"
)

target_link_libraries(PrimitiveTypeCheck PRIVATE
    "${LLVM_PREFIX}/lib/libclangTidy.a"
    "${LLVM_PREFIX}/lib/libclang-cpp.dylib"
    "${LLVM_PREFIX}/lib/libLLVM.dylib"
)

set_target_properties(PrimitiveTypeCheck PROPERTIES
    CXX_STANDARD 17
    PREFIX ""
    SUFFIX ".dylib"
    MACOSX_RPATH ON
    BUILD_WITH_INSTALL_RPATH TRUE
    INSTALL_RPATH "${LLVM_PREFIX}/lib"
    # Strip ASan from the link step — CMAKE_CXX_FLAGS_DEBUG injects -fsanitize=address
    # into both compilation AND linking.  The plugin is dlopen'd into clang-tidy which
    # has no ASan runtime, so ASan in the dylib causes an immediate fatal abort.
    LINK_FLAGS "-fno-sanitize=address"
)
