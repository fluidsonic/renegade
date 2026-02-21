set(ORIG_WWUTIL "${ORIG_SRC}/wwutil")

file(GLOB WWUTIL_SOURCES "${ORIG_WWUTIL}/*.cpp")

add_library(wwutil STATIC ${WWUTIL_SOURCES})

target_include_directories(wwutil BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWUTIL}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/WWMath"
)

target_link_libraries(wwutil wwlib wwdebug wwmath)
