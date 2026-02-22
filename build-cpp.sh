#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR/original"
BUILD_DIR="$SCRIPT_DIR/original/build"

needs_configure() {
  [ ! -f "$BUILD_DIR/build.ninja" ] && return 0
  # Check if CMake cached source dir still exists
  local cached_src
  cached_src=$(grep -m1 '^CMAKE_HOME_DIRECTORY:' "$BUILD_DIR/CMakeCache.txt" 2>/dev/null | cut -d= -f2)
  [ ! -d "$cached_src" ] && return 0
  return 1
}

if needs_configure; then
  echo "Configuring..."
  rm -f "$BUILD_DIR/CMakeCache.txt"
  cmake -S "$SRC_DIR" -B "$BUILD_DIR" -G Ninja
fi

cmake --build "$BUILD_DIR" --target Commando -j8 "$@"
