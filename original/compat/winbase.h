#pragma once

#include "global.h"

// winbase.h compat shim for macOS/clang

#ifdef __APPLE__
#include <mach-o/dyld.h>
#endif

#include "winnt.h"
#include <pthread.h>
#include <unistd.h>
#include <sys/time.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdexcept>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>
#include <dlfcn.h>

// DLL notification constants
#define DLL_PROCESS_ATTACH  1
#define DLL_THREAD_ATTACH   2
#define DLL_THREAD_DETACH   3
#define DLL_PROCESS_DETACH  0

// Sleep
inline void Sleep(DWORD milliseconds) {
    usleep(milliseconds * 1000);
}

// GetTickCount — defined in global.h (available via force-include)

// GetCurrentTime - Windows macro for GetTickCount (deprecated in modern SDK)
#ifndef GetCurrentTime
#define GetCurrentTime() GetTickCount()
#endif

// GetLastError / SetLastError — thread-local so CreateMutex/ERROR_ALREADY_EXISTS work
inline DWORD& _win_last_error() { thread_local DWORD e = 0; return e; }
inline DWORD GetLastError() { return _win_last_error(); }
inline void SetLastError(DWORD e) { _win_last_error() = e; }

// OutputDebugString -> stderr
inline void OutputDebugString(const char* str) {
    fprintf(stderr, "%s", str);
}
inline void OutputDebugStringA(const char* str) {
    fprintf(stderr, "%s", str);
}

// FormatMessage stub
#define FORMAT_MESSAGE_FROM_SYSTEM  0x1000
inline DWORD FormatMessage(DWORD flags, LPCVOID src, DWORD msgid, DWORD langid,
                           LPSTR buf, DWORD size, va_list* args) {
    if (buf && size > 0) {
        strncpy(buf, strerror(static_cast<int>(msgid)), size - 1);
        buf[size - 1] = 0;
    }
    return buf ? (DWORD)strlen(buf) : 0;
}

// ExitProcess
inline void ExitProcess(UINT code) {
    exit((int)code);
}

// GetModuleHandle stub
inline HMODULE GetModuleHandle(LPCSTR name) { return (HMODULE)1; }
inline HMODULE GetModuleHandleA(LPCSTR name) { return (HMODULE)1; }
inline BOOL GetModuleFileName(HMODULE mod, LPSTR buf, DWORD size) {
#ifdef __APPLE__
    uint32_t sz = size;
    if (buf && size > 0 && _NSGetExecutablePath(buf, &sz) == 0) return TRUE;
#endif
    if (buf && size > 0) buf[0] = 0;
    return FALSE;
}

// GetCommandLine stub
inline LPSTR GetCommandLine() { return (LPSTR)""; }
inline LPSTR GetCommandLineA() { return (LPSTR)""; }

// GetCurrentDirectoryA
#include <unistd.h>
inline DWORD GetCurrentDirectory(DWORD size, LPSTR buf) {
    if (getcwd(buf, size)) return (DWORD)strlen(buf);
    return 0;
}
inline DWORD GetCurrentDirectoryA(DWORD size, LPSTR buf) {
    return GetCurrentDirectory(size, buf);
}
inline BOOL SetCurrentDirectory(LPCSTR path) {
    return chdir(path) == 0;
}
inline BOOL SetCurrentDirectoryA(LPCSTR path) {
    return chdir(path) == 0;
}

// GetTempPath
inline DWORD GetTempPath(DWORD size, LPSTR buf) {
    const char* tmp = "/tmp";
    if (buf && size > 0) {
        strncpy(buf, tmp, size - 1);
        buf[size - 1] = 0;
    }
    return (DWORD)strlen(tmp);
}
inline DWORD GetTempPathA(DWORD size, LPSTR buf) {
    return GetTempPath(size, buf);
}

// File operations
#define CREATE_NEW          1
#define CREATE_ALWAYS       2
#define OPEN_EXISTING       3
#define OPEN_ALWAYS         4
#define TRUNCATE_EXISTING   5

#define FILE_SHARE_READ     0x00000001
#define FILE_SHARE_WRITE    0x00000002
#define FILE_SHARE_DELETE   0x00000004

#define FILE_ATTRIBUTE_NORMAL       0x00000080
#define FILE_ATTRIBUTE_READONLY     0x00000001
#define FILE_ATTRIBUTE_HIDDEN       0x00000002
#define FILE_ATTRIBUTE_DIRECTORY    0x00000010
#define FILE_ATTRIBUTE_ARCHIVE      0x00000020
#define FILE_ATTRIBUTE_TEMPORARY    0x00000100

#define FILE_BEGIN   0
#define FILE_CURRENT 1
#define FILE_END     2

inline HANDLE CreateFile(LPCSTR name, DWORD access, DWORD share,
                         LPVOID security, DWORD creation, DWORD flags, HANDLE tmpl) {
    int oflags = 0;
    if ((access & GENERIC_READ) && (access & GENERIC_WRITE)) oflags = O_RDWR;
    else if (access & GENERIC_WRITE) oflags = O_WRONLY;
    else oflags = O_RDONLY;
    if (creation == CREATE_ALWAYS) oflags |= O_CREAT | O_TRUNC;
    else if (creation == CREATE_NEW) oflags |= O_CREAT | O_EXCL;
    else if (creation == OPEN_ALWAYS) oflags |= O_CREAT;
    else if (creation == TRUNCATE_EXISTING) oflags |= O_TRUNC;
    int fd = open(name, oflags, 0644);
    if (fd < 0) return INVALID_HANDLE_VALUE;
    return (HANDLE)(intptr_t)fd;
}
inline HANDLE CreateFileA(LPCSTR name, DWORD access, DWORD share,
                          LPVOID security, DWORD creation, DWORD flags, HANDLE tmpl) {
    return CreateFile(name, access, share, security, creation, flags, tmpl);
}

// === WinHandle: tagged heap object for mutex and event HANDLE emulation ===
#define _WIN_HANDLE_MAGIC 0x57484E44u
struct WinHandle {
    unsigned magic;
    enum { WH_MUTEX, WH_EVENT } type;
    union {
        struct { pthread_mutex_t m; } mutex;
        struct { pthread_mutex_t m; pthread_cond_t c; int signaled; int manual; } event;
    };
};
static inline bool _is_win_handle(HANDLE h) {
    if (!h || (uintptr_t)h < 0x1000) return false;
    return ((WinHandle*)h)->magic == _WIN_HANDLE_MAGIC;
}

// Named handle registry (for singleton mutex detection)
struct _NamedEntry { char name[128]; WinHandle* h; };
static inline _NamedEntry* _win_named_registry() { static _NamedEntry r[32] = {}; return r; }
static inline int& _win_named_count()             { static int c = 0; return c; }
static inline pthread_mutex_t& _win_reg_lock()    { static pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER; return m; }

inline BOOL CloseHandle(HANDLE h) {
    if (!h || h == INVALID_HANDLE_VALUE) return FALSE;
    if (_is_win_handle(h)) {
        WinHandle* wh = (WinHandle*)h;
        // Remove from named registry
        pthread_mutex_lock(&_win_reg_lock());
        for (int i = 0; i < _win_named_count(); i++) {
            if (_win_named_registry()[i].h == wh) {
                for (int j = i; j < _win_named_count() - 1; j++)
                    _win_named_registry()[j] = _win_named_registry()[j + 1];
                _win_named_count()--;
                break;
            }
        }
        pthread_mutex_unlock(&_win_reg_lock());
        if (wh->type == WinHandle::WH_MUTEX)       pthread_mutex_destroy(&wh->mutex.m);
        else if (wh->type == WinHandle::WH_EVENT) { pthread_cond_destroy(&wh->event.c); pthread_mutex_destroy(&wh->event.m); }
        wh->magic = 0;
        free(wh);
        return TRUE;
    }
    if ((uintptr_t)h < 0x10000) return close((int)(intptr_t)h) == 0;  // file descriptor
    free(h);  // legacy thread handle (raw pthread_t*)
    return TRUE;
}

