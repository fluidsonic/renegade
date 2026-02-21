set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

file(GLOB SCONTROL_SOURCES "${ORIG_SRC}/SControl/*.cpp")

add_library(SControl STATIC ${SCONTROL_SOURCES})

target_include_directories(SControl PRIVATE
    "${ORIG_SRC}/SControl"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwnet"
    "${ORIG_SRC}/wwutil"
)
