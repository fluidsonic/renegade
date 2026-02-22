#pragma once

#include "global.h"

// tchar.h compat shim for macOS - TCHAR as plain char (MBCS/ASCII mode)

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

// Wide string functions — WCHAR = char16_t, use c16s* functions

// wcstrim is declared in trim.h and defined in trim.cpp — no redefinition here.

// _wcsupr / _wcslwr - not in POSIX
inline char16_t* _wcsupr(char16_t* s) { return c16supr(s); }
inline char16_t* _wcslwr(char16_t* s) { return c16slwr(s); }
#define wcsupr  _wcsupr
#define wcslwr  _wcslwr

// Case-insensitive wide string comparison (Windows names) — char16_t versions
#ifndef WCSICMP_COMPAT_DEFINED
#define WCSICMP_COMPAT_DEFINED
inline int wcsicmp (const char16_t* a, const char16_t* b)          { return c16sicmp(a, b); }
inline int wcsnicmp(const char16_t* a, const char16_t* b, size_t n) { return c16snicmp(a, b, n); }
inline int _wcsicmp (const char16_t* a, const char16_t* b)         { return c16sicmp(a, b); }
inline int _wcsnicmp(const char16_t* a, const char16_t* b, size_t n){ return c16snicmp(a, b, n); }
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
