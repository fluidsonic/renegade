// winsock.h compat shim for macOS - maps to BSD sockets
#pragma once
#ifndef WINSOCK_H_COMPAT
#define WINSOCK_H_COMPAT

#include "windef.h"
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
inline int  ioctlsocket(SOCKET s, long cmd, u_long* argp) { return fcntl(s, F_SETFL, *argp ? O_NONBLOCK : 0); }

// Socket options
#define SO_REUSEADDR    SO_REUSEADDR
#define IPPROTO_TCP     IPPROTO_TCP
#define TCP_NODELAY     TCP_NODELAY

// HOSTENT
typedef struct hostent HOSTENT;

// Address helpers
#define INADDR_ANY       0x00000000
#define INADDR_BROADCAST 0xffffffff
#define INADDR_NONE      0xffffffff

// inet_addr is available from netinet/in.h

// htons/ntohs/htonl/ntohl are in arpa/inet.h

// TIMEVAL is struct timeval from sys/time.h

// fd_set operations are in sys/select.h
#include <sys/select.h>

#endif // WINSOCK_H_COMPAT
