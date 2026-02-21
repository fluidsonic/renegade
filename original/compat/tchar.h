// tchar.h compat shim for macOS - TCHAR as plain char (MBCS/ASCII mode)
#pragma once
#ifndef TCHAR_H_COMPAT
#define TCHAR_H_COMPAT

#include <string.h>
#include <stdio.h>
#include <stdarg.h>

typedef char    TCHAR;
typedef char*   LPTSTR;
typedef const char* LPCTSTR;

#define _T(x)           x
#define TEXT(x)         x

// String functions
#define _tcslen         strlen
#define _tcsclen        strlen
#define _tcscpy         strcpy
#define _tcsncpy        strncpy
#define _tcscat         strcat
#define _tcscmp         strcmp
#define _tcsncmp        strncmp
#define _tcsicmp        strcasecmp
#define _tcsnicmp       strncasecmp
#define _tcschr         strchr
#define _tcsrchr        strrchr
#define _tcsstr         strstr
#define _tcstok         strtok
#define _tcstol         strtol
#define _tcstoul        strtoul
#define _tcstod         strtod
#define _tcsupr         strupr
#define _tcslwr         strlwr
#define _tcsdup         strdup

// Printf functions
#define _tprintf        printf
#define _ftprintf       fprintf
#define _stprintf       sprintf
#define _sntprintf      snprintf
#define _vstprintf      vsprintf
#define _vsntprintf     vsnprintf
#define _tfscanf        fscanf
#define _stscanf        sscanf

// File functions
#define _tfopen         fopen
#define _tremove        remove
#define _trename        rename

// Wide string functions - WCHAR = wchar_t so standard wcs* functions work directly
#include <wchar.h>
#include <wctype.h>

// wcstrim - trim leading and trailing whitespace in-place (not in POSIX)
inline wchar_t* wcstrim(wchar_t* s) {
    if (!s) return s;
    size_t len = wcslen(s);
    while (len > 0 && s[len-1] <= 32) len--;
    s[len] = 0;
    size_t start = 0;
    while (s[start] && s[start] <= 32) start++;
    if (start > 0) {
        size_t i;
        for (i = 0; s[start + i]; i++) s[i] = s[start + i];
        s[i] = 0;
    }
    return s;
}

// _wcsupr / _wcslwr - not in POSIX
inline wchar_t* _wcsupr(wchar_t* s) {
    wchar_t* p = s;
    while (*p) { *p = (wchar_t)towupper(*p); p++; }
    return s;
}
inline wchar_t* _wcslwr(wchar_t* s) {
    wchar_t* p = s;
    while (*p) { *p = (wchar_t)towlower(*p); p++; }
    return s;
}
#define wcsupr  _wcsupr
#define wcslwr  _wcslwr

// Case-insensitive wide string comparison (Windows names)
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

// Char classification
#define _istalpha       isalpha
#define _istalnum       isalnum
#define _istdigit       isdigit
#define _istspace       isspace
#define _istupper       isupper
#define _istlower       islower
#define _totupper       toupper
#define _totlower       tolower

#endif // TCHAR_H_COMPAT