inline BOOL ReadFile(HANDLE h, LPVOID buf, DWORD to_read, LPDWORD read_out, LPVOID overlapped) {
    ssize_t r = read((int)(intptr_t)h, buf, to_read);
    if (r < 0) { if (read_out) *read_out = 0; return FALSE; }
    if (read_out) *read_out = (DWORD)r;
    return TRUE;
}

// Overload for 'unsigned long *' read_out (64-bit macOS: unsigned long != DWORD/unsigned int)
inline BOOL ReadFile(HANDLE h, LPVOID buf, DWORD to_read, unsigned long* read_out, LPVOID overlapped) {
    DWORD tmp = 0;
    BOOL r = ReadFile(h, buf, to_read, &tmp, overlapped);
    if (read_out) *read_out = tmp;
    return r;
}

inline BOOL WriteFile(HANDLE h, LPCVOID buf, DWORD to_write, LPDWORD written, LPVOID overlapped) {
    ssize_t r = write((int)(intptr_t)h, buf, to_write);
    if (r < 0) { if (written) *written = 0; return FALSE; }
    if (written) *written = (DWORD)r;
    return TRUE;
}

inline DWORD SetFilePointer(HANDLE h, LONG dist, LONG* dist_high, DWORD method) {
    int whence = (method == FILE_BEGIN) ? SEEK_SET :
                 (method == FILE_CURRENT) ? SEEK_CUR : SEEK_END;
    off_t r = lseek((int)(intptr_t)h, dist, whence);
    return (r == (off_t)-1) ? 0xFFFFFFFF : (DWORD)r;
}

inline DWORD GetFileSize(HANDLE h, LPDWORD size_high) {
    struct stat st;
    if (fstat((int)(intptr_t)h, &st) != 0) return 0xFFFFFFFF;
    if (size_high) *size_high = (DWORD)((st.st_size >> 32) & 0xFFFFFFFF);
    return (DWORD)(st.st_size & 0xFFFFFFFF);
}

inline BOOL DeleteFile(LPCSTR name) { return unlink(name) == 0; }
inline BOOL DeleteFileA(LPCSTR name) { return unlink(name) == 0; }

inline BOOL CopyFile(LPCSTR src, LPCSTR dst, BOOL fail_if_exists) {
    // Minimal implementation
    return FALSE;
}

inline BOOL MoveFile(LPCSTR src, LPCSTR dst) {
    return rename(src, dst) == 0;
}

inline BOOL CreateDirectory(LPCSTR path, LPVOID security) {
    return mkdir(path, 0755) == 0;
}
inline BOOL CreateDirectoryA(LPCSTR path, LPVOID security) {
    return mkdir(path, 0755) == 0;
}

inline BOOL RemoveDirectory(LPCSTR path) { return rmdir(path) == 0; }

inline DWORD GetFileAttributes(LPCSTR path) {
    struct stat st;
    if (stat(path, &st) != 0) return 0xFFFFFFFF;
    DWORD attrs = FILE_ATTRIBUTE_NORMAL;
    if (S_ISDIR(st.st_mode)) attrs = FILE_ATTRIBUTE_DIRECTORY;
    return attrs;
}
inline DWORD GetFileAttributesA(LPCSTR path) { return GetFileAttributes(path); }

// Memory file mapping stubs
#define PAGE_READWRITE  0x04
#define PAGE_READONLY   0x02
#define FILE_MAP_WRITE  0x02
#define FILE_MAP_READ   0x04
inline HANDLE CreateFileMapping(HANDLE h, LPVOID sec, DWORD protect, DWORD max_high, DWORD max_low, LPCSTR name) { return NULL; }
inline LPVOID MapViewOfFile(HANDLE h, DWORD access, DWORD off_high, DWORD off_low, SIZE_T bytes) { return NULL; }
inline BOOL UnmapViewOfFile(LPCVOID addr) { return FALSE; }

// Threading
typedef DWORD (*LPTHREAD_START_ROUTINE)(LPVOID);
typedef struct _SECURITY_ATTRIBUTES { DWORD nLength; LPVOID lpSecurityDescriptor; BOOL bInheritHandle; } SECURITY_ATTRIBUTES;
typedef SECURITY_ATTRIBUTES* LPSECURITY_ATTRIBUTES;

struct _ThreadArgs { LPTHREAD_START_ROUTINE fn; LPVOID arg; };
inline void* _thread_wrapper(void* p) {
    struct _ThreadArgs* ta = (struct _ThreadArgs*)p;
    ta->fn(ta->arg);
    free(ta);
    return NULL;
}
inline HANDLE CreateThread(LPSECURITY_ATTRIBUTES sec, SIZE_T stack, LPTHREAD_START_ROUTINE fn,
                           LPVOID arg, DWORD flags, LPDWORD id) {
    pthread_t* t = (pthread_t*)malloc(sizeof(pthread_t));
    struct _ThreadArgs* ta = (struct _ThreadArgs*)malloc(sizeof(struct _ThreadArgs));
    ta->fn = fn; ta->arg = arg;
    if (pthread_create(t, NULL, _thread_wrapper, ta) != 0) {
        free(t); free(ta); return NULL;
    }
    if (id) *id = (DWORD)(uintptr_t)*t;
    return (HANDLE)t;
}

inline DWORD WaitForSingleObject(HANDLE h, DWORD timeout) {
    if (!h) return WAIT_FAILED;
    if (_is_win_handle(h)) {
        WinHandle* wh = (WinHandle*)h;
        if (wh->type == WinHandle::WH_MUTEX) {
            if (timeout == 0) return pthread_mutex_trylock(&wh->mutex.m) == 0 ? WAIT_OBJECT_0 : WAIT_TIMEOUT;
            pthread_mutex_lock(&wh->mutex.m);
            return WAIT_OBJECT_0;
        }
        if (wh->type == WinHandle::WH_EVENT) {
            pthread_mutex_lock(&wh->event.m);
            while (!wh->event.signaled)
                pthread_cond_wait(&wh->event.c, &wh->event.m);
            if (!wh->event.manual) wh->event.signaled = 0;
            pthread_mutex_unlock(&wh->event.m);
            return WAIT_OBJECT_0;
        }
        return WAIT_FAILED;
    }
    if ((uintptr_t)h < 0x1000) return WAIT_OBJECT_0;
    pthread_join(*(pthread_t*)h, NULL);  // thread handle
    return WAIT_OBJECT_0;
}

inline BOOL TerminateThread(HANDLE h, DWORD code) { return FALSE; }

inline DWORD GetCurrentThreadId() {
    return (DWORD)(uintptr_t)pthread_self();
}

inline HANDLE GetCurrentThread() { return (HANDLE)(uintptr_t)pthread_self(); }
inline HANDLE GetCurrentProcess() { return (HANDLE)(uintptr_t)getpid(); }
inline DWORD GetCurrentProcessId() { return (DWORD)getpid(); }

// Events — real pthread_cond_t based
#define EVENT_MODIFY_STATE 0x0002
inline HANDLE CreateEvent(LPSECURITY_ATTRIBUTES sec, BOOL manual, BOOL initial, LPCSTR name) {
    WinHandle* wh = (WinHandle*)malloc(sizeof(WinHandle));
    wh->magic = _WIN_HANDLE_MAGIC;
    wh->type = WinHandle::WH_EVENT;
    pthread_mutex_init(&wh->event.m, NULL);
    pthread_cond_init(&wh->event.c, NULL);
    wh->event.signaled = initial ? 1 : 0;
    wh->event.manual = manual ? 1 : 0;
    return (HANDLE)wh;
}
inline HANDLE CreateEventA(LPSECURITY_ATTRIBUTES sec, BOOL manual, BOOL initial, LPCSTR name) {
    return CreateEvent(sec, manual, initial, name);
}
inline HANDLE OpenEvent(DWORD access, BOOL inherit, LPCSTR name) { return NULL; }
inline BOOL SetEvent(HANDLE h) {
    if (!_is_win_handle(h)) return FALSE;
    WinHandle* wh = (WinHandle*)h;
    if (wh->type != WinHandle::WH_EVENT) return FALSE;
    pthread_mutex_lock(&wh->event.m);
    wh->event.signaled = 1;
    if (wh->event.manual) pthread_cond_broadcast(&wh->event.c);
    else pthread_cond_signal(&wh->event.c);
    pthread_mutex_unlock(&wh->event.m);
    return TRUE;
}
inline BOOL ResetEvent(HANDLE h) {
    if (!_is_win_handle(h)) return FALSE;
    WinHandle* wh = (WinHandle*)h;
    if (wh->type != WinHandle::WH_EVENT) return FALSE;
    pthread_mutex_lock(&wh->event.m);
    wh->event.signaled = 0;
    pthread_mutex_unlock(&wh->event.m);
    return TRUE;
}
inline BOOL PulseEvent(HANDLE h) { SetEvent(h); ResetEvent(h); return TRUE; }

