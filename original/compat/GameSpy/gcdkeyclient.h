// GameSpy/gcdkeyclient.h - stub
#pragma once
#ifndef GAMESPY_GCDKEYCLIENT_H
#define GAMESPY_GCDKEYCLIENT_H
#include "nonport.h"
typedef void* GCDKEYData;
typedef void (*gcd_authcallback)(int localid, int authenticated, char* errmsg, void* instance);
#define RESPONSE_SIZE 73
inline void gcd_init(int gameid) {}
inline void gcd_think(GCDKEYData data) {}
inline void gcd_shutdown(void) {}
inline int  gcd_authenticate_user(int localid, unsigned int ip, const char* challenge,
                                   const char* response, gcd_authcallback cb, void* inst) { return 0; }
inline void gcd_disconnect_user(int localid) {}
inline void gcd_compute_response(const char* cdkey, const char* challenge, char* response) {
    if (response) response[0] = 0;
}
#endif
