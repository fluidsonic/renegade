set(ORIG_WWLIB "${ORIG_SRC}/wwlib")

file(GLOB WWLIB_SOURCES "${ORIG_WWLIB}/*.cpp")

add_library(wwlib STATIC ${WWLIB_SOURCES})

target_include_directories(wwlib BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWLIB}"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwutil"
)

target_link_libraries(wwlib wwdebug wwmath)
