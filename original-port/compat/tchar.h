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
