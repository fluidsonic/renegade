// shlobj.h compat shim for macOS - Shell API stubs
#pragma once
#ifndef SHLOBJ_H_COMPAT
#define SHLOBJ_H_COMPAT

#include "windef.h"

// Special folder IDs
#define CSIDL_DESKTOP       0x0000
#define CSIDL_PROGRAMS      0x0002
#define CSIDL_PERSONAL      0x0005
#define CSIDL_FAVORITES     0x0006
#define CSIDL_STARTUP       0x0007
#define CSIDL_RECENT        0x0008
#define CSIDL_SENDTO        0x0009
#define CSIDL_STARTMENU     0x000b
#define CSIDL_MYMUSIC       0x000d
#define CSIDL_MYVIDEO       0x000e
#define CSIDL_DESKTOPDIRECTORY 0x0010
#define CSIDL_DRIVES        0x0011
#define CSIDL_APPDATA       0x001a
#define CSIDL_LOCAL_APPDATA 0x001c
#define CSIDL_COMMON_APPDATA 0x0023
#define CSIDL_WINDOWS       0x0024
#define CSIDL_SYSTEM        0x0025
#define CSIDL_PROGRAM_FILES 0x0026
#define CSIDL_MYPICTURES    0x0027
#define CSIDL_PROFILE       0x0028
#define CSIDL_COMMON_DOCUMENTS 0x002e
#define CSIDL_FLAG_CREATE   0x8000

// SHGetFolderPath stub
#include <stdlib.h>
inline HRESULT SHGetFolderPath(HWND hwnd, int nFolder, HANDLE hToken, DWORD dwFlags, LPSTR pszPath) {
    if (!pszPath) return E_FAIL;
    const char* home = getenv("HOME");
    if (!home) home = "/tmp";
    // Map common folders to Unix equivalents
    switch (nFolder & 0x7fff) {
        case CSIDL_APPDATA:
        case CSIDL_LOCAL_APPDATA:
            snprintf(pszPath, MAX_PATH, "%s/.local/share", home);
            break;
        case CSIDL_PERSONAL:
            snprintf(pszPath, MAX_PATH, "%s/Documents", home);
            break;
        case CSIDL_DESKTOP:
        case CSIDL_DESKTOPDIRECTORY:
            snprintf(pszPath, MAX_PATH, "%s/Desktop", home);
            break;
        default:
            snprintf(pszPath, MAX_PATH, "%s", home);
            break;
    }
    return S_OK;
}

inline HRESULT SHGetFolderPathA(HWND hwnd, int nFolder, HANDLE hToken, DWORD dwFlags, LPSTR pszPath) {
    return SHGetFolderPath(hwnd, nFolder, hToken, dwFlags, pszPath);
}

// SHGetSpecialFolderPath stub
inline BOOL SHGetSpecialFolderPath(HWND hwnd, LPSTR pszPath, int nFolder, BOOL fCreate) {
    return SUCCEEDED(SHGetFolderPath(hwnd, nFolder, NULL, 0, pszPath));
}

// SHFileOperation stub
typedef struct { HWND hwnd; UINT wFunc; LPCSTR pFrom, pTo; FILEOP_FLAGS fFlags; BOOL fAnyOpAborted; LPVOID hNameMappings; LPCSTR lpszProgressTitle; } SHFILEOPSTRUCT;
typedef WORD FILEOP_FLAGS;
#define FO_MOVE   1
#define FO_COPY   2
#define FO_DELETE 3
inline int SHFileOperation(SHFILEOPSTRUCT* op) { return 1; }

#endif // SHLOBJ_H_COMPAT
