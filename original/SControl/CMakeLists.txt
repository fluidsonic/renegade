set(ORIG_SRC "/Users/marc/Documents/ccr/original/Code")

file(GLOB SCONTROL_SOURCES "${ORIG_SRC}/SControl/*.cpp")

add_library(SControl STATIC ${SCONTROL_SOURCES})

target_include_directories(SControl PRIVATE
    "${ORIG_SRC}/SControl"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwnet"
    "${ORIG_SRC}/wwutil"
)