// Mutexes — real pthread_mutex_t with named registry for singleton detection
#ifndef ERROR_ALREADY_EXISTS
#define ERROR_ALREADY_EXISTS 183
#endif
#define MUTEX_ALL_ACCESS 0x1F0001
inline HANDLE CreateMutex(LPSECURITY_ATTRIBUTES sec, BOOL initial_owner, LPCSTR name) {
    pthread_mutex_lock(&_win_reg_lock());
    if (name) {
        for (int i = 0; i < _win_named_count(); i++) {
            if (strncmp(_win_named_registry()[i].name, name, 127) == 0) {
                WinHandle* existing = _win_named_registry()[i].h;
                pthread_mutex_unlock(&_win_reg_lock());
                SetLastError(ERROR_ALREADY_EXISTS);
                return (HANDLE)existing;
            }
        }
    }
    WinHandle* wh = (WinHandle*)malloc(sizeof(WinHandle));
    wh->magic = _WIN_HANDLE_MAGIC;
    wh->type = WinHandle::WH_MUTEX;
    pthread_mutex_init(&wh->mutex.m, NULL);
    if (initial_owner) pthread_mutex_lock(&wh->mutex.m);
    if (name && _win_named_count() < 32) {
        strncpy(_win_named_registry()[_win_named_count()].name, name, 127);
        _win_named_registry()[_win_named_count()].h = wh;
        _win_named_count()++;
    }
    SetLastError(0);
    pthread_mutex_unlock(&_win_reg_lock());
    return (HANDLE)wh;
}
inline HANDLE OpenMutex(DWORD access, BOOL inherit, LPCSTR name) {
    if (!name) return NULL;
    pthread_mutex_lock(&_win_reg_lock());
    for (int i = 0; i < _win_named_count(); i++) {
        if (strncmp(_win_named_registry()[i].name, name, 127) == 0) {
            WinHandle* h = _win_named_registry()[i].h;
            pthread_mutex_unlock(&_win_reg_lock());
            return (HANDLE)h;
        }
    }
    pthread_mutex_unlock(&_win_reg_lock());
    return NULL;
}
inline BOOL ReleaseMutex(HANDLE h) {
    if (!_is_win_handle(h)) return FALSE;
    WinHandle* wh = (WinHandle*)h;
    if (wh->type != WinHandle::WH_MUTEX) return FALSE;
    pthread_mutex_unlock(&wh->mutex.m);
    return TRUE;
}

// FILETIME (needed by registry APIs)
#ifndef _FILETIME_
#define _FILETIME_
typedef struct _FILETIME { DWORD dwLowDateTime; DWORD dwHighDateTime; } FILETIME, *LPFILETIME;
#endif

// Registry stubs
typedef HKEY* PHKEY;
#define HKEY_LOCAL_MACHINE  ((HKEY)(uintptr_t)0x80000002)
#define HKEY_CURRENT_USER   ((HKEY)(uintptr_t)0x80000001)
#define HKEY_CLASSES_ROOT   ((HKEY)(uintptr_t)0x80000000)
#define KEY_READ            0x20019
#define KEY_WRITE           0x20006
#define KEY_ALL_ACCESS      0xF003F
#define REG_SZ              1
#define REG_DWORD           4
#define REG_BINARY          3
#define REG_EXPAND_SZ       2
#define ERROR_SUCCESS       0
#define ERROR_FILE_NOT_FOUND 2
#define ERROR_NO_MORE_ITEMS 259

inline LONG RegOpenKeyEx(HKEY key, LPCSTR sub, DWORD opt, DWORD sam, PHKEY result) {
    if (result) *result = NULL;
    return ERROR_FILE_NOT_FOUND;
}
inline LONG RegOpenKeyExA(HKEY key, LPCSTR sub, DWORD opt, DWORD sam, PHKEY result) {
    return RegOpenKeyEx(key, sub, opt, sam, result);
}
inline LONG RegCloseKey(HKEY key) { return ERROR_SUCCESS; }
inline LONG RegQueryValueEx(HKEY key, LPCSTR name, LPDWORD reserved, LPDWORD type, LPBYTE data, LPDWORD size) {
    return ERROR_FILE_NOT_FOUND;
}
inline LONG RegQueryValueExA(HKEY key, LPCSTR name, LPDWORD reserved, LPDWORD type, LPBYTE data, LPDWORD size) {
    return RegQueryValueEx(key, name, reserved, type, data, size);
}
inline LONG RegSetValueEx(HKEY key, LPCSTR name, DWORD reserved, DWORD type, const BYTE* data, DWORD size) {
    return ERROR_SUCCESS;
}
inline LONG RegSetValueExA(HKEY key, LPCSTR name, DWORD reserved, DWORD type, const BYTE* data, DWORD size) {
    return RegSetValueEx(key, name, reserved, type, data, size);
}
inline LONG RegCreateKeyEx(HKEY key, LPCSTR sub, DWORD reserved, LPSTR cls, DWORD opts, DWORD sam,
                           LPSECURITY_ATTRIBUTES sec, PHKEY result, LPDWORD disp) {
    if (result) *result = NULL;
    return ERROR_SUCCESS;
}
inline LONG RegCreateKeyExA(HKEY key, LPCSTR sub, DWORD reserved, LPSTR cls, DWORD opts, DWORD sam,
                             LPSECURITY_ATTRIBUTES sec, PHKEY result, LPDWORD disp) {
    return RegCreateKeyEx(key, sub, reserved, cls, opts, sam, sec, result, disp);
}
inline LONG RegDeleteKey(HKEY key, LPCSTR sub) { return ERROR_SUCCESS; }
inline LONG RegDeleteKeyA(HKEY key, LPCSTR sub) { return ERROR_SUCCESS; }
inline LONG RegEnumKeyEx(HKEY key, DWORD idx, LPSTR name, LPDWORD name_len, LPDWORD reserved,
                         LPSTR cls, LPDWORD cls_len, FILETIME* ft) { return ERROR_NO_MORE_ITEMS; }
inline LONG RegEnumValue(HKEY key, DWORD idx, LPSTR name, void* name_len, void* reserved,
                         void* type, void* data, void* size) { return ERROR_NO_MORE_ITEMS; }

// wsprintf / wsprintfA (wide-ish printf)
inline int wsprintf(LPSTR buf, LPCSTR fmt, ...) {
    va_list va;
    va_start(va, fmt);
    int r = vsprintf(buf, fmt, va);
    va_end(va);
    return r;
}

// FindFirst/FindNext file stubs
#include <dirent.h>
typedef struct {
    DWORD    dwFileAttributes;
    FILETIME ftCreationTime;
    FILETIME ftLastAccessTime;
    FILETIME ftLastWriteTime;
    DWORD    nFileSizeHigh;
    DWORD    nFileSizeLow;
    DWORD    dwReserved0;
    DWORD    dwReserved1;
    char     cFileName[260];
    char     cAlternateFileName[14];
} WIN32_FIND_DATA, *LPWIN32_FIND_DATA;
typedef WIN32_FIND_DATA WIN32_FIND_DATAA;
typedef LPWIN32_FIND_DATA LPWIN32_FIND_DATAA;

// Implemented in compat/winfile.cpp using POSIX opendir/readdir/fnmatch
HANDLE FindFirstFile(LPCSTR pattern, LPWIN32_FIND_DATA fd);
HANDLE FindFirstFileA(LPCSTR pattern, LPWIN32_FIND_DATA fd);
BOOL   FindNextFile(HANDLE h, LPWIN32_FIND_DATA fd);
BOOL   FindNextFileA(HANDLE h, LPWIN32_FIND_DATA fd);
BOOL   FindClose(HANDLE h);

