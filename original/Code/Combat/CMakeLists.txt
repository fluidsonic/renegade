set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")

file(GLOB COMBAT_SOURCES "${ORIG_SRC}/Combat/*.cpp")

add_library(Combat STATIC ${COMBAT_SOURCES})

target_include_directories(Combat PRIVATE
    "${ORIG_SRC}/Combat"
    "${ORIG_SRC}/Commando"
    "${ORIG_SRC}/wwui"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/WWMath"
    "${ORIG_SRC}/ww3d2"
    "${ORIG_SRC}/wwphys"
    "${ORIG_SRC}/wwutil"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwtranslatedb"
    "${ORIG_SRC}/wwnet"
    "${ORIG_SRC}/WWAudio"
    "${ORIG_SRC}/wwbitpack"
    "${ORIG_SRC}/BinkMovie"
    "${ORIG_SRC}/Scripts"
    "${ORIG_SRC}/SControl"
)
