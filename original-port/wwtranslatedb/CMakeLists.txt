set(ORIG_WWTRANSLATEDB "${ORIG_SRC}/wwtranslatedb")

file(GLOB WWTRANSLATEDB_SOURCES "${ORIG_WWTRANSLATEDB}/*.cpp")

add_library(wwtranslatedb STATIC ${WWTRANSLATEDB_SOURCES})

target_include_directories(wwtranslatedb BEFORE PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_WWTRANSLATEDB}"
    "${ORIG_SRC}/wwsaveload"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/WWMath"
)

target_link_libraries(wwtranslatedb wwsaveload wwlib wwdebug wwmath)
