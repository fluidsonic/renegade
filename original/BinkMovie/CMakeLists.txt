set(ORIG_SRC "/Users/marc/Documents/ccr/original/Code")

file(GLOB BINKMOVIE_SOURCES "${ORIG_SRC}/BinkMovie/*.cpp")

add_library(BinkMovie STATIC ${BINKMOVIE_SOURCES})

target_include_directories(BinkMovie PRIVATE
    "${ORIG_SRC}/BinkMovie"
    "${ORIG_SRC}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/ww3d2"
)
