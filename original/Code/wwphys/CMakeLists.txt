set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

file(GLOB WWPHYS_SOURCES "${ORIG_SRC}/wwphys/*.cpp")

add_library(wwphys STATIC ${WWPHYS_SOURCES})

target_include_directories(wwphys PRIVATE
    "${ORIG_SRC}/wwphys"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/ww3d2"
    "${ORIG_SRC}/WWAudio"
    "${ORIG_SRC}/wwbitpack"
)