// Misc
inline BOOL FlushFileBuffers(HANDLE h) { return TRUE; }
inline BOOL SetEndOfFile(HANDLE h) { return FALSE; }

// Interlocked operations
inline LONG InterlockedIncrement(volatile LONG* p) { return __atomic_add_fetch(p, 1, __ATOMIC_SEQ_CST); }
inline LONG InterlockedDecrement(volatile LONG* p) { return __atomic_sub_fetch(p, 1, __ATOMIC_SEQ_CST); }
inline LONG InterlockedExchange(volatile LONG* p, LONG val) {
    LONG old = val;
    __atomic_exchange(p, &val, &old, __ATOMIC_SEQ_CST);
    return old;
}
inline LONG InterlockedCompareExchange(volatile LONG* dest, LONG exch, LONG comp) {
    LONG tmp = comp;
    __atomic_compare_exchange(dest, &tmp, &exch, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
    return tmp;
}

// Dynamic library loading (POSIX dlopen/dlsym/dlclose)
// Windows DLLs that don't exist on macOS get a sentinel handle so callers don't bail out.
typedef intptr_t (*FARPROC)();

#define _WINCOMPAT_D3D8_HANDLE ((HMODULE)(intptr_t)0xD3D80000)

inline HMODULE LoadLibrary(LPCSTR name) {
    if (!name) return NULL;
    // Build lowercase copy for pattern matching.
    char lower[256]; int i = 0;
    while (name[i] && i < 255) { lower[i] = (char)((name[i] >= 'A' && name[i] <= 'Z') ? name[i]+32 : name[i]); i++; }
    lower[i] = 0;
    // D3D8 is handled entirely in the compat layer.
    if (strstr(lower, "d3d8")) return _WINCOMPAT_D3D8_HANDLE;
    // Try loading as-is first (e.g., the caller already has a .dylib path).
    HMODULE h = (HMODULE)dlopen(name, RTLD_LAZY | RTLD_GLOBAL);
    if (h) return h;
    // If the name ends in .dll, try the macOS equivalent (.dylib).
    const char *dot = strstr(lower, ".dll");
    if (dot) {
        char dylib_name[256];
        int base_len = (int)(dot - lower);
        strncpy(dylib_name, name, static_cast<size_t>(base_len));
        strcpy(dylib_name + base_len, ".dylib");
        h = (HMODULE)dlopen(dylib_name, RTLD_LAZY | RTLD_GLOBAL);
        if (h) return h;
    }
    return NULL;  // Caller can check for NULL and handle gracefully.
}
inline HMODULE LoadLibraryA(LPCSTR name) { return LoadLibrary(name); }
inline BOOL    FreeLibrary(HMODULE h) {
    if (!h || h == _WINCOMPAT_D3D8_HANDLE) return TRUE;
    return dlclose(h) == 0;
}
inline FARPROC GetProcAddress(HMODULE h, LPCSTR name) {
    if (!h || h == _WINCOMPAT_D3D8_HANDLE) return NULL;
    return (FARPROC)(intptr_t)dlsym(h, name);
}

// Heap stubs (use malloc)
inline HANDLE GetProcessHeap() { return (HANDLE)1; }
inline LPVOID HeapAlloc(HANDLE heap, DWORD flags, SIZE_T size) { return malloc(size); }
inline BOOL HeapFree(HANDLE heap, DWORD flags, LPVOID p) { free(p); return TRUE; }
inline LPVOID HeapReAlloc(HANDLE heap, DWORD flags, LPVOID p, SIZE_T size) { return realloc(p, size); }

// SYSTEMTIME structure
typedef struct _SYSTEMTIME {
    WORD wYear, wMonth, wDayOfWeek, wDay;
    WORD wHour, wMinute, wSecond, wMilliseconds;
} SYSTEMTIME, *LPSYSTEMTIME;
inline void GetSystemTime(LPSYSTEMTIME st) {
    if (st) memset(st, 0, sizeof(*st));
}
inline void GetLocalTime(LPSYSTEMTIME st) {
    if (st) memset(st, 0, sizeof(*st));
}

// OS version info
#define VER_PLATFORM_WIN32s         0
#define VER_PLATFORM_WIN32_WINDOWS  1
#define VER_PLATFORM_WIN32_NT       2

typedef struct _OSVERSIONINFO {
    DWORD dwOSVersionInfoSize;
    DWORD dwMajorVersion;
    DWORD dwMinorVersion;
    DWORD dwBuildNumber;
    DWORD dwPlatformId;
    char  szCSDVersion[128];
} OSVERSIONINFO, *POSVERSIONINFO;

inline BOOL GetVersionEx(OSVERSIONINFO* ovi) {
    if (!ovi) return FALSE;
    ovi->dwMajorVersion = 10; // Fake Windows 10
    ovi->dwMinorVersion = 0;
    ovi->dwBuildNumber  = 19041;
    ovi->dwPlatformId   = VER_PLATFORM_WIN32_NT;
    ovi->szCSDVersion[0] = 0;
    return TRUE;
}

// Memory status
typedef struct _MEMORYSTATUS {
    DWORD  dwLength;
    DWORD  dwMemoryLoad;
    SIZE_T dwTotalPhys;
    SIZE_T dwAvailPhys;
    SIZE_T dwTotalPageFile;
    SIZE_T dwAvailPageFile;
    SIZE_T dwTotalVirtual;
    SIZE_T dwAvailVirtual;
} MEMORYSTATUS, *LPMEMORYSTATUS;

#include <sys/sysctl.h>
inline void GlobalMemoryStatus(LPMEMORYSTATUS ms) {
    if (!ms) return;
    ms->dwLength = sizeof(MEMORYSTATUS);
    ms->dwMemoryLoad = 50;
    int64_t physmem = 0;
    size_t len = sizeof(physmem);
    if (sysctlbyname("hw.memsize", &physmem, &len, NULL, 0) == 0) {
        ms->dwTotalPhys = (SIZE_T)physmem;
        ms->dwAvailPhys = (SIZE_T)(physmem / 2);
    } else {
        ms->dwTotalPhys = 512 * 1024 * 1024;
        ms->dwAvailPhys = 256 * 1024 * 1024;
    }
    ms->dwTotalPageFile  = ms->dwTotalPhys * 2;
    ms->dwAvailPageFile  = ms->dwAvailPhys * 2;
    ms->dwTotalVirtual   = (SIZE_T)0x7FFFFFFF;
    ms->dwAvailVirtual   = (SIZE_T)0x7FFFFFFF / 2;
}

// Time zone info
typedef struct _TIME_ZONE_INFORMATION {
    LONG Bias;
    WCHAR StandardName[32];
    SYSTEMTIME StandardDate;
    LONG StandardBias;
    WCHAR DaylightName[32];
    SYSTEMTIME DaylightDate;
    LONG DaylightBias;
} TIME_ZONE_INFORMATION, *LPTIME_ZONE_INFORMATION;
#define TIME_ZONE_ID_UNKNOWN  0
#define TIME_ZONE_ID_STANDARD 1
#define TIME_ZONE_ID_DAYLIGHT 2
inline DWORD GetTimeZoneInformation(LPTIME_ZONE_INFORMATION tzi) {
    if (tzi) tzi->Bias = 0;
    return TIME_ZONE_ID_UNKNOWN;
}

// GetSystemInfo stub
typedef struct {
    DWORD dwNumberOfProcessors;
    DWORD dwPageSize;
} SYSTEM_INFO;
inline void GetSystemInfo(SYSTEM_INFO* si) {
    si->dwNumberOfProcessors = 1;
    si->dwPageSize = 4096;
}

// Exception stubs
#define EXCEPTION_EXECUTE_HANDLER 1
#define EXCEPTION_CONTINUE_SEARCH 0
#define EXCEPTION_CONTINUE_EXECUTION -1

// OpenFile flags (legacy)
#define OF_READ         0x00000000
#define OF_WRITE        0x00000001
#define OF_READWRITE    0x00000002
#define OF_CREATE       0x00001000
#define OF_DELETE       0x00000200

// GlobalAlloc/GlobalFree
#define GMEM_MOVEABLE 0x0002
#define GMEM_ZEROINIT 0x0040
inline HGLOBAL GlobalAlloc(UINT flags, SIZE_T size) { return malloc(size); }
inline HGLOBAL GlobalFree(HGLOBAL h) { free(h); return NULL; }
inline LPVOID GlobalLock(HGLOBAL h) { return h; }
inline BOOL GlobalUnlock(HGLOBAL h) { return TRUE; }
inline SIZE_T GlobalSize(HGLOBAL h) { return 0; }

// QueryPerformanceCounter / QueryPerformanceFrequency (using mach_absolute_time)
#include <mach/mach_time.h>
inline BOOL QueryPerformanceFrequency(LARGE_INTEGER* freq) {
    mach_timebase_info_data_t info;
    mach_timebase_info(&info);
    if (freq) freq->QuadPart = (int64_t)(1000000000ul * info.denom / info.numer);
    return TRUE;
}
inline BOOL QueryPerformanceCounter(LARGE_INTEGER* count) {
    if (count) count->QuadPart = (int64_t)mach_absolute_time();
    return TRUE;
}

// Process/thread priority constants and stubs
#define REALTIME_PRIORITY_CLASS         0x00000100
#define HIGH_PRIORITY_CLASS             0x00000080
#define ABOVE_NORMAL_PRIORITY_CLASS     0x00008000
#define NORMAL_PRIORITY_CLASS           0x00000020
#define IDLE_PRIORITY_CLASS             0x00000040
#define BELOW_NORMAL_PRIORITY_CLASS     0x00004000

#define THREAD_PRIORITY_TIME_CRITICAL   15
#define THREAD_PRIORITY_HIGHEST         2
#define THREAD_PRIORITY_ABOVE_NORMAL    1
#define THREAD_PRIORITY_NORMAL          0
#define THREAD_PRIORITY_BELOW_NORMAL    (-1)
#define THREAD_PRIORITY_LOWEST          (-2)
#define THREAD_PRIORITY_IDLE            (-15)

inline DWORD GetPriorityClass(HANDLE h) { return NORMAL_PRIORITY_CLASS; }
inline BOOL  SetPriorityClass(HANDLE h, DWORD cls) { return TRUE; }
inline int   GetThreadPriority(HANDLE h) { return THREAD_PRIORITY_NORMAL; }
inline BOOL  SetThreadPriority(HANDLE h, int priority) { return TRUE; }

// Process and startup structures
typedef struct _STARTUPINFOA {
    DWORD   cb;
    LPSTR   lpReserved;
    LPSTR   lpDesktop;
    LPSTR   lpTitle;
    DWORD   dwX, dwY, dwXSize, dwYSize;
    DWORD   dwXCountChars, dwYCountChars;
    DWORD   dwFillAttribute, dwFlags;
    WORD    wShowWindow;
    WORD    cbReserved2;
    LPBYTE  lpReserved2;
    HANDLE  hStdInput, hStdOutput, hStdError;
} STARTUPINFO, *LPSTARTUPINFO;

typedef struct _PROCESS_INFORMATION {
    HANDLE hProcess, hThread;
    DWORD  dwProcessId, dwThreadId;
} PROCESS_INFORMATION, *LPPROCESS_INFORMATION;

#define CREATE_NEW_CONSOLE     0x00000010
#define CREATE_NO_WINDOW       0x08000000

inline UINT GetWindowsDirectory(LPSTR buf, UINT size) {
    if (buf && size > 4) { strncpy(buf, "/tmp", size); return 4; }
    return 0;
}

inline UINT GetTempFileName(LPCSTR path, LPCSTR prefix, UINT uUnique, LPSTR buf) {
    if (buf) {
        snprintf(buf, MAX_PATH, "%s/%s%04X.tmp",
                 path ? path : "/tmp", prefix ? prefix : "tmp",
                 uUnique ? uUnique : (unsigned)1);
    }
    return uUnique ? uUnique : 1;
}

inline BOOL CreateProcess(LPCSTR lpApp, LPSTR lpCmd, void* lpProcAttr, void* lpThreadAttr,
    BOOL bInherit, DWORD flags, LPVOID lpEnv, LPCSTR lpDir,
    LPSTARTUPINFO lpSI, LPPROCESS_INFORMATION lpPI) {
    if (lpCmd) system(lpCmd);
    if (lpPI) memset(lpPI, 0, sizeof(*lpPI));
    return FALSE;
}

// Code page constants
#define CP_ACP      0  // ANSI code page
#define CP_UTF8     65001

// MultiByteToWideChar / WideCharToMultiByte stubs (simple ASCII-range conversion)
inline int MultiByteToWideChar(UINT cp, DWORD flags, const char* src, int src_len,
                                WCHAR* dst, int dst_size) {
    int len = (src_len == -1) ? (int)strlen(src) + 1 : src_len;
    if (dst_size == 0) return len;
    int count = 0;
    while (count < dst_size - 1 && count < len - 1) {
        dst[count] = (WCHAR)(unsigned char)src[count];
        count++;
    }
    if (count < dst_size) dst[count] = 0;
    return count + 1;
}
inline int WideCharToMultiByte(UINT cp, DWORD flags, const WCHAR* src, int src_len,
                                char* dst, int dst_size, const char* def, BOOL* used) {
    if (used) *used = FALSE;
    int len = 0;
    while ((src_len == -1 || len < src_len) && src[len]) len++;
    if (src_len == -1) len++; // include null
    if (dst_size == 0) return len;
    int count = 0;
    while (count < dst_size - 1 && count < len - 1) {
        dst[count] = (char)(src[count] & 0xFF);
        count++;
    }
    if (count < dst_size) dst[count] = 0;
    return count + 1;
}

// _vsnwprintf — native char16_t printf, no wchar_t dependency.
// Handles: %% %s %c %d %i %u %o %x %X %f %e %E %g %G %a %A %p
// with flags (- + space 0 #), width (literal or *), precision (literal or *),
// and length modifiers (h hh l ll I64 z t).
// %s/%S reads char16_t* (WCHAR); numeric specifiers dispatch to snprintf internally.
#include <stdio.h>
#include <string.h>
inline int _vsnwprintf(char16_t* buf, size_t count, const char16_t* fmt, va_list va) {
    if (!fmt || count == 0) return 0;
    size_t n = 0;
#define _C16PUT(ch) do { if (n + 1 < count) buf[n] = (ch); ++n; } while (0)
    for (const char16_t* p = fmt; *p; ) {
        if (*p != u'%') { _C16PUT(*p++); continue; }
        ++p;
        if (!*p) break;
        if (*p == u'%') { _C16PUT(u'%'); ++p; continue; }

        // Build equivalent narrow specifier for snprintf dispatch
        char ns[48]; int ni = 0;
        ns[ni++] = '%';

        // Flags
        bool ljust = false;
        while (*p==u'-'||*p==u'+'||*p==u' '||*p==u'0'||*p==u'#') {
            if (*p == u'-') ljust = true;
            ns[ni++] = (char)*p++;
        }

        // Width (literal or *)
        int width = 0;
        if (*p == u'*') {
            width = va_arg(va, int);
            if (width < 0) {
                if (!ljust) { memmove(ns+2, ns+1, (size_t)(ni-1)); ns[1] = '-'; ni++; }
                ljust = true; width = -width;
            }
            ni += snprintf(ns+ni, sizeof(ns) - static_cast<size_t>(ni), "%d", width);
            ++p;
        } else {
            while (*p >= u'0' && *p <= u'9') { ns[ni++] = (char)*p; width = width*10 + (*p - u'0'); ++p; }
        }

        // Precision
        int prec = -1;
        if (*p == u'.') {
            ns[ni++] = '.'; ++p;
            if (*p == u'*') {
                prec = va_arg(va, int);
                if (prec < 0) prec = 0;
                ni += snprintf(ns+ni, sizeof(ns) - static_cast<size_t>(ni), "%d", prec);
                ++p;
            } else {
                prec = 0;
                while (*p >= u'0' && *p <= u'9') { ns[ni++] = (char)*p; prec = prec*10 + (*p - u'0'); ++p; }
            }
        }

        // Length modifier (0=int, 1=h, 2=hh, 3=l, 4=ll, 5=z, 6=t)
        int lmod = 0;
        if (*p == u'h') {
            ns[ni++] = 'h'; ++p;
            if (*p == u'h') { ns[ni++] = 'h'; ++p; lmod = 2; } else lmod = 1;
        } else if (*p == u'l') {
            ns[ni++] = 'l'; ++p;
            if (*p == u'l') { ns[ni++] = 'l'; ++p; lmod = 4; } else lmod = 3;
        } else if (*p==u'I' && p[1]==u'6' && p[2]==u'4') {
            ns[ni++] = 'l'; ns[ni++] = 'l'; p += 3; lmod = 4;  // MSVC %I64 → %ll
        } else if (*p == u'z') { ns[ni++] = 'z'; ++p; lmod = 5; }
          else if (*p == u't') { ns[ni++] = 't'; ++p; lmod = 6; }

        char conv = *p ? (char)*p++ : 0;
        ns[ni++] = conv; ns[ni] = 0;

        if (conv == 's' || conv == 'S') {
            // char16_t* string — handled natively, no wchar_t needed
            const char16_t* s = va_arg(va, const char16_t*);
            if (!s) s = u"(null)";
            size_t slen = 0;
            while (s[slen] && (prec < 0 || (int)slen < prec)) ++slen;
            if (!ljust) for (int k = (int)slen; k < width; ++k) _C16PUT(u' ');
            for (size_t k = 0; k < slen; ++k) _C16PUT(s[k]);
            if ( ljust) for (int k = (int)slen; k < width; ++k) _C16PUT(u' ');
        } else if (conv == 'c') {
            _C16PUT((char16_t)va_arg(va, int));
        } else {
            // Numeric / pointer — format via snprintf, expand ASCII result to char16_t
            char tmp[128]; int r = 0;
            if (conv=='f'||conv=='e'||conv=='E'||conv=='g'||conv=='G'||conv=='a'||conv=='A')
                r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, double));
            else if (conv == 'p')
                r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, void*));
            else if (lmod == 4) r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, long long));
            else if (lmod == 3) r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, long));
            else if (lmod == 5) r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, size_t));
            else if (lmod == 6) r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, ptrdiff_t));
            else                r = snprintf(tmp, sizeof(tmp), ns, va_arg(va, int));
            for (int k = 0; k < r; ++k) _C16PUT((char16_t)(unsigned char)tmp[k]);
        }
    }
    buf[n < count ? n : count - 1] = 0;
    return (int)n;
