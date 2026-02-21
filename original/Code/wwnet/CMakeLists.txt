set(ORIG_WWNET "${ORIG_SRC}/wwnet")

file(GLOB WWNET_SOURCES "${ORIG_WWNET}/*.cpp")

add_library(wwnet STATIC ${WWNET_SOURCES})

target_include_directories(wwnet BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWNET}"
    "${ORIG_SRC}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwbitpack"
    "${ORIG_SRC}/wolapi"
    "${ORIG_SRC}/WWOnline"
)

target_link_libraries(wwnet wwsaveload wwutil wwlib wwmath)
