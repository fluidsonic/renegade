set(ORIG_SRC "/Users/marc/Documents/ccr/original/Code")

file(GLOB COMMANDO_SOURCES "${ORIG_SRC}/Commando/*.cpp")

# Stub sources: global definitions for excluded/deleted files + macOS main bridge
set(STUB_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/main_stub.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/commando_stubs.cpp"
)

add_executable(Commando ${COMMANDO_SOURCES} ${STUB_SOURCES})

target_include_directories(Commando PRIVATE
    "${ORIG_SRC}"
    "${ORIG_SRC}/Commando"
    "${ORIG_SRC}/Combat"
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

target_link_libraries(Commando PRIVATE
    Combat
    Scripts
    SControl
    wwui
    ww3d2
    wwphys
    WWAudio
    BinkMovie
    wwnet
    wwtranslatedb
    wwsaveload
    wwutil
    wwlib
    wwbitpack
    wwmath
    wwdebug
    Threads::Threads
)