#undef _C16PUT
}

// Wide string to integer conversion — char16_t* (simple ASCII digit parsing)
inline int _wtoi(const char16_t* s) {
    if (!s) return 0;
    int sign = 1, result = 0, i = 0;
    while (s[i] == u' ') i++;
    if (s[i] == u'-') { sign = -1; i++; } else if (s[i] == u'+') i++;
    while (s[i] >= u'0' && s[i] <= u'9') { result = result * 10 + (s[i] - u'0'); i++; }
    return sign * result;
}
inline long _wtol(const char16_t* s) { return (long)_wtoi(s); }
inline long long _wtoi64(const char16_t* s) {
    if (!s) return 0;
    long long sign = 1, result = 0; int i = 0;
    while (s[i] == u' ') i++;
    if (s[i] == u'-') { sign = -1; i++; } else if (s[i] == u'+') i++;
    while (s[i] >= u'0' && s[i] <= u'9') { result = result * 10 + (s[i] - u'0'); i++; }
    return sign * result;
}

// Wide string sprintf wrappers — char16_t*
inline int _swprintf(char16_t* buf, const char16_t* fmt, ...) {
    va_list va; va_start(va, fmt); int r = _vsnwprintf(buf, 4096, fmt, va); va_end(va); return r;
}
inline int _swprintf(char16_t* buf, size_t count, const char16_t* fmt, ...) {
    va_list va; va_start(va, fmt); int r = _vsnwprintf(buf, count, fmt, va); va_end(va); return r;
}
inline int _snwprintf(char16_t* buf, size_t count, const char16_t* fmt, ...) {
    va_list va; va_start(va, fmt); int r = _vsnwprintf(buf, count, fmt, va); va_end(va); return r;
}
// Windows swprintf uses 2 args (buf, fmt, ...) unlike POSIX 3 args (buf, n, fmt, ...).
// Map to our _swprintf wrapper.
#ifndef swprintf
#define swprintf _swprintf
#endif

