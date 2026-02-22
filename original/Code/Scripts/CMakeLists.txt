set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

file(GLOB SCRIPTS_SOURCES "${ORIG_SRC}/Scripts/*.cpp")

# Build as a shared library (dylib) so the game can load it dynamically via dlopen,
# mirroring how SCRIPTS.DLL was loaded on Windows.
add_library(Scripts SHARED ${SCRIPTS_SOURCES})

# BUILDING_DLL makes scriptevents.h declare exports as extern "C" __declspec(dllexport),
# which on macOS (where _declspec is a no-op) gives the functions C linkage (unmangled
# names) so dlsym("Create_Script") etc. work correctly.
target_compile_definitions(Scripts PRIVATE BUILDING_DLL)

# Enable MSVC extensions for Scripts: scriptcommands.h uses default args in function pointer types
target_compile_options(Scripts PRIVATE -fms-extensions)

# Allow undefined symbols to be resolved from the main binary at runtime.
# -undefined dynamic_lookup: defer undefined symbol resolution to load time.
target_link_options(Scripts PRIVATE "-undefined" "dynamic_lookup")

# Place the dylib next to the Commando executable inside the .app bundle so
# dlopen("SCRIPTS.dylib") can find it via the @executable_path rpath.
set_target_properties(Scripts PROPERTIES
    OUTPUT_NAME "SCRIPTS"
    PREFIX ""
    SUFFIX ".dylib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/Code/Commando/Commando.app/Contents/MacOS"
)

target_include_directories(Scripts PRIVATE
    "${ORIG_SRC}/Scripts"
    "${ORIG_SRC}"
    "${ORIG_SRC}/Combat"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/ww3d2"
    "${ORIG_SRC}/wwphys"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwtranslatedb"
    "${ORIG_SRC}/wwnet"
    "${ORIG_SRC}/WWAudio"
    "${ORIG_SRC}/wwbitpack"
    "${ORIG_SRC}/wwui"
)
