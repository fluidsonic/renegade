set(WWDEBUG_SRC "${ORIG_SRC}/wwdebug")

add_library(wwdebug STATIC
    "${WWDEBUG_SRC}/wwdebug.cpp"
    "${WWDEBUG_SRC}/wwmemlog.cpp"
    "${WWDEBUG_SRC}/wwprofile.cpp"
)

target_include_directories(wwdebug PRIVATE
    "${COMPAT_DIR}"
    "${ORIG_SRC}/wwdebug"
    "${ORIG_SRC}/wwlib"
    "${ORIG_SRC}/WWMath"
)

target_link_libraries(wwdebug PRIVATE Threads::Threads)