// swscanf for char16_t* — converts to narrow string, then calls vsscanf
// Only supports simple ASCII-compatible format strings like "%f", "%d"
inline int swscanf(const char16_t* str, const char16_t* fmt, ...) {
    char narrow_str[512]; int i;
    for (i = 0; str[i] && i < 511; i++) narrow_str[i] = (char)str[i];
    narrow_str[i] = 0;
    char narrow_fmt[64];
    for (i = 0; fmt[i] && i < 63; i++) narrow_fmt[i] = (char)fmt[i];
    narrow_fmt[i] = 0;
    va_list ap; va_start(ap, fmt);
    int r = vsscanf(narrow_str, narrow_fmt, ap);
    va_end(ap); return r;
}

// DebugBreak
inline void DebugBreak() { __builtin_debugtrap(); }

// Resource access — implemented in compat/winres.cpp via chat.res parsing
typedef HANDLE HRSRC;
HRSRC   FindResource(HMODULE hmod, LPCSTR name, LPCSTR type);
HRSRC   FindResourceA(HMODULE hmod, LPCSTR name, LPCSTR type);
HGLOBAL LoadResource(HMODULE hmod, HRSRC res);
LPVOID  LockResource(HGLOBAL res);
DWORD   SizeofResource(HMODULE hmod, HRSRC res);

// Additional registry functions
inline LONG RegDeleteValue(HKEY key, LPCSTR name) { return ERROR_SUCCESS; }
inline LONG RegDeleteValueA(HKEY key, LPCSTR name) { return ERROR_SUCCESS; }
inline LONG RegQueryValueExW(HKEY key, const WCHAR* name, LPDWORD reserved, LPDWORD type, LPBYTE data, LPDWORD size) {
    return ERROR_FILE_NOT_FOUND;
}
inline LONG RegSetValueExW(HKEY key, const WCHAR* name, DWORD reserved, DWORD type, const BYTE* data, DWORD size) {
    return ERROR_SUCCESS;
}
inline LONG RegQueryInfoKey(HKEY key, LPSTR cls, LPDWORD cls_len, LPDWORD reserved,
    LPDWORD sub_keys, LPDWORD max_sub_key_len, LPDWORD max_cls_len,
    LPDWORD values, LPDWORD max_value_name_len, LPDWORD max_value_len,
    LPDWORD security_desc, LPFILETIME last_write) {
    if (sub_keys)  *sub_keys  = 0;
    if (values)    *values    = 0;
    return ERROR_SUCCESS;
}

// Windows lstr* string functions - map to POSIX equivalents
// Note: lstrlen must be an inline function (not a macro) so ::lstrlen() works
#include <string.h>
inline int lstrlen(LPCSTR s) { return s ? (int)strlen(s) : 0; }
inline int lstrlenA(LPCSTR s) { return lstrlen(s); }
#ifndef lstrcpy
#define lstrcpy(d,s)        strcpy((d),(s))
#define lstrcat(d,s)        strcat((d),(s))
#define lstrcmpi(a,b)       strcasecmp((a),(b))
#define lstrcpyn(d,s,n)     strncpy((d),(s),(size_t)(n))
#define lstrcmp(a,b)        strcmp((a),(b))
#endif

// _strdup is POSIX strdup
#ifndef _strdup
#define _strdup strdup
#endif

// Wait constants
#define WAIT_OBJECT_0       0x00000000L
#define WAIT_TIMEOUT        0x00000102L
#define WAIT_FAILED         0xFFFFFFFFL
#define WAIT_ABANDONED      0x00000080L

