// winfile.cpp — POSIX implementation of Win32 FindFirstFile / FindNextFile / FindClose
#include "winbase.h"
#include <dirent.h>
#include <fnmatch.h>
#include <sys/stat.h>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <cerrno>

struct FindState {
    DIR*  dir;
    char  dir_path[512];   // directory to enumerate
    char  pattern[260];    // filename glob pattern (no directory prefix)
};

// Fill a WIN32_FIND_DATA from a directory entry.
static bool fill_find_data(const char* dir_path, const char* name, LPWIN32_FIND_DATA fd)
{
    // Build full path for stat
    char full[800];
    snprintf(full, sizeof(full), "%s/%s", dir_path, name);

    struct stat st;
    if (stat(full, &st) != 0) return false;

    fd->dwFileAttributes = S_ISDIR(st.st_mode)
        ? FILE_ATTRIBUTE_DIRECTORY
        : FILE_ATTRIBUTE_NORMAL;
    fd->nFileSizeLow  = (DWORD)(st.st_size & 0xFFFFFFFF);
    fd->nFileSizeHigh = (DWORD)((st.st_size >> 32) & 0xFFFFFFFF);
    fd->dwReserved0   = 0;
    fd->dwReserved1   = 0;
    memset(&fd->ftCreationTime,   0, sizeof(FILETIME));
    memset(&fd->ftLastAccessTime, 0, sizeof(FILETIME));
    memset(&fd->ftLastWriteTime,  0, sizeof(FILETIME));
    strncpy(fd->cFileName, name, sizeof(fd->cFileName) - 1);
    fd->cFileName[sizeof(fd->cFileName) - 1] = '\0';
    fd->cAlternateFileName[0] = '\0';
    return true;
}

// Find the next matching entry in the directory.
static bool find_next(FindState* fs, LPWIN32_FIND_DATA fd)
{
    struct dirent* entry;
    while ((entry = readdir(fs->dir)) != nullptr) {
        const char* name = entry->d_name;
        // Skip . and ..
        if (name[0] == '.' && (name[1] == '\0' || (name[1] == '.' && name[2] == '\0')))
            continue;
        int match = fnmatch(fs->pattern, name, FNM_CASEFOLD);
        if (match == 0) {
            if (fill_find_data(fs->dir_path, name, fd))
                return true;
        }
    }
    return false;
}

// Split "data\*.mix" into dir_path="data" and pattern="*.mix".
// Normalises backslashes and handles no-directory case.
static void split_pattern(const char* raw_pattern, char* dir_out, size_t dir_size,
                           char* pat_out, size_t pat_size)
{
    // Normalise backslash → forward slash
    char norm[800];
    size_t len = strlen(raw_pattern);
    if (len >= sizeof(norm)) len = sizeof(norm) - 1;
    for (size_t i = 0; i < len; ++i)
        norm[i] = (raw_pattern[i] == '\\') ? '/' : raw_pattern[i];
    norm[len] = '\0';

    // Find last slash
    const char* last_slash = strrchr(norm, '/');
    if (last_slash) {
        size_t dir_len = (size_t)(last_slash - norm);
        if (dir_len >= dir_size) dir_len = dir_size - 1;
        memcpy(dir_out, norm, dir_len);
        dir_out[dir_len] = '\0';
        strncpy(pat_out, last_slash + 1, pat_size - 1);
        pat_out[pat_size - 1] = '\0';
    } else {
        // No directory — use "."
        strncpy(dir_out, ".", dir_size - 1);
        dir_out[dir_size - 1] = '\0';
        strncpy(pat_out, norm, pat_size - 1);
        pat_out[pat_size - 1] = '\0';
    }
}

HANDLE FindFirstFile(LPCSTR raw_pattern, LPWIN32_FIND_DATA fd)
{
    char dir_path[512], pattern[260];
    split_pattern(raw_pattern, dir_path, sizeof(dir_path), pattern, sizeof(pattern));

    fprintf(stderr, "[FindFirstFile] pattern='%s' -> dir='%s' glob='%s'\n", raw_pattern, dir_path, pattern);

    DIR* dir = opendir(dir_path);
    if (!dir) {
        fprintf(stderr, "[FindFirstFile] opendir('%s') failed: %s\n", dir_path, strerror(errno));
        return INVALID_HANDLE_VALUE;
    }

    FindState* fs = (FindState*)malloc(sizeof(FindState));
    if (!fs) { closedir(dir); return INVALID_HANDLE_VALUE; }

    fs->dir = dir;
    strncpy(fs->dir_path, dir_path, sizeof(fs->dir_path) - 1);
    fs->dir_path[sizeof(fs->dir_path) - 1] = '\0';
    strncpy(fs->pattern, pattern, sizeof(fs->pattern) - 1);
    fs->pattern[sizeof(fs->pattern) - 1] = '\0';

    if (!find_next(fs, fd)) {
        closedir(dir);
        free(fs);
        return INVALID_HANDLE_VALUE;
    }
    return (HANDLE)fs;
}

HANDLE FindFirstFileA(LPCSTR pattern, LPWIN32_FIND_DATA fd)
{
    return FindFirstFile(pattern, fd);
}

BOOL FindNextFile(HANDLE h, LPWIN32_FIND_DATA fd)
{
    if (h == INVALID_HANDLE_VALUE || !h) return FALSE;
    FindState* fs = (FindState*)h;
    return find_next(fs, fd) ? TRUE : FALSE;
}

BOOL FindNextFileA(HANDLE h, LPWIN32_FIND_DATA fd)
{
    return FindNextFile(h, fd);
}

BOOL FindClose(HANDLE h)
{
    if (h == INVALID_HANDLE_VALUE || !h) return FALSE;
    FindState* fs = (FindState*)h;
    closedir(fs->dir);
    free(fs);
    return TRUE;
}
