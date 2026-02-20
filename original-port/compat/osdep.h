// osdep.h compat shim for macOS - Unix compatibility
#pragma once
#ifndef OSDEP_H_COMPAT
#define OSDEP_H_COMPAT

#include <string.h>
#include <strings.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <ctype.h>
#include <unistd.h>

// Case-insensitive string compare (already in strings.h on macOS)
#ifndef stricmp
#define stricmp  strcasecmp
#endif
#ifndef strnicmp
#define strnicmp strncasecmp
#endif
#ifndef _stricmp
#define _stricmp strcasecmp
#endif
#ifndef _strnicmp
#define _strnicmp strncasecmp
#endif

// snprintf (available on macOS but sometimes used with _snprintf)
#ifndef _snprintf
#define _snprintf snprintf
#endif
#ifndef _vsnprintf
#define _vsnprintf vsnprintf
#endif

// strupr / strlwr (not in POSIX)
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

// min/max (already defined in always.h via template, but ensure macros work)
#ifndef min
#define min(a,b) ((a)<(b)?(a):(b))
#endif
#ifndef max
#define max(a,b) ((a)>(b)?(a):(b))
#endif

// Compact sleep
#define Sleep(ms) usleep((ms)*1000)

#endif // OSDEP_H_COMPAT