// Memory utility macros (Windows style)
#define ZeroMemory(dest, len)       memset((dest), 0, (len))
#define FillMemory(dest, len, val)  memset((dest), (val), (len))
#define CopyMemory(dest, src, len)  memcpy((dest), (src), (len))
#define MoveMemory(dest, src, len)  memmove((dest), (src), (len))

// Structured Exception Handling stubs (Windows SEH — no-ops on macOS)
#ifndef _EXCEPTION_RECORD_DEFINED
#define _EXCEPTION_RECORD_DEFINED
struct _EXCEPTION_RECORD {
    DWORD ExceptionCode;
    DWORD ExceptionFlags;
    struct _EXCEPTION_RECORD *ExceptionRecord;
    void *ExceptionAddress;
    DWORD NumberParameters;
    uint32_t* ExceptionInformation[15];
};
typedef struct _EXCEPTION_RECORD EXCEPTION_RECORD;
#endif

#ifndef _EXCEPTION_POINTERS_DEFINED
#define _EXCEPTION_POINTERS_DEFINED
struct _CONTEXT { DWORD ContextFlags; };
typedef struct _CONTEXT CONTEXT;
struct _EXCEPTION_POINTERS {
    EXCEPTION_RECORD *ExceptionRecord;
    CONTEXT *ContextRecord;
};
typedef struct _EXCEPTION_POINTERS EXCEPTION_POINTERS;
#endif

// Exception_Handler stub — declared in wwlib/Except.h, no-op on non-Windows
#ifndef EXCEPTION_HANDLER_DECLARED
#define EXCEPTION_HANDLER_DECLARED
inline int Exception_Handler(int, EXCEPTION_POINTERS*) { return 0; }
#endif

// SEH keywords — map to no-ops on macOS
#ifndef __except
#define __try       if(true)
#define __except(x) if(false)
#define __finally
#define GetExceptionCode()         0
#define GetExceptionInformation()  ((EXCEPTION_POINTERS*)NULL)
#endif

// Console API stubs
typedef struct _COORD { short X; short Y; } COORD;
typedef struct _SMALL_RECT { short Left, Top, Right, Bottom; } SMALL_RECT;
typedef struct _CONSOLE_SCREEN_BUFFER_INFO {
    COORD      dwSize;
    COORD      dwCursorPosition;
    WORD       wAttributes;
    SMALL_RECT srWindow;
    COORD      dwMaximumWindowSize;
} CONSOLE_SCREEN_BUFFER_INFO;
#define STD_INPUT_HANDLE  ((DWORD)-10)
#define STD_OUTPUT_HANDLE ((DWORD)-11)
#define STD_ERROR_HANDLE  ((DWORD)-12)
#define FOREGROUND_BLUE      0x0001
#define FOREGROUND_GREEN     0x0002
#define FOREGROUND_RED       0x0004
#define FOREGROUND_INTENSITY 0x0008
#define BACKGROUND_BLUE      0x0010
#define BACKGROUND_GREEN     0x0020
#define BACKGROUND_RED       0x0040
#define BACKGROUND_INTENSITY 0x0080
inline BOOL AllocConsole(void) { return TRUE; }
inline BOOL FreeConsole(void)  { return TRUE; }
inline HANDLE GetStdHandle(DWORD nStdHandle) { return (HANDLE)(intptr_t)nStdHandle; }
inline BOOL SetConsoleTextAttribute(HANDLE h, WORD attr) { return TRUE; }
inline BOOL SetConsoleScreenBufferSize(HANDLE h, COORD size) { return TRUE; }
inline BOOL GetConsoleScreenBufferInfo(HANDLE h, CONSOLE_SCREEN_BUFFER_INFO* info) {
    if (info) { memset(info, 0, sizeof(*info)); } return TRUE;
}
inline BOOL SetConsoleCursorPosition(HANDLE h, COORD pos) { return TRUE; }
inline BOOL SetConsoleTitle(const char* title) { return TRUE; }
inline BOOL WriteConsole(HANDLE h, const void* buf, DWORD nch, DWORD* written, void*) {
    if (written) *written = nch; return TRUE;
}
inline BOOL ReadConsole(HANDLE h, void* buf, DWORD nch, DWORD* read, void*) {
    if (read) *read = 0; return TRUE;
}
inline BOOL FillConsoleOutputAttribute(HANDLE h, WORD attr, DWORD len, COORD coord, void* written) {
    return TRUE;
}
inline BOOL FillConsoleOutputCharacter(HANDLE h, char ch, DWORD len, COORD coord, void* written) {
    return TRUE;
}
inline int CompareFileTime(const FILETIME* a, const FILETIME* b) {
    if (a->dwHighDateTime != b->dwHighDateTime) return (a->dwHighDateTime > b->dwHighDateTime) ? 1 : -1;
    if (a->dwLowDateTime  != b->dwLowDateTime)  return (a->dwLowDateTime  > b->dwLowDateTime)  ? 1 : -1;
    return 0;
}
inline BOOL SetForegroundWindow(HWND hwnd) { return TRUE; }
inline BOOL SystemTimeToFileTime(LPSYSTEMTIME st, LPFILETIME ft) { return TRUE; }
inline BOOL FileTimeToSystemTime(LPFILETIME ft, LPSYSTEMTIME st) {
    if (st) memset(st, 0, sizeof(SYSTEMTIME)); return TRUE;
}
inline BOOL FileTimeToLocalFileTime(LPFILETIME lpFileTime, LPFILETIME lpLocalFileTime) {
    if (lpLocalFileTime && lpFileTime) *lpLocalFileTime = *lpFileTime; return TRUE;
}
#define TIME_FORCE24HOURFORMAT 0x00000008
#define LOCALE_STIME  0x0000001E
#define LOCALE_STIMEFORMAT 0x00001003
#define LOCALE_SSHORTDATE 0x0000001F
inline int GetTimeFormat(DWORD locale, DWORD flags, LPSYSTEMTIME time, const char* fmt, char* buf, int size) {
    if (buf && size > 0) buf[0] = 0; return 0;
}
inline int GetDateFormat(DWORD locale, DWORD flags, LPSYSTEMTIME date, const char* fmt, char* buf, int size) {
    if (buf && size > 0) buf[0] = 0; return 0;
}

// Version info (winver.h / verrsrc.h)
typedef struct tagVS_FIXEDFILEINFO {
    DWORD dwSignature;
    DWORD dwStrucVersion;
    DWORD dwFileVersionMS;
    DWORD dwFileVersionLS;
    DWORD dwProductVersionMS;
    DWORD dwProductVersionLS;
    DWORD dwFileFlagsMask;
    DWORD dwFileFlags;
    DWORD dwFileOS;
    DWORD dwFileType;
    DWORD dwFileSubtype;
    DWORD dwFileDateMS;
    DWORD dwFileDateLS;
} VS_FIXEDFILEINFO;
inline DWORD GetFileVersionInfoSize(LPCSTR filename, LPDWORD handle) { if(handle) *handle=0; return 0; }
inline BOOL  GetFileVersionInfo(LPCSTR filename, DWORD handle, DWORD size, LPVOID data) { return FALSE; }
inline BOOL  VerQueryValue(const LPVOID block, LPCSTR sub, LPVOID* buf, UINT* len) { return FALSE; }

// Process exit code
#define STILL_ACTIVE 259
inline BOOL GetExitCodeProcess(HANDLE hProcess, LPDWORD lpExitCode) {
    if (lpExitCode) *lpExitCode = 0; return TRUE;
}
// Overload for code that uses unsigned long* instead of DWORD*
inline BOOL GetExitCodeProcess(HANDLE hProcess, unsigned long* lpExitCode) {
    if (lpExitCode) *lpExitCode = 0; return TRUE;
}

