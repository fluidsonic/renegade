set(ORIG_SRC "/Users/marc/Documents/ccr/original/Code")

file(GLOB WWAUDIO_SOURCES "${ORIG_SRC}/WWAudio/*.cpp")

add_library(WWAudio STATIC ${WWAUDIO_SOURCES})

target_include_directories(WWAudio PRIVATE
    "${ORIG_SRC}/WWAudio"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/ww3d2"
)
