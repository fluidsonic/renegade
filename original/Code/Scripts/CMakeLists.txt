set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

file(GLOB SCRIPTS_SOURCES "${ORIG_SRC}/Scripts/*.cpp")

add_library(Scripts STATIC ${SCRIPTS_SOURCES})

# Enable MSVC extensions for Scripts: scriptcommands.h uses default args in function pointer types
target_compile_options(Scripts PRIVATE -fms-extensions)

target_include_directories(Scripts PRIVATE
    "${ORIG_SRC}/Scripts"
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
