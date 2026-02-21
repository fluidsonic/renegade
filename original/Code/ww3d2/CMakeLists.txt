set(ORIG_WW3D2 "${ORIG_SRC}/ww3d2")

file(GLOB WW3D2_SOURCES "${ORIG_WW3D2}/*.cpp")

add_library(ww3d2 STATIC ${WW3D2_SOURCES})

target_include_directories(ww3d2 BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WW3D2}"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/WWAudio"
)

target_link_libraries(ww3d2 wwsaveload wwlib wwmath)
