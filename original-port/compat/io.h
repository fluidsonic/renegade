// io.h compat shim for macOS
#pragma once
#ifndef IO_H_COMPAT
#define IO_H_COMPAT

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>

// _access maps to POSIX access()
inline int _access(const char* path, int mode) { return access(path, mode); }

// _open/_close/_read/_write map to POSIX
inline int _open(const char* path, int flags, ...) { return open(path, flags, 0644); }
inline int _close(int fd) { return close(fd); }
inline int _read(int fd, void* buf, unsigned int count) { return (int)read(fd, buf, count); }
inline int _write(int fd, const void* buf, unsigned int count) { return (int)write(fd, buf, count); }
inline long _lseek(int fd, long offset, int origin) { return (long)lseek(fd, offset, origin); }
inline long _tell(int fd) { return (long)lseek(fd, 0, SEEK_CUR); }
inline int  _eof(int fd) {
    off_t cur = lseek(fd, 0, SEEK_CUR);
    off_t end = lseek(fd, 0, SEEK_END);
    lseek(fd, cur, SEEK_SET);
    return cur == end;
}

// File mode flags (subset)
#define _O_RDONLY   O_RDONLY
#define _O_WRONLY   O_WRONLY
#define _O_RDWR     O_RDWR
#define _O_CREAT    O_CREAT
#define _O_TRUNC    O_TRUNC
#define _O_APPEND   O_APPEND
#define _O_BINARY   0  // no-op on POSIX
#define _O_TEXT     0  // no-op on POSIX

// _stat maps to stat
#define _stat       stat
#define _stati64    stat
inline int _fstat(int fd, struct stat* st) { return fstat(fd, st); }

// Access mode flags
#define _A_NORMAL   0x00
#define _A_RDONLY   0x01
#define _A_HIDDEN   0x02
#define _A_SUBDIR   0x10
#define _A_ARCH     0x20

// Find file stubs (minimal)
struct _finddata_t {
    unsigned attrib;
    time_t   time_create;
    time_t   time_access;
    time_t   time_write;
    long     size;
    char     name[260];
};

// These need full dirent implementation; stub for now
inline long _findfirst(const char* filespec, struct _finddata_t* fileinfo) { return -1L; }
inline int  _findnext(long handle, struct _finddata_t* fileinfo) { return -1; }
inline int  _findclose(long handle) { return 0; }

#endif // IO_H_COMPAT
