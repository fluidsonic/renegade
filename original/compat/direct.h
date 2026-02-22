#pragma once

#include "global.h"

// direct.h compat shim for macOS

#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdlib.h>

inline int _mkdir(const char* path) { return mkdir(path, 0755); }
inline int _rmdir(const char* path) { return rmdir(path); }
inline int _chdir(const char* path) { return chdir(path); }
inline char* _getcwd(char* buf, int size) { return getcwd(buf, size); }
