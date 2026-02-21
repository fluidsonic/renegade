// winsock2.h compat shim for macOS
#pragma once
#ifndef WINSOCK2_H_COMPAT
#define WINSOCK2_H_COMPAT

#include "winsock.h"

// Additional Winsock2 constants
#define SD_RECEIVE  SHUT_RD
#define SD_SEND     SHUT_WR
#define SD_BOTH     SHUT_RDWR

#define MSG_PEEK    MSG_PEEK
#define MSG_WAITALL MSG_WAITALL

// Winsock2 extended address info stub
typedef struct addrinfo ADDRINFOA;

#endif // WINSOCK2_H_COMPAT
