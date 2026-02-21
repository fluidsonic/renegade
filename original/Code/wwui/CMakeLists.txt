set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

file(GLOB WWUI_SOURCES "${ORIG_SRC}/wwui/*.cpp")

add_library(wwui STATIC ${WWUI_SOURCES})

target_include_directories(wwui PRIVATE
    "${ORIG_SRC}/wwui"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/ww3d2"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwtranslatedb"
    "${ORIG_SRC}/WWAudio"
    "${ORIG_SRC}/wwphys"
    "${ORIG_SRC}/wwbitpack"
    "${ORIG_SRC}/wwnet"
)
