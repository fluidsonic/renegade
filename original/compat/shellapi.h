#pragma once

#include "global.h"

// shellapi.h compat shim for macOS


// ShellExecute stub - launches a URL or file
inline HINSTANCE ShellExecute(HWND hwnd, LPCSTR op, LPCSTR file,
                               LPCSTR params, LPCSTR dir, int show) {
    // On macOS, use 'open' command
    if (file) {
        char cmd[1024];
        snprintf(cmd, sizeof(cmd), "open \"%s\" &", file);
        system(cmd);
    }
    return (HINSTANCE)(intptr_t)32; // > 32 means success
}
inline HINSTANCE ShellExecuteA(HWND hwnd, LPCSTR op, LPCSTR file,
                                LPCSTR params, LPCSTR dir, int show) {
    return ShellExecute(hwnd, op, file, params, dir, show);
}

inline HINSTANCE FindExecutable(LPCSTR lpFile, LPCSTR lpDir, LPSTR lpResult) {
    if (lpResult) lpResult[0] = '\0';
    return (HINSTANCE)(intptr_t)33; // > 32 means success
}

#define SE_ERR_FNF    2
#define SE_ERR_PNF    3
#define SE_ERR_OOM    8
#define SW_SHOW       5
#define SW_SHOWNORMAL 1