// Path search — do a real stat-based search so "always.dat" check in WINMAIN.CPP passes
inline DWORD SearchPath(LPCSTR lpPath, LPCSTR lpFileName, LPCSTR lpExtension,
    DWORD nBufferLength, LPSTR lpBuffer, LPSTR* lpFilePart) {
    if (!lpFileName) return 0;
    // Build candidate path: lpPath/lpFileName[lpExtension]
    char candidate[1024];
    if (lpPath && lpPath[0]) {
        snprintf(candidate, sizeof(candidate), "%s/%s%s", lpPath, lpFileName,
                 lpExtension ? lpExtension : "");
    } else {
        snprintf(candidate, sizeof(candidate), "%s%s", lpFileName,
                 lpExtension ? lpExtension : "");
    }
    struct stat st;
    if (stat(candidate, &st) == 0) {
        DWORD len = (DWORD)strlen(candidate);
        if (lpBuffer && nBufferLength > len) {
            strcpy(lpBuffer, candidate);
            if (lpFilePart) {
                char* slash = strrchr(lpBuffer, '/');
                *lpFilePart = slash ? slash + 1 : lpBuffer;
            }
        }
        return len;
    }
    // File not found — return 0
    if (lpBuffer && nBufferLength > 0) lpBuffer[0] = 0;
    return 0;
}

// Top-level exception filter
typedef LONG (*LPTOP_LEVEL_EXCEPTION_FILTER)(void* ExceptionInfo);
inline LPTOP_LEVEL_EXCEPTION_FILTER SetUnhandledExceptionFilter(LPTOP_LEVEL_EXCEPTION_FILTER f) {
    return NULL;
}

// Error reporting
inline void Print_Win32Error(const char* msg, DWORD err = 0) {
    if (msg) fprintf(stderr, "Win32Error: %s (err=%u)\n", msg, (unsigned)err);
}

// WaitForInputIdle - stub, immediately return
inline DWORD WaitForInputIdle(HANDLE hProcess, DWORD dwMilliseconds) { return 0; }

// Application exception/version callbacks for wwdebug
// Use templates to accept any function pointer type (C++ doesn't allow implicit fn ptr -> void*)
template<typename F>
inline void Register_Application_Exception_Callback(F cb) {}
template<typename F>
inline void Register_Application_Version_Callback(F cb) {}

// _alloca (stack allocation)
#include <alloca.h>
#ifndef _alloca
#define _alloca alloca
#endif

// Locale constants (not already defined above)
#define LOCALE_SYSTEM_DEFAULT  0x0800

// Process timing stubs - return success with zeroed times
inline BOOL GetProcessTimes(HANDLE hProcess,
    LPFILETIME lpCreationTime, LPFILETIME lpExitTime,
    LPFILETIME lpKernelTime, LPFILETIME lpUserTime) {
    if (lpCreationTime) { lpCreationTime->dwLowDateTime = 0; lpCreationTime->dwHighDateTime = 0; }
    if (lpExitTime)     { lpExitTime->dwLowDateTime = 0; lpExitTime->dwHighDateTime = 0; }
    if (lpKernelTime)   { lpKernelTime->dwLowDateTime = 0; lpKernelTime->dwHighDateTime = 0; }
    if (lpUserTime)     { lpUserTime->dwLowDateTime = 0; lpUserTime->dwHighDateTime = 0; }
    return TRUE;
}

// Drive type constants
#define DRIVE_UNKNOWN       0
#define DRIVE_NO_ROOT_DIR   1
#define DRIVE_REMOVABLE     2
#define DRIVE_FIXED         3
#define DRIVE_REMOTE        4
#define DRIVE_CDROM         5
#define DRIVE_RAMDISK       6

// CD/Drive detection stubs - no CDs on macOS
inline DWORD GetLogicalDriveStrings(DWORD nBufferLength, LPSTR lpBuffer) {
    if (lpBuffer && nBufferLength > 0) *lpBuffer = 0;
    return 0;
}
inline UINT GetDriveType(LPCSTR lpRootPathName) { return DRIVE_UNKNOWN; }
inline BOOL GetVolumeInformation(LPCSTR lpRootPathName,
    LPSTR lpVolumeNameBuffer, DWORD nVolumeNameSize,
    LPDWORD lpVolumeSerialNumber, LPDWORD lpMaximumComponentLength,
    LPDWORD lpFileSystemFlags, LPSTR lpFileSystemNameBuffer,
    DWORD nFileSystemNameSize) {
    if (lpVolumeNameBuffer && nVolumeNameSize > 0) *lpVolumeNameBuffer = 0;
    return FALSE;
}

// Process constants
#define PROCESS_TERMINATE         0x0001
#define PROCESS_CREATE_THREAD     0x0002
#define PROCESS_SET_INFORMATION   0x0200
#define PROCESS_QUERY_INFORMATION 0x0400
#define PROCESS_ALL_ACCESS        0x1F0FFF
#define STILL_ACTIVE              259

// Mutex constants
#define MUTEX_ALL_ACCESS          0x1F0001
#define MUTEX_MODIFY_STATE        0x0001

// Process functions stubs (new ones not elsewhere defined)
inline DWORD GetProcessVersion(DWORD dwProcessId) { (void)dwProcessId; return 0; }
inline BOOL TerminateProcess(HANDLE hProcess, UINT uExitCode) { (void)hProcess; (void)uExitCode; return FALSE; }
inline HANDLE OpenProcess(DWORD dwDesiredAccess, BOOL bInheritHandle, DWORD dwProcessId) {
    (void)dwDesiredAccess; (void)bInheritHandle; (void)dwProcessId; return nullptr;
}

// User name
#include <unistd.h>
#ifndef UNLEN
#define UNLEN 256
#endif
inline BOOL GetUserName(LPSTR lpBuffer, LPDWORD lpnSize) {
    if (!lpBuffer || !lpnSize || *lpnSize == 0) return FALSE;
#ifdef __APPLE__
    const char* name = getenv("USER");
    if (!name) name = "user";
#else
    const char* name = getenv("USER");
    if (!name) name = getenv("LOGNAME");
    if (!name) name = "user";
#endif
    size_t len = strlen(name);
    if (len >= (size_t)*lpnSize) len = (size_t)*lpnSize - 1;
    memcpy(lpBuffer, name, len);
    lpBuffer[len] = 0;
    *lpnSize = (DWORD)len;
    return TRUE;
}

// Error codes
#define ERROR_ALREADY_EXISTS    183
#define ERROR_FILE_NOT_FOUND    2
#define ERROR_PATH_NOT_FOUND    3
#define ERROR_ACCESS_DENIED     5
#define ERROR_SHARING_VIOLATION 32
#define ERROR_FILE_EXISTS       80

// Computer name
#define MAX_COMPUTERNAME_LENGTH 15
inline BOOL GetComputerName(LPSTR lpBuffer, LPDWORD lpnSize) {
    if (!lpBuffer || !lpnSize || *lpnSize == 0) return FALSE;
    const char* name = "localhost";
    size_t len = strlen(name);
    if (len >= (size_t)*lpnSize) len = (size_t)*lpnSize - 1;
    memcpy(lpBuffer, name, len);
    lpBuffer[len] = 0;
    *lpnSize = (DWORD)len;
    return TRUE;
}

// Resource type/name constants and misc
typedef WCHAR* LPWSTR;

#define RT_STRING   ((LPCSTR)6)
#define RT_RCDATA   ((LPCSTR)10)
#define RT_DIALOG   ((LPCSTR)5)
#define RT_MENU     ((LPCSTR)4)
#define RT_BITMAP   ((LPCSTR)2)
#define RT_ICON     ((LPCSTR)3)
#define RT_CURSOR   ((LPCSTR)1)
#define RT_VERSION  ((LPCSTR)16)
#define RT_MANIFEST ((LPCSTR)24)

#define LANG_NEUTRAL     0x00
#define SUBLANG_NEUTRAL  0x00
#define SUBLANG_DEFAULT  0x01
#define MAKELANGID(p,s) ((((WORD)(s)) << 10) | (WORD)(p))

inline HRSRC FindResourceEx(HMODULE hModule, LPCSTR lpType, LPCSTR lpName, WORD wLanguage) {
    (void)hModule; (void)lpType; (void)lpName; (void)wLanguage; return nullptr;
}
inline BOOL  FreeResource(HGLOBAL hResData) { (void)hResData; return TRUE; }
