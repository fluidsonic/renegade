set(ORIG_SRC "${CMAKE_SOURCE_DIR}/Code")
set(COMPAT_DIR "${CMAKE_SOURCE_DIR}/compat")

file(GLOB COMMANDO_SOURCES "${ORIG_SRC}/Commando/*.cpp")

# ---- Compile chat.rc → chat.res using llvm-rc ----
find_program(LLVM_RC NAMES llvm-rc
    HINTS /opt/homebrew/Cellar/llvm/21.1.8_1/bin /opt/homebrew/bin /usr/local/bin
    NO_DEFAULT_PATH)
if(NOT LLVM_RC)
    message(FATAL_ERROR "llvm-rc not found. Install LLVM: brew install llvm")
endif()

set(CHAT_RC  "${ORIG_SRC}/Commando/chat.rc")
set(CHAT_RES "${CMAKE_CURRENT_BINARY_DIR}/chat.res")

add_custom_command(
    OUTPUT  "${CHAT_RES}"
    COMMAND "${LLVM_RC}"
            "/C1252"
            "/I."
            "/FO${CHAT_RES}"
            "chat.rc"
    WORKING_DIRECTORY "${ORIG_SRC}/Commando"
    DEPENDS "${CHAT_RC}"
            "${ORIG_SRC}/Commando/resource.h"
            "${ORIG_SRC}/Commando/dialogresource.h"
            "${ORIG_SRC}/Commando/afxres.h"
    COMMENT "Compiling chat.rc → chat.res"
)

# chat.res will be copied into Contents/MacOS/ inside the .app bundle
set_source_files_properties("${CHAT_RES}" PROPERTIES
    MACOSX_PACKAGE_LOCATION "MacOS"
    GENERATED TRUE
)

add_executable(Commando MACOSX_BUNDLE
    ${COMMANDO_SOURCES}
    "${COMPAT_DIR}/sdl2_platform.cpp"
    "${COMPAT_DIR}/d3d8_gl.cpp"
    "${COMPAT_DIR}/macos_fix.mm"
    "${COMPAT_DIR}/winres.cpp"
    "${COMPAT_DIR}/wingdi_coretext.cpp"
    "${CHAT_RES}"
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

# Scripts is loaded dynamically at runtime (like SCRIPTS.DLL on Windows).
# We must still build it before Commando so the dylib is ready to be found.
add_dependencies(Commando Scripts)

target_link_libraries(Commando PRIVATE
    Combat
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
    "-framework CoreText"
    "-framework CoreGraphics"
    # Export all global symbols so Scripts.dylib (loaded via dlopen) can find them.
    "-Wl,-export_dynamic"
    # Allow dlopen("SCRIPTS.dylib") to find the dylib next to the executable.
    "-Wl,-rpath,@executable_path"
)
