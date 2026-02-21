set(ORIG_WWMATH "${ORIG_SRC}/WWMath")

file(GLOB WWMATH_SOURCES "${ORIG_WWMATH}/*.cpp")

add_library(wwmath STATIC ${WWMATH_SOURCES})

target_include_directories(wwmath BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWMATH}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/wwsaveload"
)

target_link_libraries(wwmath wwdebug)
