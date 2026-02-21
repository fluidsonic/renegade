// osdep.h compat shim for macOS - Unix compatibility
#pragma once
#ifndef OSDEP_H_COMPAT
#define OSDEP_H_COMPAT

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <ctype.h>
#include <unistd.h>

// _strdup (for code that doesn't include always.h)
#ifndef _strdup
#define _strdup strdup
#endif

// Case-insensitive string compare
#ifndef stricmp
#define stricmp  strcasecmp
#endif
#ifndef strcmpi
#define strcmpi  strcasecmp
#endif
#ifndef strnicmp
#define strnicmp strncasecmp
#endif
#ifndef _stricmp
#define _stricmp strcasecmp
#endif
#ifndef _strcmpi
#define _strcmpi strcasecmp
#endif
#ifndef _strnicmp
#define _strnicmp strncasecmp
#endif

// Wide string case-insensitive comparison (Windows-specific names missing on macOS)
#include <wchar.h>
#ifndef wcsicmp
#define wcsicmp   wcscasecmp
#endif
#ifndef wcsnicmp
#define wcsnicmp  wcsncasecmp
#endif
#ifndef _wcsicmp
#define _wcsicmp  wcscasecmp
#endif
#ifndef _wcsnicmp
#define _wcsnicmp wcsncasecmp
#endif

// snprintf (available on macOS but sometimes used with _snprintf)
#ifndef _snprintf
#define _snprintf snprintf
#endif
#ifndef _vsnprintf
#define _vsnprintf vsnprintf
#endif

// strupr / strlwr (not in POSIX) - also defined in windef.h with _STRUPR_DEFINED guard
#ifndef _STRUPR_DEFINED
#define _STRUPR_DEFINED
inline char* strupr(char* s) {
    char* p = s;
    while (*p) { *p = (char)toupper((unsigned char)*p); p++; }
    return s;
}
inline char* strlwr(char* s) {
    char* p = s;
    while (*p) { *p = (char)tolower((unsigned char)*p); p++; }
    return s;
}
#define _strupr strupr
#define _strlwr strlwr
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

// GetTickCount - milliseconds since epoch (for _UNIX code paths that skip win.h)
#ifndef GETTICKCOUNT_DEFINED
#define GETTICKCOUNT_DEFINED
#include <stdint.h>
#include <sys/time.h>
inline unsigned int GetTickCount() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (unsigned int)(tv.tv_sec * 1000 + tv.tv_usec / 1000);
}
#endif

// min/max (already defined in always.h via template, but ensure macros work)
#ifndef min
#define min(a,b) ((a)<(b)?(a):(b))
#endif
#ifndef max
#define max(a,b) ((a)>(b)?(a):(b))
#endif

// Sleep is provided by winbase.h as an inline function; don't redefine as macro here

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

#endif // OSDEP_H_COMPAT
