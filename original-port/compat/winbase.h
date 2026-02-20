// winbase.h compat shim for macOS/clang
#pragma once
#ifndef WINBASE_H_COMPAT
#define WINBASE_H_COMPAT

#include "windef.h"
#include "winnt.h"
#include <pthread.h>
#include <unistd.h>
#include <sys/time.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <sys/stat.h>
#include <fcntl.h>

// Sleep
inline void Sleep(DWORD milliseconds) {
    usleep(milliseconds * 1000);
}

// GetTickCount (milliseconds since some epoch)
inline DWORD GetTickCount() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (DWORD)(tv.tv_sec * 1000 + tv.tv_usec / 1000);
}

// GetLastError / SetLastError
inline DWORD GetLastError() { return (DWORD)errno; }
inline void SetLastError(DWORD e) { errno = (int)e; }

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
        strncpy(buf, strerror(msgid), size - 1);
        buf[size - 1] = 0;
    }
    return buf ? (DWORD)strlen(buf) : 0;
}

// ExitProcess
inline void ExitProcess(UINT code) {
    exit((int)code);
}

// GetModuleHandle stub
inline HMODULE GetModuleHandle(LPCSTR name) { return NULL; }
inline HMODULE GetModuleHandleA(LPCSTR name) { return NULL; }
inline BOOL GetModuleFileName(HMODULE mod, LPSTR buf, DWORD size) {
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

inline BOOL CloseHandle(HANDLE h) {
    if (h == INVALID_HANDLE_VALUE || h == NULL) return FALSE;
    return close((int)(intptr_t)h) == 0;
}

inline BOOL ReadFile(HANDLE h, LPVOID buf, DWORD to_read, LPDWORD read_out, LPVOID overlapped) {
    ssize_t r = read((int)(intptr_t)h, buf, to_read);
    if (r < 0) { if (read_out) *read_out = 0; return FALSE; }
    if (read_out) *read_out = (DWORD)r;
    return TRUE;
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
    if (fstat((int)(intptr_t)h, &st) \!= 0) return 0xFFFFFFFF;
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
    if (stat(path, &st) \!= 0) return 0xFFFFFFFF;
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
    if (pthread_create(t, NULL, _thread_wrapper, ta) \!= 0) {
        free(t); free(ta); return NULL;
    }
    if (id) *id = (DWORD)(uintptr_t)*t;
    return (HANDLE)t;
}

inline DWORD WaitForSingleObject(HANDLE h, DWORD timeout) {
    if (\!h) return WAIT_FAILED;
    pthread_join(*(pthread_t*)h, NULL);
    return WAIT_OBJECT_0;
}

inline BOOL TerminateThread(HANDLE h, DWORD code) { return FALSE; }

inline DWORD GetCurrentThreadId() {
    return (DWORD)(uintptr_t)pthread_self();
}

inline HANDLE GetCurrentThread() { return (HANDLE)(uintptr_t)pthread_self(); }
inline HANDLE GetCurrentProcess() { return (HANDLE)(uintptr_t)getpid(); }
inline DWORD GetCurrentProcessId() { return (DWORD)getpid(); }

// Event stubs
inline HANDLE CreateEvent(LPSECURITY_ATTRIBUTES sec, BOOL manual, BOOL initial, LPCSTR name) { return NULL; }
inline HANDLE CreateEventA(LPSECURITY_ATTRIBUTES sec, BOOL manual, BOOL initial, LPCSTR name) { return NULL; }
inline HANDLE OpenEvent(DWORD access, BOOL inherit, LPCSTR name) { return NULL; }
#define EVENT_MODIFY_STATE 0x0002
inline BOOL SetEvent(HANDLE h) { return FALSE; }
inline BOOL ResetEvent(HANDLE h) { return FALSE; }
inline BOOL PulseEvent(HANDLE h) { return FALSE; }

// Mutex stubs
inline HANDLE CreateMutex(LPSECURITY_ATTRIBUTES sec, BOOL initial, LPCSTR name) { return NULL; }
inline HANDLE OpenMutex(DWORD access, BOOL inherit, LPCSTR name) { return NULL; }
inline BOOL ReleaseMutex(HANDLE h) { return FALSE; }

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
                         LPSTR cls, LPDWORD cls_len, LPVOID ft) { return ERROR_NO_MORE_ITEMS; }
inline LONG RegEnumValue(HKEY key, DWORD idx, LPSTR name, LPDWORD name_len, LPDWORD reserved,
                         LPDWORD type, LPBYTE data, LPDWORD size) { return ERROR_NO_MORE_ITEMS; }

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
    DWORD dwFileAttributes;
    char  cFileName[260];
    char  cAlternateFileName[14];
    DWORD nFileSizeLow;
    DWORD nFileSizeHigh;
} WIN32_FIND_DATA, *LPWIN32_FIND_DATA;
typedef WIN32_FIND_DATA WIN32_FIND_DATAA;
typedef LPWIN32_FIND_DATA LPWIN32_FIND_DATAA;

inline HANDLE FindFirstFile(LPCSTR pattern, LPWIN32_FIND_DATA fd) { return INVALID_HANDLE_VALUE; }
inline HANDLE FindFirstFileA(LPCSTR pattern, LPWIN32_FIND_DATA fd) { return INVALID_HANDLE_VALUE; }
inline BOOL FindNextFile(HANDLE h, LPWIN32_FIND_DATA fd) { return FALSE; }
inline BOOL FindNextFileA(HANDLE h, LPWIN32_FIND_DATA fd) { return FALSE; }
inline BOOL FindClose(HANDLE h) { return FALSE; }

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

// LoadLibrary stubs
inline HMODULE LoadLibrary(LPCSTR name) { return NULL; }
inline HMODULE LoadLibraryA(LPCSTR name) { return NULL; }
inline BOOL FreeLibrary(HMODULE h) { return FALSE; }
inline LPVOID GetProcAddress(HMODULE h, LPCSTR name) { return NULL; }

// Heap stubs (use malloc)
inline HANDLE GetProcessHeap() { return (HANDLE)1; }
inline LPVOID HeapAlloc(HANDLE heap, DWORD flags, SIZE_T size) { return malloc(size); }
inline BOOL HeapFree(HANDLE heap, DWORD flags, LPVOID p) { free(p); return TRUE; }
inline LPVOID HeapReAlloc(HANDLE heap, DWORD flags, LPVOID p, SIZE_T size) { return realloc(p, size); }

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

// GlobalAlloc/GlobalFree
#define GMEM_MOVEABLE 0x0002
#define GMEM_ZEROINIT 0x0040
inline HGLOBAL GlobalAlloc(UINT flags, SIZE_T size) { return malloc(size); }
inline HGLOBAL GlobalFree(HGLOBAL h) { free(h); return NULL; }
inline LPVOID GlobalLock(HGLOBAL h) { return h; }
inline BOOL GlobalUnlock(HGLOBAL h) { return TRUE; }
inline SIZE_T GlobalSize(HGLOBAL h) { return 0; }

#endif // WINBASE_H_COMPAT
