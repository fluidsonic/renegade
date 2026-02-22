#pragma once

#include "global.h"

// winsock.h compat shim for macOS - maps to BSD sockets

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>

typedef int             SOCKET;
typedef struct sockaddr SOCKADDR;
typedef struct sockaddr* LPSOCKADDR;
typedef struct sockaddr_in SOCKADDR_IN;
typedef struct sockaddr_in* LPSOCKADDR_IN;
typedef struct in_addr  IN_ADDR;

#define INVALID_SOCKET  ((SOCKET)(-1))
#define SOCKET_ERROR    (-1)

// Error codes
#define WSAEWOULDBLOCK  EWOULDBLOCK
#define WSAEINPROGRESS  EINPROGRESS
#define WSAECONNREFUSED ECONNREFUSED
#define WSAETIMEDOUT    ETIMEDOUT
#define WSAENETUNREACH  ENETUNREACH
#define WSAEADDRINUSE   EADDRINUSE
#define WSAENOTCONN     ENOTCONN

// WSA init stubs
typedef struct { WORD wVersion, wHighVersion; char szDescription[257]; char szSystemStatus[129]; } WSADATA;
inline int  WSAStartup(WORD ver, WSADATA* data) { if (data) { data->wVersion=ver; data->wHighVersion=ver; } return 0; }
inline int  WSACleanup() { return 0; }
inline int  WSAGetLastError() { return errno; }
inline void WSASetLastError(int err) { errno = err; }

// Socket operations mapping
inline int  closesocket(SOCKET s) { return close(s); }

#include <sys/ioctl.h>
// ioctlsocket: handle FIONBIO (non-blocking) and FIONREAD properly
inline int ioctlsocket(SOCKET s, long cmd, u_long* argp) {
    if (cmd == FIONBIO) {
        int flags = fcntl(s, F_GETFL, 0);
        if (*argp) flags |= O_NONBLOCK; else flags &= ~O_NONBLOCK;
        return fcntl(s, F_SETFL, flags);
    }
    return ioctl(s, (unsigned long)cmd, argp);
}

// Socket options
#define SO_REUSEADDR    SO_REUSEADDR
#define IPPROTO_TCP     IPPROTO_TCP
#define TCP_NODELAY     TCP_NODELAY

// HOSTENT
typedef struct hostent  HOSTENT;
typedef struct hostent* LPHOSTENT;

// Address helpers
#define INADDR_ANY       0x00000000
#define INADDR_BROADCAST 0xffffffff
#define INADDR_NONE      0xffffffff

// inet_addr is available from netinet/in.h

// htons/ntohs/htonl/ntohl: On macOS these are macros, which breaks ::ntohs() syntax.
// Convert them to inline functions so qualified calls work.
#ifdef ntohs
static inline unsigned short _compat_ntohs(unsigned short v) { return ntohs(v); }
#undef ntohs
#endif
#ifdef htons
static inline unsigned short _compat_htons(unsigned short v) { return htons(v); }
#undef htons
#endif
#ifdef ntohl
static inline unsigned int _compat_ntohl(unsigned int v) { return ntohl(v); }
#undef ntohl
#endif
#ifdef htonl
static inline unsigned int _compat_htonl(unsigned int v) { return htonl(v); }
#undef htonl
#endif
inline unsigned short ntohs(unsigned short v) { return _compat_ntohs(v); }
inline unsigned short htons(unsigned short v) { return _compat_htons(v); }
inline unsigned int   ntohl(unsigned int v)   { return _compat_ntohl(v); }
inline unsigned int   htonl(unsigned int v)   { return _compat_htonl(v); }

// TIMEVAL is struct timeval from sys/time.h

// fd_set operations are in sys/select.h
#include <sys/select.h>

// All WSAE* error codes mapped to POSIX errno equivalents
#ifndef WSAEINTR
// POSIX equivalents
#define WSAEINTR            EINTR
#define WSAEBADF            EBADF
#define WSAEACCES           EACCES
#define WSAEFAULT           EFAULT
#define WSAEINVAL           EINVAL
#define WSAEMFILE           EMFILE
#define WSAEALREADY         EALREADY
#define WSAENOTSOCK         ENOTSOCK
#define WSAEDESTADDRREQ     EDESTADDRREQ
#define WSAEMSGSIZE         EMSGSIZE
#define WSAEPROTOTYPE       EPROTOTYPE
#define WSAENOPROTOOPT      ENOPROTOOPT
#define WSAEPROTONOSUPPORT  EPROTONOSUPPORT
#define WSAESOCKTNOSUPPORT  ESOCKTNOSUPPORT
#define WSAEOPNOTSUPP       EOPNOTSUPP
#define WSAEPFNOSUPPORT     EPFNOSUPPORT
#define WSAEAFNOSUPPORT     EAFNOSUPPORT
#define WSAEADDRNOTAVAIL    EADDRNOTAVAIL
#define WSAENETDOWN         ENETDOWN
#define WSAENETRESET        ENETRESET
#define WSAECONNABORTED     ECONNABORTED
#define WSAECONNRESET       ECONNRESET
#define WSAENOBUFS          ENOBUFS
#define WSAEISCONN          EISCONN
#define WSAESHUTDOWN        ESHUTDOWN
#define WSAETOOMANYREFS     ETOOMANYREFS
#define WSAELOOP            ELOOP
#define WSAENAMETOOLONG     ENAMETOOLONG
#define WSAEHOSTDOWN        EHOSTDOWN
#define WSAEHOSTUNREACH     EHOSTUNREACH
#define WSAENOTEMPTY        ENOTEMPTY
#define WSAEUSERS           EUSERS
#define WSAEDQUOT           EDQUOT
#define WSAESTALE           ESTALE
#define WSAEREMOTE          EREMOTE
// Windows-specific codes with no POSIX equivalent - use unique values
#define WSAEPROCLIM         10067
#define WSASYSNOTREADY      10091
#define WSAVERNOTSUPPORTED  10092
#define WSANOTINITIALISED   10093
#define WSAEDISCON          10101
#endif

#ifdef __cplusplus
// Overloads to accept int* (Windows style) instead of socklen_t* (POSIX)
inline int getsockopt(SOCKET s, int level, int optname, char* optval, int* optlen) {
    socklen_t sl = (socklen_t)*optlen;
    int r = ::getsockopt(s, level, optname, optval, &sl);
    *optlen = (int)sl;
    return r;
}
inline int recvfrom(SOCKET s, char* buf, int len, int flags, LPSOCKADDR from, int* fromlen) {
    socklen_t sl = (socklen_t)*fromlen;
    int r = ::recvfrom(s, buf, len, flags, from, &sl);
    *fromlen = (int)sl;
    return r;
}
inline int accept(SOCKET s, LPSOCKADDR from, int* fromlen) {
    socklen_t sl = (socklen_t)*fromlen;
    int r = ::accept(s, from, &sl);
    *fromlen = (int)sl;
    return r;
}
#endif // __cplusplus

// LINGER - map to BSD struct linger
#ifndef _LINGER_DEFINED
#define _LINGER_DEFINED
typedef struct linger LINGER;
#endif
