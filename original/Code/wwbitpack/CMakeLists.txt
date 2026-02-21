set(ORIG_WWBITPACK "${ORIG_SRC}/wwbitpack")

file(GLOB WWBITPACK_SOURCES "${ORIG_WWBITPACK}/*.cpp")

add_library(wwbitpack STATIC ${WWBITPACK_SOURCES})

target_include_directories(wwbitpack BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWBITPACK}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwutil"
)

target_link_libraries(wwbitpack wwmath)
