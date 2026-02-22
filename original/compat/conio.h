#pragma once

#include "global.h"

// conio.h compat shim for macOS

#include <stdio.h>

// Console I/O stubs
inline int _kbhit(void) { return 0; }
inline int _getch(void) { return getchar(); }
inline int _getche(void) { return getchar(); }
inline int _putch(int c) { return putchar(c); }
#ifndef _CPRINTF_DEFINED
#define _CPRINTF_DEFINED
#include <stdarg.h>
inline int cprintf(const char* fmt, ...) {
    va_list ap; va_start(ap, fmt); int r = vprintf(fmt, ap); va_end(ap); return r;
}
#endif
