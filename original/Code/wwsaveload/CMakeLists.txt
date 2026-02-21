set(ORIG_WWSAVELOAD "${ORIG_SRC}/wwsaveload")

file(GLOB WWSAVELOAD_SOURCES "${ORIG_WWSAVELOAD}/*.cpp")

add_library(wwsaveload STATIC ${WWSAVELOAD_SOURCES})

target_include_directories(wwsaveload BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWSAVELOAD}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
)

target_link_libraries(wwsaveload wwlib wwmath)
