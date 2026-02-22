#pragma once

#include "global.h"

// osdep.h compat shim for macOS - Unix compatibility


#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <unistd.h>

// _strdup (for code that doesn't include always.h)
#ifndef _strdup
#define _strdup strdup
#endif

// snprintf (available on macOS but sometimes used with _snprintf)
#ifndef _snprintf
#define _snprintf snprintf
#endif
#ifndef _vsnprintf
#define _vsnprintf vsnprintf
#endif

// itoa / _itoa
inline char* _itoa(int value, char* str, int radix) {
    if (radix == 10) sprintf(str, "%d", value);
    else if (radix == 16) sprintf(str, "%x", value);
    else sprintf(str, "%d", value);
    return str;
}
inline char* itoa(int value, char* str, int radix) { return _itoa(value, str, radix); }

// strrev (not in POSIX)
inline char* strrev(char* s) {
    char* start = s;
    char* end = s + strlen(s) - 1;
    while (start < end) {
        char tmp = *start; *start = *end; *end = tmp;
        start++; end--;
    }
    return s;
}
#define _strrev strrev

// _alloca - stack allocation
#include <alloca.h>
#ifndef _alloca
#define _alloca alloca
#endif

// Path manipulation (MSVC _splitpath/_makepath)
#define _MAX_DRIVE  3
#define _MAX_DIR    256
#define _MAX_FNAME  256
#define _MAX_EXT    256
#include <libgen.h>
inline void _splitpath(const char* path, char* drive, char* dir, char* fname, char* ext) {
    if (drive) drive[0] = '\0';
    char tmp1[1024], tmp2[1024];
    strncpy(tmp1, path ? path : "", 1023); tmp1[1023] = 0;
    strncpy(tmp2, path ? path : "", 1023); tmp2[1023] = 0;
    if (dir) {
        const char* d = dirname(tmp1);
        strncpy(dir, d, _MAX_DIR-1); dir[_MAX_DIR-1] = '\0';
        if (dir[0] && dir[1] == '\0' && dir[0] == '.') dir[0] = '\0';
        else { int n = strlen(dir); if (n && dir[n-1] != '/') { dir[n] = '/'; dir[n+1] = '\0'; } }
    }
    if (fname || ext) {
        const char* b = basename(tmp2);
        if (fname) {
            strncpy(fname, b, _MAX_FNAME-1); fname[_MAX_FNAME-1] = '\0';
            char* dot = strrchr(fname, '.');
            if (dot) { if (ext) strncpy(ext, dot, _MAX_EXT-1); *dot = '\0'; }
            else if (ext) ext[0] = '\0';
        } else if (ext) {
            char tmpb[1024]; strncpy(tmpb, b, 1023); tmpb[1023] = 0;
            char* dot = strrchr(tmpb, '.');
            if (dot) strncpy(ext, dot, _MAX_EXT-1); else ext[0] = '\0';
        }
    }
}
inline void _makepath(char* path, const char* drive, const char* dir, const char* fname, const char* ext) {
    if (!path) return;
    path[0] = '\0';
    if (dir) strcat(path, dir);
    if (fname) strcat(path, fname);
    if (ext) strcat(path, ext);
}
