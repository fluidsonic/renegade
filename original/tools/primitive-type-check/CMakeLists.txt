# Primitive-type clang-tidy check plugin
# Requires: brew install llvm  (LLVM at /opt/homebrew/opt/llvm)
set(LLVM_PREFIX "/opt/homebrew/opt/llvm" CACHE PATH "LLVM installation prefix")

add_library(PrimitiveTypeCheck MODULE
    PrimitiveTypeCheck.cpp
)

target_include_directories(PrimitiveTypeCheck PRIVATE
    "${LLVM_PREFIX}/include"
)

target_compile_options(PrimitiveTypeCheck PRIVATE
    -fno-rtti   # LLVM is built without RTTI; all plugins must match
    -fPIC
)

target_link_libraries(PrimitiveTypeCheck PRIVATE
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
)
