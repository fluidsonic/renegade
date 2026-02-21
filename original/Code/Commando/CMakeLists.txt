set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")
set(COMPAT_DIR "${CMAKE_SOURCE_DIR}/compat")

file(GLOB COMMANDO_SOURCES "${ORIG_SRC}/Commando/*.cpp")

add_executable(Commando MACOSX_BUNDLE
    ${COMMANDO_SOURCES}
    "${COMPAT_DIR}/sdl2_platform.cpp"
    "${COMPAT_DIR}/d3d8_gl.cpp"
    "${COMPAT_DIR}/macos_fix.mm"
)

# Build as a proper macOS .app bundle so AppKit/CoreAnalytics can find
# the bundle identifier (prevents crash in [NSApplication sharedApplication]).
set_target_properties(Commando PROPERTIES
    MACOSX_BUNDLE_BUNDLE_IDENTIFIER "com.westwood.renegade"
    MACOSX_BUNDLE_BUNDLE_NAME       "Renegade"
    MACOSX_BUNDLE_SHORT_VERSION_STRING "1.0"
    MACOSX_BUNDLE_GUI_IDENTIFIER    "com.westwood.renegade"
    # Tell macOS this is a background/agent process so it doesn't show in Dock
    # until we explicitly request it (optional — remove to get Dock icon)
    MACOSX_BUNDLE_INFO_PLIST        "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist.in"
)

target_include_directories(Commando PRIVATE
    "${ORIG_SRC}"
    "${ORIG_SRC}/Commando"
    "${ORIG_SRC}/Combat"
    "${ORIG_SRC}/wwui"
    "${ORIG_SRC}/wwlib"
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
    Threads::Threads
    SDL2::SDL2-static
    "-framework OpenGL"
    "-framework AppKit"
    "-framework Foundation"
)
